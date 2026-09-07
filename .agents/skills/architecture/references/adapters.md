# Referência de Adaptadores e Integrações

> Estrutura e princípios: `principles.md` (P3, P4, P7, P9, P11) e `scaffolding.md` (Parte 3).

O eligibility-service tem exatamente **duas dependências externas**: o serviço de **toggles** (HTTP,
Retrofit) e o **DynamoDB** (AWS SDK v2 Enhanced). Não há banco relacional, fila nem cache — não
introduza um sem discutir com o usuário.

---

# Parte 1: Adaptador HTTP (Retrofit)

## Anatomia — `infrastructure/toggle/`

| Arquivo | Papel |
|---------|-------|
| `ToggleApi` | Interface Retrofit — só declara a chamada, sem lógica |
| `ToggleHttpAdapter` | Implementa `TogglePort`; `@Profile("!fake")` |
| `FakeToggleAdapter` | Implementação local; `@Profile("fake")` — permite subir sem rede |
| `ToggleResponse` | Corpo da resposta (formato do wire) — não sai do pacote |
| `ToggleMapper` | MapStruct: `ToggleResponse` → `ProductToggle` |
| `ToggleProperties` | `TOGGLE_BASE_URL`, `TOGGLE_PRODUCT`, `TOGGLE_TIMEOUT_SECONDS` |
| `ToggleUnavailableException` | Falha de comunicação/status, traduzida na borda |

## Regras

1. **O cliente fica encapsulado** — `ToggleApi` (Retrofit) e `OkHttpClient` nunca escapam do pacote
2. **Toda falha vira `ToggleUnavailableException`** — `IOException`, status não-2xx e corpo nulo
3. **Timeout sempre explícito**, vindo de `ToggleProperties`, nunca hard-coded
4. **Nada de retry silencioso** que multiplique latência sem orçamento definido — se for adicionar, defina timeout total e discuta com o usuário
5. **Detalhe da falha só no log** — o corpo da resposta HTTP ao cliente é genérico (P9)

```java
// ✅ Tradução de falha completa: status ruim, corpo nulo e erro de I/O
try {
    Response<ToggleResponse> response = toggleApi.getToggle(
            properties.getProduct(),
            request.getCnpj().getValue(),
            request.getRegiao().getValue(),
            request.getDicom().getValue()).execute();

    if (!response.isSuccessful() || response.body() == null) {
        throw new ToggleUnavailableException("toggle service returned status " + response.code());
    }
    return toggleMapper.toDomain(response.body());
} catch (IOException exception) {
    throw new ToggleUnavailableException("toggle service call failed", exception);
}
```

## Configuração

`infrastructure/config/RetrofitConfiguration` monta `OkHttpClient` (timeouts de
`ToggleProperties`) + `Retrofit` (converter Jackson) e expõe `ToggleApi` como bean.
O `ToggleProperties` é registrado em `InfrastructurePropertiesConfiguration`.

## Adicionando uma nova integração HTTP

1. Nova porta em `application/port/out` nomeada pela capacidade
2. Novo pacote `infrastructure/<servico>/` com a anatomia da tabela acima
3. Bean do cliente numa `@Configuration` própria; nunca amplie `RetrofitConfiguration` para dois serviços sem separar as instâncias e os timeouts
4. Exceção própria, mapeada no `ApiExceptionHandler`
5. Teste com MockWebServer cobrindo: 2xx, status de erro, corpo vazio e timeout/`IOException`

---

# Parte 2: Persistência (DynamoDB Enhanced)

## Anatomia — `infrastructure/dynamo/`

| Arquivo | Papel |
|---------|-------|
| `CnpjPermissionDynamoAdapter` | Implementa `CnpjPermissionPort`; monta a `Key` e consulta |
| `CnpjPermissionItem` | Bean da tabela (`TableSchema.fromBean`) — não sai do pacote |
| `CnpjPermissionMapper` | MapStruct: `Item` → domínio, criando os value objects |
| `DynamoProperties` | `DYNAMO_TABLE_NAME`, `DYNAMO_ENDPOINT`, `AWS_REGION` |

Tabela: **`cnpj-product-control`**, chave `cnpj` (HASH) + `regiao` (RANGE).

## Regras

1. **`DynamoDbTable<T>` é resolvido no construtor do adaptador**, a partir de `DynamoProperties` — sem nome de tabela hard-coded
2. **`*Item` nunca vaza** — a conversão para domínio é do `*Mapper`, que constrói os value objects via `Cnpj.of` / `Regiao.of`
3. **Credenciais vêm da cadeia padrão do SDK** (task role no ECS). Nunca em código nem em `application.yml`
4. **Prefira `getItem` por chave completa**; `query` só quando a chave de partição sozinha basta. Nada de `scan` — é custo linear na tabela inteira
5. **Ausência é `Optional.empty()`**, não exceção — "não cadastrado" é decisão de domínio (`DenialReason.CNPJ_NOT_REGISTERED`), não erro de infra

```java
// ✅ Chave completa + Optional; a decisão sobre "não existe" é do domínio
Key key = Key.builder()
        .partitionValue(cnpj.getValue())
        .sortValue(regiao.getValue())
        .build();

return Optional.ofNullable(table.getItem(key)).map(mapper::toDomain);
```

## Mudança de schema, índice ou permissão — os três lugares

A definição real da tabela vive no Terraform. Mudou uma, mude as três:

| Mudança | Arquivos a alinhar |
|---------|--------------------|
| Atributo, chave ou índice novo | `infra/terraform/dynamodb.tf` → `scripts/seed-dynamo.sh` → `CnpjPermissionItem` |
| Nova operação (ex.: `PutItem`) | `infra/terraform/iam.tf` — **menor privilégio**, hoje só `GetItem`/`Query` |
| Nova tabela | os três acima + `DynamoProperties` + nova porta/adaptador |

❌ **Nunca** altere o schema só no `scripts/seed-dynamo.sh` — o seed acompanha o Terraform, não o contrário.

## Ambiente local

LocalStack 3.4 (apenas DynamoDB) na porta `4566`, via `docker compose up -d`.
`scripts/seed-dynamo.sh` é montado em `/etc/localstack/init/ready.d/` e cria/popula a tabela na
subida do container.

```bash
docker compose up -d
DYNAMO_ENDPOINT=http://localhost:4566 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  java -jar <jar>-boot.jar --spring.profiles.active=fake
```

---

# Parte 3: Resiliência e Observabilidade

## Timeouts

- Todo cliente externo tem timeout de connect e de read explícitos, vindos de `*Properties`
- Default razoável no `application.yml`; ajuste por ambiente via variável de ambiente
- Sem timeout = thread do Tomcat presa até o TCP desistir; trate como bug

## Degradação

- Toggle indisponível **não** vira "elegível por padrão": a falha se propaga como
  `ToggleUnavailableException` → 503. Fail-closed é a decisão de negócio vigente
- O profile `fake` (`FakeToggleAdapter`, região `NORTE` desligada) é para desenvolvimento local,
  não fallback de produção

## Logging

- SLF4J via `@Slf4j` (permitido no núcleo — é API, não framework de I/O)
- Formato chave=valor: `log.info("eligibility evaluated regiao={} eligible={} reason={}", ...)`
- ❌ Nunca logue CNPJ completo, payload inteiro, credencial, token ou header de autorização
- ❌ Nunca logue no nível `error` o que é comportamento esperado (CNPJ não cadastrado)

## Health check

`/actuator/health` com probes habilitados, porta 8080. Ao adicionar dependência externa nova,
decida explicitamente se ela entra no health check de readiness — dependência não crítica no
readiness derruba o serviço por falha alheia.

---

# Checklist de Verificação (Adaptadores)

```
□ Adaptador implementa exatamente uma porta e é @Component
□ Cliente/SDK não escapa do pacote do adaptador
□ *Item / *Response não aparecem em application nem domain
□ *Mapper cobre todos os campos de destino (unmappedTargetPolicy=ERROR)
□ Timeout explícito, vindo de *Properties
□ Toda falha traduzida para exceção do pacote e mapeada no ApiExceptionHandler
□ Resposta HTTP de erro genérica — sem stacktrace, classe, host, tabela ou status externo
□ Zero credencial em código, application.yml ou log
□ DynamoDB: consulta por chave (getItem/query), nunca scan
□ Mudança de schema alinhou dynamodb.tf, iam.tf e seed-dynamo.sh
□ IAM ampliado de forma mínima e explícita
□ Teste com MockWebServer / stub da porta — nenhuma chamada a serviço real
```
