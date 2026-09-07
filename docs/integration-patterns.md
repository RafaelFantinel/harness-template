# Integration Patterns

Padroes para integrar servicos externos e manter o servico resiliente e observavel.

> **Estrutura de camadas**: para limites de modulo e regras de dependencia, veja a skill
> [`.agents/skills/architecture/SKILL.md`](../.agents/skills/architecture/SKILL.md).
> Em caso de divergencia, os testes em `src/test/java/br/com/acme/eligibility/architecture/` vencem esta documentacao.

Integracoes existentes hoje:

| Dependencia externa | Porta (`application/port/out`) | Adaptador (`infrastructure`) | Tecnologia |
| --- | --- | --- | --- |
| Servico de toggles | `TogglePort` | `ToggleHttpAdapter` / `FakeToggleAdapter` | Retrofit + OkHttp |
| Tabela de controle de CNPJ | `CnpjPermissionPort` | `CnpjPermissionDynamoAdapter` | AWS SDK v2 (DynamoDB Enhanced) |

---

## Encapsulamento de cliente externo

**Principio central**: o adaptador concentra **todo** detalhe de protocolo. O caso de uso so conhece operacao de negocio.

**O adaptador conhece:**
- ✅ URLs, paths e query params
- ✅ Autenticacao (headers, tokens)
- ✅ Mapeamento payload ↔ dominio
- ✅ Traducao de erro tecnico para excecao de infraestrutura
- ✅ Timeouts

**O caso de uso conhece:**
- ✅ Somente a porta (`TogglePort`, `CnpjPermissionPort`)
- ❌ Nada de HTTP, Retrofit, AWS SDK, URL ou credencial

### Anatomia de uma integracao HTTP

Uma integracao Retrofit e composta por cinco arquivos no mesmo pacote `infrastructure/<tecnologia>/`:

```
infrastructure/toggle/
  ToggleApi.java                    # contrato HTTP (interface Retrofit)
  ToggleResponse.java               # payload cru do servico externo
  ToggleMapper.java                 # MapStruct: payload -> dominio
  ToggleHttpAdapter.java            # implementa a porta, traduz erro
  ToggleProperties.java             # @ConfigurationProperties do cliente
  ToggleUnavailableException.java   # excecao de infraestrutura
```

O bean Retrofit e montado em `infrastructure/config/RetrofitConfiguration.java` — nunca dentro do adaptador.

#### 1. Contrato HTTP isolado

```java
/** Contrato HTTP do servico externo de toggles. */
public interface ToggleApi {

    @GET("/toggles/{product}")
    Call<ToggleResponse> getToggle(@Path("product") String product,
                                   @Query("cnpj") String cnpj,
                                   @Query("regiao") String regiao,
                                   @Query("dicom") String dicom);
}
```

#### 2. Payload cru tolerante a mudanca

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // campo novo no provedor nao quebra o servico
public class ToggleResponse {

    private String name;
    private boolean enabled;
}
```

#### 3. Adaptador implementando a porta

```java
@Slf4j
@Component
@Profile("!fake")
@RequiredArgsConstructor
public class ToggleHttpAdapter implements TogglePort {

    private final ToggleApi toggleApi;
    private final ToggleMapper toggleMapper;
    private final ToggleProperties properties;

    @Override
    public ProductToggle fetchToggle(EligibilityRequest request) {
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
    }
}
```

Pontos obrigatorios do exemplo acima:

- `response.body() == null` e checado junto com `isSuccessful()` — 204 com corpo vazio nao pode virar `NullPointerException`.
- `IOException` (timeout, DNS, connection reset) e traduzida, nunca propagada crua.
- O payload externo (`ToggleResponse`) **nao** sai do pacote do adaptador; quem cruza a fronteira e `ProductToggle`.

#### 4. Configuracao do cliente centralizada

```java
@Configuration
public class RetrofitConfiguration {

    @Bean
    public OkHttpClient toggleHttpClient(ToggleProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        return new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .callTimeout(timeout)
                .build();
    }

    @Bean
    public ToggleApi toggleApi(OkHttpClient toggleHttpClient, ToggleProperties properties, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
                .baseUrl(properties.getBaseUrl())
                .client(toggleHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(ToggleApi.class);
    }
}
```

### Anatomia de uma integracao DynamoDB

```
infrastructure/dynamo/
  CnpjPermissionItem.java           # @DynamoDbBean — detalhe de persistencia
  CnpjPermissionMapper.java         # MapStruct: item -> dominio
  CnpjPermissionDynamoAdapter.java  # implementa a porta
  DynamoProperties.java             # tabela, regiao, endpoint
```

```java
@Slf4j
@Component
public class CnpjPermissionDynamoAdapter implements CnpjPermissionPort {

    private final DynamoDbTable<CnpjPermissionItem> table;
    private final CnpjPermissionMapper mapper;

    public CnpjPermissionDynamoAdapter(DynamoDbEnhancedClient enhancedClient,
                                       DynamoProperties properties,
                                       CnpjPermissionMapper mapper) {
        this.table = enhancedClient.table(properties.getTableName(),
                TableSchema.fromBean(CnpjPermissionItem.class));
        this.mapper = mapper;
    }

    @Override
    public Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao) {
        Key key = Key.builder()
                .partitionValue(cnpj.getValue())
                .sortValue(regiao.getValue())
                .build();

        return Optional.ofNullable(table.getItem(key)).map(mapper::toDomain);
    }
}
```

Regras:

- **Ausencia nao e erro.** "CNPJ nao cadastrado" volta como `Optional.empty()`; quem decide o significado e a `EligibilityPolicy`, nao o adaptador.
- Acesso sempre por chave (`GetItem`) ou `Query` sobre a chave composta `cnpj` (HASH) + `regiao` (RANGE). **Nunca `Scan`** — a IAM policy do task role nem permite.
- `TableSchema.fromBean` e resolvido uma vez no construtor, nao por chamada.

### Ao criar uma tabela, indice ou permissao nova

A definicao real da infraestrutura vive no Terraform. Mudar so o seed local produz divergencia silenciosa entre dev e producao.

| Mudanca | Arquivos que precisam andar juntos |
| --- | --- |
| Nova tabela / novo atributo de chave | `infra/terraform/dynamodb.tf` + `scripts/seed-dynamo.sh` |
| Nova acao do DynamoDB (ex.: `PutItem`) | `infra/terraform/iam.tf` (statement explicito e minimo) |
| Novo nome de tabela / endpoint | `DynamoProperties` + `application.yml` + `infra/terraform` |

---

## Injecao e selecao de implementacao

### Padrao: uma porta, um adaptador por profile

O servico usa `@Profile` para escolher a implementacao. `ToggleHttpAdapter` (`@Profile("!fake")`) e `FakeToggleAdapter` (`@Profile("fake")`) sao mutuamente exclusivos — nao existe bean ambiguo.

```java
@Component
@Profile("fake")
@RequiredArgsConstructor
public class FakeToggleAdapter implements TogglePort {

    private static final Set<String> DISABLED_REGIONS = Set.of("NORTE");

    private final ToggleProperties properties;

    @Override
    public ProductToggle fetchToggle(EligibilityRequest request) {
        boolean enabled = !DISABLED_REGIONS.contains(request.getRegiao().getValue());
        return new ProductToggle(properties.getProduct(), enabled);
    }
}
```

Isso permite subir o servico sem rede externa:

```bash
java -jar <jar>-boot.jar --spring.profiles.active=fake
```

### Dominio e aplicacao continuam livres de framework

Caso de uso e policy **nao** levam anotacao do Spring. Sao registrados como bean por configuracao explicita na infraestrutura:

```java
@Configuration
public class UseCaseConfiguration {

    @Bean
    public CheckEligibilityUseCase checkEligibilityUseCase(TogglePort togglePort,
                                                           CnpjPermissionPort cnpjPermissionPort,
                                                           EligibilityPolicy eligibilityPolicy) {
        return new CheckEligibilityUseCase(togglePort, cnpjPermissionPort, eligibilityPolicy);
    }
}
```

`CleanDomainTest` reprova o build se `domain..` ou `application..` importar Spring, AWS SDK, Retrofit, OkHttp ou Jackson.

### Properties por adaptador

Cada integracao tem sua propria classe `*Properties` no pacote do adaptador, com default seguro, registrada em `InfrastructurePropertiesConfiguration`:

```java
@Getter
@Setter
@ConfigurationProperties(prefix = "eligibility.toggle")
public class ToggleProperties {

    private String baseUrl = "http://localhost:8081";
    private String product = "eligibility-product";
    private long timeoutSeconds = 3L;
}
```

```yaml
eligibility:
  toggle:
    base-url: ${TOGGLE_BASE_URL:http://localhost:8081}
    product: ${TOGGLE_PRODUCT:eligibility-product}
    timeout-seconds: ${TOGGLE_TIMEOUT_SECONDS:3}
  dynamo:
    table-name: ${DYNAMO_TABLE_NAME:cnpj-product-control}
    region: ${AWS_REGION:us-east-1}
    endpoint: ${DYNAMO_ENDPOINT:}
```

Toda configuracao entra por variavel de ambiente com default no `application.yml`. **Nenhum valor de ambiente e fixado em codigo.**

---

## Timeouts e resiliencia

### Timeout e obrigatorio em toda chamada externa

O cliente de toggles configura os tres timeouts do OkHttp a partir de `timeout-seconds` (default 3s):

```java
Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
new OkHttpClient.Builder()
        .connectTimeout(timeout)   // handshake TCP/TLS
        .readTimeout(timeout)      // silencio entre bytes da resposta
        .callTimeout(timeout)      // teto da chamada inteira
        .build();
```

`callTimeout` e o que garante o teto real — sem ele, uma resposta lenta em chunks pode segurar a thread indefinidamente.

### Degradacao: falha rapida, erro explicito

O toggle e dependencia **critica**: sem ele nao existe decisao de elegibilidade. Por isso a estrategia e falhar de forma explicita (`503`), nao inventar um default:

```java
throw new ToggleUnavailableException("toggle service returned status " + response.code());
```

`ApiExceptionHandler` traduz para uma resposta estavel, sem vazar detalhe interno:

```java
@ExceptionHandler(ToggleUnavailableException.class)
public ResponseEntity<ErrorResponseDto> handleToggleUnavailable(ToggleUnavailableException exception) {
    log.error("toggle service unavailable", exception);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponseDto("TOGGLE_UNAVAILABLE", "toggle service is unavailable"));
}
```

> ❌ **Nao** aplicar fallback silencioso (assumir `enabled = true`) em toggle. Liberar produto para uma regiao desligada e pior que devolver `503`.

### Evitar chamada desnecessaria

Quando o toggle ja nega, o caso de uso nao lê o DynamoDB:

```java
ProductToggle toggle = togglePort.fetchToggle(request);

// Produto desligado na regiao dispensa a leitura no DynamoDB.
Optional<EligibilityDecision> toggleDenial = policy.denyWhenProductDisabled(request, toggle);
if (toggleDenial.isPresent()) {
    return log(request, toggleDenial.get());
}

Optional<CnpjPermission> permission = cnpjPermissionPort.findBy(request.getCnpj(), request.getRegiao());
```

Ordenar as integracoes da mais barata/decisiva para a mais cara e o padrao do servico.

### Retry e circuit breaker

Hoje **nao existe** retry nem circuit breaker no projeto — a estrategia e timeout curto (3s) + falha explicita. Ao considerar adicionar:

- Retry so em operacao **idempotente** (o `GET` de toggle e; um `PutItem` de escrita nao necessariamente).
- Backoff exponencial, no maximo 3 tentativas, e o retry precisa caber **dentro** do orcamento de latencia do endpoint — 3 tentativas de 3s viram 9s de p99.
- O AWS SDK v2 ja aplica retry proprio com backoff; nao empilhar outro por cima do `DynamoDbClient`.
- Circuit breaker so vale se a dependencia tiver fallback util. Sem fallback, ele apenas troca `503` lento por `503` rapido — o que ainda pode ser desejavel sob carga, mas nao e ganho funcional.

**Checklist por chamada externa:**
- [ ] Timeout configurado por property (connect + read + call)
- [ ] Erro tecnico traduzido para excecao propria de infraestrutura
- [ ] Corpo nulo / status nao-2xx tratados explicitamente
- [ ] Handler mapeando a excecao para status HTTP estavel
- [ ] Teste cobrindo happy path **e** falha (`MockWebServer`)
- [ ] Decisao consciente sobre fallback: valor degradado ou erro explicito

---

## Logs estruturados

**Regras:**
- ✅ SLF4J via `@Slf4j`, com log parametrizado (`{}`), nunca concatenacao
- ✅ Logar evento de negocio com os campos que permitem investigar
- ✅ Nivel `ERROR` com a excecao completa para falha de dependencia
- ❌ Nunca logar token, credencial AWS ou header de autenticacao
- ❌ Nunca logar o CNPJ completo em nivel `INFO` — e dado identificavel

```java
private EligibilityDecision log(EligibilityRequest request, EligibilityDecision decision) {
    log.info("eligibility evaluated regiao={} eligible={} reason={}",
            request.getRegiao().getValue(), decision.isEligible(), decision.getReason());
    return decision;
}
```

O log de decisao registra regiao, resultado e motivo — suficiente para auditar comportamento sem expor o CNPJ.

**O que logar:**
- ✅ Decisao de elegibilidade (regiao, resultado, motivo)
- ✅ Falha de dependencia externa (`log.error` com a excecao)
- ✅ Erro inesperado, antes de virar `INTERNAL_ERROR`

**O que NAO logar:**
- ❌ CNPJ completo, DICOM ou qualquer PII sem mascarar
- ❌ Credencial, token, chave de API
- ❌ Payload cru completo do servico externo

Nivel default em `application.yml`:

```yaml
logging:
  level:
    br.com.acme.eligibility: INFO
```

---

## Health check e observabilidade

O Actuator expoe apenas `health` e `info`. Probes de liveness/readiness estao habilitados para o ECS:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
```

- Endpoint de saude: `GET /actuator/health` (porta `8080`), usado pelo target group do ALB.
- **Nao** expor `env`, `beans`, `configprops` ou `heapdump` — vazam configuracao interna.
- Ao adicionar um health indicator de dependencia externa, ele nao pode deixar a task `UNHEALTHY` por indisponibilidade de terceiro: uma dependencia degradada nao justifica o ECS reciclar o container em loop.
- Nao existe stack de metricas (Micrometer/Prometheus) no projeto hoje. Se for adicionada, nomear metrica por dominio (`eligibility_decision_total`) e **nunca** usar CNPJ como label — cardinalidade infinita derruba o backend de metricas.

---

## Testes de integracao

Chamada HTTP real e proibida em teste. O padrao e `MockWebServer` apontando o `Retrofit` para o servidor local:

```java
@BeforeEach
void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    ToggleProperties properties = new ToggleProperties();
    properties.setBaseUrl(server.url("/").toString());

    ToggleApi api = new Retrofit.Builder()
            .baseUrl(properties.getBaseUrl())
            .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper()))
            .build()
            .create(ToggleApi.class);

    adapter = new ToggleHttpAdapter(api, new ToggleMapperImpl(), properties);
}

@Test
void shouldFailWhenToggleServiceReturnsError() {
    server.enqueue(new MockResponse().setResponseCode(SERVER_ERROR));

    assertThatThrownBy(() -> adapter.fetchToggle(request))
            .isInstanceOf(ToggleUnavailableException.class);
}
```

Cobertura minima de um adaptador novo:

- Happy path com mapeamento para o modelo de dominio
- Status de erro (`5xx`) traduzido para a excecao de infraestrutura
- Corpo vazio ou malformado
- Timeout, quando houver logica de timeout propria

Para o DynamoDB, o LocalStack sobe via `docker compose up -d` na porta `4566`, com a tabela criada e populada por `scripts/seed-dynamo.sh`:

```bash
DYNAMO_ENDPOINT=http://localhost:4566 \
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
java -jar <jar>-boot.jar --spring.profiles.active=fake
```

Suite completa: `mvn clean install` (compila, testes, Checkstyle e ArchUnit).

---

## Seguranca

### Credencial nunca em codigo ou em `application.yml`

Credencial AWS vem da cadeia padrao do SDK (task role no ECS). O `DynamoDbClient` so recebe endpoint e regiao:

```java
DynamoDbClientBuilder builder = DynamoDbClient.builder()
        .region(Region.of(properties.getRegion()));

if (!properties.getEndpoint().isEmpty()) {          // endpoint override so para LocalStack
    builder.endpointOverride(URI.create(properties.getEndpoint()));
}
```

```java
// ❌ RUIM
private static final String API_TOKEN = "tk_live_1234567890";

// ✅ BOM — property com default seguro, valor real via variavel de ambiente
@ConfigurationProperties(prefix = "eligibility.toggle")
public class ToggleProperties { private String baseUrl = "http://localhost:8081"; }
```

Em LocalStack use `AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test` — credencial descartavel, nunca a real.

### Resposta de erro nao vaza detalhe interno

```java
private static final String GENERIC_MESSAGE = "unexpected error, try again later";

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
    log.error("unexpected error", exception);   // detalhe fica no log
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponseDto("INTERNAL_ERROR", GENERIC_MESSAGE));
}
```

Stacktrace, nome de classe, host interno ou nome de tabela **nunca** entram na resposta HTTP.

### Menor privilegio na IAM

O task role tem exatamente as acoes de que precisa, sobre exatamente o recurso que usa:

```hcl
# Menor privilegio: leitura apenas da tabela de controle de CNPJ.
data "aws_iam_policy_document" "task_dynamodb_read" {
  statement {
    actions   = ["dynamodb:GetItem", "dynamodb:Query"]
    resources = [aws_dynamodb_table.cnpj_product_control.arn]
  }
}
```

Sem `dynamodb:*`, sem `resources = ["*"]`. Acesso novo entra como statement explicito.

### Adaptador nao atravessa fronteira

```java
// ❌ RUIM: controller importando adaptador de infraestrutura
import br.com.acme.eligibility.infrastructure.toggle.ToggleHttpAdapter;

// ✅ BOM: controller depende do caso de uso; caso de uso depende da porta
import br.com.acme.eligibility.application.usecase.CheckEligibilityUseCase;
```

`NamingConventionTest` reprova o build se o controller tocar `infrastructure.dynamo..` ou `infrastructure.toggle..`.

**Checklist por integracao:**
- [ ] Credencial via variavel de ambiente / task role, nunca em codigo
- [ ] Sem dado sensivel (CNPJ completo, token) em log
- [ ] Detalhe interno fora da resposta de erro
- [ ] TLS validado — sem desabilitar verificacao de certificado no OkHttp
- [ ] IAM com acao e recurso explicitos e minimos
- [ ] Payload externo (`*Response`, `*Item`) confinado ao pacote do adaptador
