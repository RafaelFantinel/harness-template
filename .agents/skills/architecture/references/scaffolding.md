# Referência de Scaffolding

Como criar cada tipo de classe do eligibility-service, em ordem de dentro para fora.

**Ordem canônica:** domínio → porta → caso de uso → adaptador → bean de configuração → endpoint → testes.

Sempre que a mudança tocar um sistema externo, leia também `references/adapters.md`.
Ao terminar, rode a verificação de `references/verification.md`.

---

# Parte 0: Onde a Classe Nova Deve Morar

```
A classe tem regra de negócio?
├─ SIM, é um conceito com invariante próprio        → domain/model/<Nome>.java (value object)
├─ SIM, é uma decisão que combina conceitos         → domain/policy/<Nome>Policy.java
├─ NÃO, ela orquestra chamadas e delega a decisão   → application/usecase/<Nome>UseCase.java
├─ NÃO, ela fala com sistema externo                → application/port/out/<Nome>Port.java (interface)
│                                                     + infrastructure/<tecnologia>/<Nome><Tec>Adapter.java
├─ NÃO, ela converte formato                        → *Mapper no pacote do lado "sujo" da conversão
├─ NÃO, ela expõe HTTP                              → presentation/api/<Nome>Controller.java
└─ NÃO, ela monta bean/configuração                 → infrastructure/config/<Nome>Configuration.java
```

Sinal de alerta: se a classe não cabe em nenhum desses, provavelmente ela é duas classes.
Não crie `util`, `helper`, `common` nem `shared`.

---

# Parte 1: Modelo de Domínio, Value Object e Policy

## Value Object

Use quando o conceito tem invariante (formato, faixa, conjunto fechado). Exemplos existentes:
`Cnpj` (14 dígitos normalizados), `Regiao` (conjunto conhecido), `Dicom`.

```java
package br.com.acme.eligibility.domain.model;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Javadoc obrigatório em tipo público (Checkstyle JavadocType). */
@Getter
@ToString
@EqualsAndHashCode
public final class <Nome> {

    private static final int LENGTH = 14;   // sem número mágico solto

    private final String value;

    private <Nome>(String value) {
        this.value = value;
    }

    public static <Nome> of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainValidationException("<campo> is required");
        }
        // normalize, valide, e só então construa
        return new <Nome>(raw.trim());
    }
}
```

Checklist:
- [ ] `final`, imutável, construtor privado, fábrica `of(...)`
- [ ] Falha lança `DomainValidationException` com mensagem sobre o **campo**, nunca sobre o valor recebido
- [ ] Zero import de framework (P2)
- [ ] Teste unitário em `src/test/java/.../domain/...` cobrindo nulo, vazio, formato inválido e normalização

## Policy

Regra que combina modelos e produz decisão. Sem I/O, sem `Optional` de infraestrutura vazando.

```java
package br.com.acme.eligibility.domain.policy;

/** Regras de <assunto>. */
public class <Nome>Policy {

    /** Cada método devolve a decisão ou Optional.empty() quando a regra não se aplica. */
    public Optional<EligibilityDecision> denyWhen<Condicao>(EligibilityRequest request, ProductToggle toggle) {
        ...
    }
}
```

Checklist:
- [ ] Nenhuma chamada externa — a policy recebe tudo pronto por parâmetro
- [ ] Cada regra num método nomeado pela regra, não pelo passo (`denyWhenProductDisabled` ✅, `step1` ❌)
- [ ] Registrada como `@Bean` em `infrastructure/config/UseCaseConfiguration` (P10)

---

# Parte 2: Caso de Uso

O caso de uso **orquestra**: chama portas, ordena as chamadas, delega a decisão à policy e loga o
resultado. Ele não decide regra de negócio.

```java
package br.com.acme.eligibility.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Javadoc dizendo o que o caso de uso orquestra. */
@Slf4j
@RequiredArgsConstructor
public class <Nome>UseCase {

    private final <Nome>Port <nome>Port;
    private final <Nome>Policy policy;

    public <Resultado> execute(<Comando> request) {
        // 1. buscar o mínimo necessário pelas portas
        // 2. curto-circuitar cedo quando a decisão já é possível (evita I/O desnecessário)
        // 3. delegar a decisão para a policy
        // 4. logar o resultado de forma estruturada, sem dado sensível
    }
}
```

**Ordem das chamadas importa.** `CheckEligibilityUseCase` consulta o toggle primeiro porque toggle
desligado dispensa a leitura no DynamoDB — economia real, não estilo. Documente a ordem em comentário
quando ela for intencional.

Checklist:
- [ ] Sufixo `UseCase` (`NamingConventionTest.USE_CASES_ARE_SUFFIXED`)
- [ ] Um método público `execute(...)`
- [ ] Sem anotação de estereótipo Spring (P10) — vira `@Bean` em `UseCaseConfiguration`
- [ ] Assinatura só com tipos de `domain/model`
- [ ] Log estruturado com chave=valor; sem CNPJ completo nem dado pessoal no log
- [ ] Teste unitário com portas mockadas (Mockito), cobrindo cada ramo de decisão

---

# Parte 3: Porta + Adaptador

## Passo 1 — Porta (a decisão de design)

```java
package br.com.acme.eligibility.application.port.out;

/** Porta de saida para <capacidade>. */
public interface <Nome>Port {

    <TipoDeDominio> <verbo>(<TiposDeDominio> ...);
}
```

- Nomeie pela **capacidade**, não pela tecnologia
- Só tipos de `domain/model` na assinatura
- Interface pura: nada mais pode viver em `application.port..` (`PORTS_ARE_INTERFACES`)

## Passo 2 — Adaptador

```java
package br.com.acme.eligibility.infrastructure.<tecnologia>;

@Slf4j
@Component
@RequiredArgsConstructor
public class <Nome><Tecnologia>Adapter implements <Nome>Port {

    private final <ClientDoSdk> client;
    private final <Nome>Mapper mapper;
    private final <Tecnologia>Properties properties;

    @Override
    public <TipoDeDominio> <verbo>(...) {
        // 1. traduzir domínio → formato da tecnologia
        // 2. chamar
        // 3. traduzir falha → exceção do pacote deste adaptador
        // 4. traduzir resposta → domínio via mapper
    }
}
```

## Passo 3 — Apoio, no mesmo pacote

| Arquivo | Papel |
|---------|-------|
| `<Tecnologia>Properties` | Config por variável de ambiente (P11) |
| `<Nome>Item` / `<Nome>Response` | Formato da tecnologia — não sai do pacote (P7) |
| `<Nome>Mapper` | MapStruct `@Mapper`, converte para o domínio |
| `<Nome>UnavailableException` | Falha da tecnologia, traduzida na borda (P9) |
| `Fake<Nome>Adapter` | Implementação local sob `@Profile("fake")`, quando útil |

## Passo 4 — Wiring

Bean do client em `infrastructure/config/<Tecnologia>Configuration`; `*Properties` registrado em
`InfrastructurePropertiesConfiguration`. Detalhes e patterns por tecnologia: `references/adapters.md`.

Checklist:
- [ ] Porta é interface em `application/port/out`, com vocabulário de domínio
- [ ] Adaptador `@Component`, implementa uma porta, sem regra de negócio
- [ ] `*Item`/`*Response` e `*Mapper` no pacote do adaptador
- [ ] MapStruct cobre **todos** os campos de destino (`unmappedTargetPolicy=ERROR` quebra a compilação)
- [ ] Falha traduzida para exceção do pacote, tratada no `ApiExceptionHandler`
- [ ] Teste unitário com o cliente HTTP mockado (MockWebServer/OkHttp) ou stub da porta — nunca serviço real

---

# Parte 4: Endpoint, DTO e Mapper de API

## Controller

```java
package br.com.acme.eligibility.presentation.api;

@RestController
@RequestMapping("/v1/<recurso>")
@RequiredArgsConstructor
public class <Nome>Controller {

    private final <Nome>UseCase <nome>UseCase;
    private final <Nome>ApiMapper mapper;

    @PostMapping
    public ResponseEntity<<Nome>ResponseDto> <acao>(@Valid @RequestBody <Nome>RequestDto requestDto) {
        return ResponseEntity.ok(mapper.toResponse(<nome>UseCase.execute(mapper.toDomain(requestDto))));
    }
}
```

## DTO

- Vive em `presentation/api/dto`, sufixo `Dto`
- Validação de formato com Bean Validation **`javax.validation`** (Java 11 / Spring Boot 2.7 — nunca `jakarta.*`)
- Nunca é referenciado por `application` nem `domain` (P6)

## Mapper de API

`EligibilityApiMapper` (MapStruct) faz DTO ↔ domínio, usando as fábricas dos value objects
(`Cnpj.of`, `Regiao.of`) via `@Named`/`qualifiedByName`.

## Erro

Toda tradução de exceção → HTTP fica em `presentation/exception/ApiExceptionHandler`:

| Exceção | Resposta |
|---------|----------|
| `DomainValidationException` | 400 com mensagem de campo |
| `MethodArgumentNotValidException` | 400 com mensagem de campo |
| `ToggleUnavailableException` | 503 genérico |
| Qualquer outra | 500 `INTERNAL_ERROR` genérico |

**Nunca** inclua stacktrace, nome de classe, host, tabela ou mensagem de SDK no corpo (P9).

Checklist:
- [ ] `@RestController` com sufixo `Controller`
- [ ] Injeta apenas caso de uso + mapper — nada de `infrastructure.dynamo..`/`infrastructure.toggle..` (`CONTROLLERS_DO_NOT_TOUCH_ADAPTERS`)
- [ ] DTO com `javax.validation`, só em `presentation/api/dto`
- [ ] Erro novo mapeado no `ApiExceptionHandler`, com corpo genérico
- [ ] Teste de controller cobrindo 200, 400 (payload inválido) e o erro traduzido

---

# Parte 5: Testes

- Espelhe o pacote de produção: `src/test/java/br/com/acme/eligibility/<mesmo pacote>/<Classe>Test.java`
- JUnit 5 + AssertJ + Mockito; estrutura Arrange / Act / Assert
- Nome descreve comportamento e condição
- Determinístico e independente: sem ordem entre testes, sem rede
- Chamada HTTP externa **sempre** mockada (MockWebServer/OkHttp ou stub da porta)
- DynamoDB local via LocalStack (`docker compose up -d`, porta 4566) apenas quando o teste realmente
  precisar do banco; teste de unidade usa stub da porta

**Cobertura mínima esperada** (do AGENTS.md): happy path + CNPJ inválido, região desconhecida, toggle
desligado, toggle indisponível (503), CNPJ não cadastrado e CNPJ bloqueado.

Regra estrutural nova? Adicione um `@ArchTest` (P12) — ver `references/verification.md`, Seção 2.

---

# Parte 6: Ordem de Geração Consolidada

1. Levante o requisito e classifique cada classe nova pela Parte 0
2. Crie/estenda os modelos de domínio e a policy (Parte 1) + testes
3. Declare a porta (Parte 3, passo 1)
4. Escreva o caso de uso contra a porta (Parte 2) + teste com mock
5. Implemente o adaptador, `*Item`/`*Response`, `*Mapper`, `*Properties`, exceção (Parte 3) + teste
6. Registre os beans em `infrastructure/config`
7. Exponha o endpoint: DTO, mapper de API, controller, tratamento de erro (Parte 4) + teste
8. Se tocou DynamoDB: alinhe `infra/terraform/dynamodb.tf`, `infra/terraform/iam.tf` (menor privilégio) e `scripts/seed-dynamo.sh`
9. Se criou convenção estrutural: adicione o `@ArchTest`
10. Rode `mvn clean install`, `mvn checkstyle:check` e `mvn test`
11. Se derivou um pattern novo, escreva-o em `docs/coding-patterns.md` ou `docs/integration-patterns.md`

> Lembre-se da discrepância do `pom.xml` (cinco módulos declarados, nenhum em disco) descrita no
> `SKILL.md`: comandos Maven falham até isso ser resolvido **com o usuário**. Não resolva sozinho.
