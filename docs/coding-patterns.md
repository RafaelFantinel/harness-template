# Coding Patterns

Referência de implementação para modelos de domínio, casos de uso, portas, adaptadores, controllers, mappers e configuração.

> **Regras estruturais**: os testes em [`src/test/java/br/com/acme/eligibility/architecture/`](../src/test/java/br/com/acme/eligibility/architecture/) são a fonte de verdade. Em caso de divergência, o ArchUnit vence este documento.
>
> **Integrações externas** (Retrofit, DynamoDB, timeouts, resiliência): ver [`integration-patterns.md`](./integration-patterns.md).

---

## Estrutura de Camadas e Direção de Dependência

O serviço é Clean/Hexagonal por camada. Dependências apontam **sempre para dentro**.

```
presentation → infrastructure → application → domain
```

| Camada           | Pacote                                  | Contém                                                        | Pode importar framework? |
| ---------------- | --------------------------------------- | ------------------------------------------------------------- | ------------------------ |
| `domain`         | `br.com.acme.eligibility.domain`         | Modelos, value objects, policies, exceções de domínio         | ❌ Nunca                 |
| `application`    | `br.com.acme.eligibility.application`    | Casos de uso e portas de saída (`port/out`)                   | ❌ Nunca                 |
| `infrastructure` | `br.com.acme.eligibility.infrastructure` | Adaptadores, items de persistência, `*Properties`, `@Configuration` | ✅ Sim                   |
| `presentation`   | `br.com.acme.eligibility.presentation`   | Controller, DTOs, mapper de API, handler de erro, `main`      | ✅ Sim                   |

**Regras:**

- ✅ `presentation` acessa `infrastructure`, `application` e `domain`
- ✅ `infrastructure` acessa `application` e `domain`
- ✅ `application` acessa apenas `domain`
- ❌ Nenhuma camada acessa `presentation`
- ❌ `domain` não acessa nada além dele mesmo e da JDK
- ❌ `presentation.api..` não depende de `infrastructure.dynamo..` nem de `infrastructure.toggle..`

Aplicado por `LayeredArchitectureTest` e `NamingConventionTest`.

---

## Isolamento do Núcleo (Domain + Application)

`domain` e `application` MUST ser compiláveis sem Spring, AWS SDK, Retrofit, OkHttp ou Jackson no classpath.

**Regras:**

- ✅ Apenas JDK, Lombok e tipos do próprio núcleo no `import`
- ✅ Beans do núcleo são registrados manualmente em `infrastructure/config/UseCaseConfiguration`
- ❌ Nunca `org.springframework..` em `domain..` ou `application..`
- ❌ Nunca `software.amazon..`, `retrofit2..`, `okhttp3..` no núcleo
- ❌ Nunca `com.fasterxml.jackson..` nem `javax.persistence..` em `domain..`

```java
// ✅ GOOD — application/usecase/CheckEligibilityUseCase.java
// Sem @Service. Instanciado por @Bean em infrastructure/config.
@Slf4j
@RequiredArgsConstructor
public class CheckEligibilityUseCase {

    private final TogglePort togglePort;
    private final CnpjPermissionPort cnpjPermissionPort;
    private final EligibilityPolicy policy;
}

// ✅ GOOD — infrastructure/config/UseCaseConfiguration.java
@Configuration
public class UseCaseConfiguration {

    @Bean
    public EligibilityPolicy eligibilityPolicy() {
        return new EligibilityPolicy();
    }

    @Bean
    public CheckEligibilityUseCase checkEligibilityUseCase(TogglePort togglePort,
                                                           CnpjPermissionPort cnpjPermissionPort,
                                                           EligibilityPolicy eligibilityPolicy) {
        return new CheckEligibilityUseCase(togglePort, cnpjPermissionPort, eligibilityPolicy);
    }
}

// ❌ BAD: framework vazando para o caso de uso
@Service // ❌ acopla application ao Spring — CleanDomainTest quebra o build
public class CheckEligibilityUseCase { }
```

Aplicado por `CleanDomainTest`.

---

## Value Objects de Domínio

Todo dado primitivo que carrega invariante vira value object imutável com fábrica estática validante. Um objeto construído é sempre válido.

**Regras:**

- ✅ Classe `final`, campo `private final`, construtor `private`
- ✅ Fábrica estática `of(String raw)` que valida e normaliza
- ✅ Lançar `DomainValidationException` na violação de invariante
- ✅ Normalizar na entrada (trim, upper, remover máscara), nunca no consumidor
- ✅ Limites e formatos como constantes nomeadas (`LENGTH`, `MAX_LENGTH`, `NON_DIGIT`)
- ❌ Nunca `new` público nem setter
- ❌ Nunca validar de novo em caso de uso, adaptador ou controller
- ❌ Nunca trafegar `String` crua onde existe value object

```java
// ✅ GOOD — domain/model/Cnpj.java
@Getter
@ToString
@EqualsAndHashCode
public final class Cnpj {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D");
    private static final int LENGTH = 14;

    private final String value;

    private Cnpj(String value) {
        this.value = value;
    }

    public static Cnpj of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainValidationException("cnpj is required");
        }
        String digits = NON_DIGIT.matcher(raw).replaceAll("");
        if (digits.length() != LENGTH) {
            throw new DomainValidationException("cnpj must have " + LENGTH + " digits");
        }
        return new Cnpj(digits);
    }
}

// ❌ BAD: primitivo cru atravessando as camadas
public EligibilityDecision execute(String cnpj, String regiao) { // ❌
    if (cnpj.length() != 14) { ... }                             // ❌ validação espalhada
}
```

**Agregados e resultados** usam `@Value` (imutável, all-args) e fábricas nomeadas quando há mais de uma forma de construção:

```java
// ✅ GOOD — domain/model/EligibilityDecision.java
@Value
public class EligibilityDecision {

    private final Cnpj cnpj;
    private final Regiao regiao;
    private final boolean eligible;
    private final DenialReason reason;

    public static EligibilityDecision allow(Cnpj cnpj, Regiao regiao) {
        return new EligibilityDecision(cnpj, regiao, true, null);
    }

    public static EligibilityDecision deny(Cnpj cnpj, Regiao regiao, DenialReason reason) {
        return new EligibilityDecision(cnpj, regiao, false, reason);
    }
}
```

---

## Policy de Domínio

Regra de negócio pura mora em `domain/policy`. A policy não faz I/O: recebe tudo já resolvido e devolve decisão.

**Regras:**

- ✅ Um método por etapa de decisão, com nome que declara a regra (`denyWhenProductDisabled`)
- ✅ `Optional<Decision>` vazio quando a etapa não conclui, permitindo short-circuit no caso de uso
- ✅ Javadoc explicando o **porquê** da ordem das etapas
- ❌ Nunca chamar porta, repositório ou cliente HTTP de dentro da policy
- ❌ Nunca colocar regra de negócio no caso de uso, no controller ou no adaptador

```java
// ✅ GOOD — domain/policy/EligibilityPolicy.java
public class EligibilityPolicy {

    /** Devolve a negativa quando o produto esta desligado para a regiao. */
    public Optional<EligibilityDecision> denyWhenProductDisabled(EligibilityRequest request, ProductToggle toggle) {
        if (toggle.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(EligibilityDecision.deny(request.getCnpj(), request.getRegiao(),
                DenialReason.PRODUCT_DISABLED_FOR_REGION));
    }
}
```

---

## Portas e Adaptadores

Toda dependência externa entra por uma **porta** (interface em `application/port/out`) implementada por um **adaptador** em `infrastructure/<tecnologia>/`.

**Regras:**

- ✅ Porta é sempre `interface` em `application.port..`
- ✅ Assinatura da porta usa apenas tipos de domínio — nunca DTO, item ou tipo do SDK
- ✅ Ausência de registro é `Optional.empty()`, não `null` nem exceção
- ✅ Adaptador é `@Component` em `infrastructure`, implementa a porta e traduz erro técnico em exceção própria
- ✅ Um adaptador `@Profile("fake")` quando é útil rodar sem rede
- ❌ Nunca classe concreta ou classe abstrata em `application.port..`
- ❌ Nunca vazar `Response`, `Call`, `DynamoDbTable` ou item de persistência para fora do adaptador

```java
// ✅ GOOD — application/port/out/CnpjPermissionPort.java
public interface CnpjPermissionPort {

    Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao);
}

// ✅ GOOD — infrastructure/dynamo/CnpjPermissionDynamoAdapter.java
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

// ❌ BAD: SDK vazando na assinatura da porta
public interface CnpjPermissionPort {
    CnpjPermissionItem getItem(Key key); // ❌ item e Key são detalhe de infraestrutura
}
```

**Item de persistência** é detalhe de `infrastructure/dynamo` e nunca atravessa para o núcleo:

```java
// ✅ GOOD — infrastructure/dynamo/CnpjPermissionItem.java
@Getter
@Setter
@DynamoDbBean
public class CnpjPermissionItem {

    private String cnpj;
    private String regiao;
    private boolean allowed;

    @DynamoDbPartitionKey
    public String getCnpj() {
        return cnpj;
    }

    @DynamoDbSortKey
    public String getRegiao() {
        return regiao;
    }
}
```

---

## Caso de Uso Enxuto

O caso de uso **orquestra**: chama portas, delega decisão para a policy, registra log. Não decide e não conhece HTTP.

**Regras:**

- ✅ Nome termina em `UseCase` (`application.usecase..`)
- ✅ Um método público `execute(...)` recebendo e devolvendo tipos de domínio
- ✅ Dependências por construtor via `@RequiredArgsConstructor`, tipadas pela **porta**
- ✅ Evitar I/O desnecessário: short-circuit assim que a decisão está fechada
- ✅ Log estruturado, com campos, sem dado sensível
- ❌ Nunca `if` de regra de negócio no caso de uso — isso é da policy
- ❌ Nunca depender de classe de adaptador concreta
- ❌ Nunca receber ou devolver DTO de API

```java
// ✅ GOOD — application/usecase/CheckEligibilityUseCase.java
public EligibilityDecision execute(EligibilityRequest request) {
    ProductToggle toggle = togglePort.fetchToggle(request);

    // Produto desligado na regiao dispensa a leitura no DynamoDB.
    Optional<EligibilityDecision> toggleDenial = policy.denyWhenProductDisabled(request, toggle);
    if (toggleDenial.isPresent()) {
        return log(request, toggleDenial.get());
    }

    Optional<CnpjPermission> permission = cnpjPermissionPort.findBy(request.getCnpj(), request.getRegiao());
    return log(request, policy.decideByPermission(request, permission));
}

// ❌ BAD: regra de negócio no caso de uso e dependência do adaptador
public class CheckEligibilityUseCase {
    private final ToggleHttpAdapter adapter; // ❌ classe concreta

    public EligibilityDecision execute(EligibilityRequest request) {
        if (!adapter.fetchToggle(request).isEnabled()) { // ❌ decisão fora da policy
            return EligibilityDecision.deny(...);
        }
    }
}
```

---

## Controller Enxuto

O controller trata apenas HTTP: validação do payload, uma chamada ao caso de uso, mapeamento da resposta.

**Regras:**

- ✅ Classe anotada com `@RestController` termina em `Controller`
- ✅ Método com no máximo ~5 linhas: mapear → executar → responder
- ✅ `@Valid @RequestBody` para validação de formato via Bean Validation
- ✅ Depende só de caso de uso e mapper de API
- ❌ Nunca injetar adaptador, cliente HTTP ou `DynamoDbTable`
- ❌ Nunca cálculo, agregação ou regra de negócio no controller
- ❌ Nunca expor tipo de domínio direto na resposta — sempre DTO

```java
// ✅ GOOD — presentation/api/EligibilityController.java
@RestController
@RequestMapping("/v1/elegibilidade")
@RequiredArgsConstructor
public class EligibilityController {

    private final CheckEligibilityUseCase checkEligibilityUseCase;
    private final EligibilityApiMapper mapper;

    @PostMapping
    public ResponseEntity<EligibilityResponseDto> check(@Valid @RequestBody EligibilityRequestDto requestDto) {
        EligibilityDecision decision = checkEligibilityUseCase.execute(mapper.toDomain(requestDto));
        return ResponseEntity.ok(mapper.toResponse(decision));
    }
}
```

**Responsabilidades:**

| Responsabilidade                     | Controller | Use Case | Policy | Adapter |
| ------------------------------------ | ---------- | -------- | ------ | ------- |
| Validação de formato do payload      | ✅         | ❌       | ❌     | ❌      |
| Status HTTP e serialização           | ✅         | ❌       | ❌     | ❌      |
| Conversão DTO ↔ domínio              | ✅         | ❌       | ❌     | ❌      |
| Orquestração e short-circuit         | ❌         | ✅       | ❌     | ❌      |
| Regra de negócio / decisão           | ❌         | ❌       | ✅     | ❌      |
| I/O (HTTP externo, DynamoDB)         | ❌         | ❌       | ❌     | ✅      |
| Tradução de erro técnico             | ❌         | ❌       | ❌     | ✅      |

---

## DTOs e Mapeamento com MapStruct

DTO de API existe só em `presentation/api/dto` e nunca atravessa para `application` ou `domain`. A conversão é responsabilidade de um mapper MapStruct.

**Regras:**

- ✅ DTO de entrada carrega as anotações de Bean Validation (`@NotBlank`, `@Pattern`)
- ✅ Mapper é `interface` anotada com `@Mapper`, com `default` + `@Named` para construir value objects
- ✅ Todo campo de destino mapeado — `unmappedTargetPolicy=ERROR` quebra a compilação
- ✅ Um mapper por fronteira: `EligibilityApiMapper` (API) e `CnpjPermissionMapper` (DynamoDB)
- ❌ Nunca conversão manual espalhada em controller ou adaptador
- ❌ Nunca reusar DTO de API como modelo de domínio ou item de persistência

```java
// ✅ GOOD — presentation/api/dto/EligibilityRequestDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequestDto {

    @NotBlank
    @Pattern(regexp = "\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}", message = "cnpj format is invalid")
    private String cnpj;

    @NotBlank
    private String dicom;

    @NotBlank
    private String regiao;
}

// ✅ GOOD — presentation/api/EligibilityApiMapper.java
@Mapper
public interface EligibilityApiMapper {

    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "toCnpj")
    @Mapping(target = "dicom", source = "dicom", qualifiedByName = "toDicom")
    @Mapping(target = "regiao", source = "regiao", qualifiedByName = "toRegiao")
    EligibilityRequest toDomain(EligibilityRequestDto dto);

    @Mapping(target = "cnpj", source = "cnpj.value")
    @Mapping(target = "regiao", source = "regiao.value")
    @Mapping(target = "reason", source = "reason")
    EligibilityResponseDto toResponse(EligibilityDecision decision);

    @Named("toCnpj")
    default Cnpj toCnpj(String value) {
        return Cnpj.of(value);
    }
}

// ❌ BAD: DTO cruzando a fronteira
public EligibilityDecision execute(EligibilityRequestDto dto) { } // ❌ application conhecendo DTO de API
```

**Camadas de validação** — cada uma tem seu papel, sem duplicação:

| Onde                     | Valida                                   | Erro resultante            |
| ------------------------ | ---------------------------------------- | -------------------------- |
| DTO (`@NotBlank`, `@Pattern`) | Formato do payload HTTP             | `400 INVALID_REQUEST`      |
| Value object (`Cnpj.of`) | Invariante de negócio                    | `400 INVALID_REQUEST`      |
| Policy                   | Regra de elegibilidade                   | `200` com `eligible=false` |

---

## Tratamento de Erros

Erro de infraestrutura é traduzido em exceção própria no adaptador e convertido em resposta HTTP por um `@RestControllerAdvice` único.

**Regras:**

- ✅ Cada integração tem sua exceção (`ToggleUnavailableException`) lançada pelo adaptador
- ✅ Um handler por tipo de exceção, com `code` estável e mensagem curta
- ✅ Fallback `Exception.class` responde `500 INTERNAL_ERROR` com mensagem genérica
- ✅ `log.error` com a exceção completa — o detalhe fica no log, não na resposta
- ✅ `serialVersionUID` em toda exceção (exigido pelo Checkstyle)
- ❌ **Nunca** vazar stacktrace, nome de classe ou detalhe de infraestrutura no corpo HTTP
- ❌ Nunca capturar exceção e devolver `null` ou valor default silencioso

```java
// ✅ GOOD — presentation/exception/ApiExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String GENERIC_MESSAGE = "unexpected error, try again later";

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleDomainValidation(DomainValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(ToggleUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleToggleUnavailable(ToggleUnavailableException exception) {
        log.error("toggle service unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponseDto("TOGGLE_UNAVAILABLE", "toggle service is unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
        log.error("unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("INTERNAL_ERROR", GENERIC_MESSAGE));
    }
}

// ❌ BAD: detalhe interno na resposta
return ResponseEntity.status(500).body(new ErrorResponseDto("ERROR", exception.toString())); // ❌
```

**Mapa de erros da API:**

| Situação                        | Status | `code`               |
| ------------------------------- | ------ | -------------------- |
| Payload inválido (Bean Validation) | `400`  | `INVALID_REQUEST`    |
| Invariante de domínio violada   | `400`  | `INVALID_REQUEST`    |
| Serviço de toggles fora do ar   | `503`  | `TOGGLE_UNAVAILABLE` |
| Qualquer outra falha            | `500`  | `INTERNAL_ERROR`     |

---

## Configuração e Properties

Toda configuração vem de variável de ambiente com default no `application.yml`, tipada em uma classe `*Properties` no pacote do adaptador.

**Regras:**

- ✅ `*Properties` fica junto do adaptador (`infrastructure/toggle`, `infrastructure/dynamo`)
- ✅ `@ConfigurationProperties(prefix = "eligibility.<tecnologia>")` com defaults nos campos
- ✅ Registro centralizado em `InfrastructurePropertiesConfiguration` via `@EnableConfigurationProperties`
- ✅ `@Configuration` por tecnologia (`DynamoConfiguration`, `RetrofitConfiguration`, `UseCaseConfiguration`)
- ✅ Credencial AWS pela cadeia padrão do SDK (task role)
- ❌ **Nunca** credencial, token ou segredo em código, `application.yml` ou log
- ❌ Nunca `@Value("${...}")` espalhado por classes de negócio

```java
// ✅ GOOD — infrastructure/toggle/ToggleProperties.java
@Getter
@Setter
@ConfigurationProperties(prefix = "eligibility.toggle")
public class ToggleProperties {

    private String baseUrl = "http://localhost:8081";
    private String product = "eligibility-product";
    private long timeoutSeconds = 3L;
}

// ✅ GOOD — infrastructure/config/InfrastructurePropertiesConfiguration.java
@Configuration
@EnableConfigurationProperties({ToggleProperties.class, DynamoProperties.class})
public class InfrastructurePropertiesConfiguration {
}
```

---

## Uso de Enum

Conjunto finito e nomeado de valores é `enum` no domínio — nunca `String` solta.

**Regras:**

- ✅ Motivo, status e tipo são `enum` em `domain/model` (`DenialReason`)
- ✅ O `enum` é o tipo do campo, do parâmetro e do retorno
- ✅ A conversão para `String` acontece só na borda (mapper de API)
- ❌ Nunca literal de string onde existe membro de enum (`"CNPJ_BLOCKED"`)
- ❌ Nunca `String reason` em modelo de domínio

```java
// ✅ GOOD — domain/model/DenialReason.java
public enum DenialReason {

    PRODUCT_DISABLED_FOR_REGION,
    CNPJ_NOT_REGISTERED,
    CNPJ_BLOCKED
}

// ❌ BAD
return EligibilityDecision.deny(cnpj, regiao, "CNPJ_BLOCKED"); // ❌ literal cru
```

---

## Convenções de Nomenclatura

| Categoria             | Convenção                      | Local                       | Exemplo                        |
| --------------------- | ------------------------------ | --------------------------- | ------------------------------ |
| Value object / modelo | Substantivo do negócio         | `domain/model`              | `Cnpj`, `Regiao`, `ProductToggle` |
| Enum de domínio       | Substantivo, membros `SCREAMING_SNAKE` | `domain/model`      | `DenialReason`                 |
| Policy                | `*Policy`                      | `domain/policy`             | `EligibilityPolicy`            |
| Exceção de domínio    | `*Exception`                   | `domain/exception`          | `DomainValidationException`    |
| Caso de uso           | `*UseCase` (obrigatório)       | `application/usecase`       | `CheckEligibilityUseCase`      |
| Porta                 | `*Port`, sempre `interface`    | `application/port/out`      | `TogglePort`                   |
| Adaptador             | `*<Tecnologia>Adapter`         | `infrastructure/<tec>`      | `ToggleHttpAdapter`            |
| Contrato HTTP externo | `*Api`                         | `infrastructure/<tec>`      | `ToggleApi`                    |
| Payload externo       | `*Response` / `*Request`       | `infrastructure/<tec>`      | `ToggleResponse`               |
| Item de persistência  | `*Item`                        | `infrastructure/dynamo`     | `CnpjPermissionItem`           |
| Properties            | `*Properties`                  | `infrastructure/<tec>`      | `ToggleProperties`             |
| Configuração          | `*Configuration`               | `infrastructure/config`     | `RetrofitConfiguration`        |
| Controller            | `*Controller` (obrigatório)    | `presentation/api`          | `EligibilityController`        |
| DTO de API            | `*Dto`                         | `presentation/api/dto`      | `EligibilityRequestDto`        |
| Mapper                | `*Mapper`, `interface @Mapper` | pacote da fronteira         | `EligibilityApiMapper`         |
| Teste                 | `<Classe>Test`                 | espelha o pacote de produção | `CheckEligibilityUseCaseTest`  |

**Estilo (aplicado por Checkstyle, `mvn checkstyle:check` falha em `warning`):**

- Linha com no máximo 120 caracteres, sem espaço no fim, arquivo terminando em newline
- Sem import estrela, sem import não usado; ordem `STATIC` → `THIRD_PARTY`, alfabética
- Chaves obrigatórias em todo `if`/`for`/`while`, sem bloco vazio
- Constante nomeada no lugar de número ou string mágica
- Javadoc curto em toda classe, dizendo o **porquê**, não o **quê**

---

## Testes

Testes espelham o pacote de produção e cobrem happy path mais os edge cases de negócio.

**Regras:**

- ✅ Nome no formato `should_<comportamento>_when_<condição>`
- ✅ Estrutura Arrange / Act / Assert
- ✅ Caso de uso testado com stub das portas — sem Spring context
- ✅ Adaptador HTTP testado com `MockWebServer`; controller com `MockMvc`
- ✅ Cobrir: CNPJ inválido, região desconhecida, toggle desligado, toggle indisponível (`503`), CNPJ não cadastrado, CNPJ bloqueado
- ✅ `archRule.failOnEmptyShould=true` — regra ArchUnit que não casa com nenhuma classe quebra o build
- ❌ Nunca bater em serviço de toggles real ou DynamoDB remoto em teste
- ❌ Nunca teste dependente de ordem de execução ou de estado de outro teste

```
mvn test              # unitários + ArchUnit
mvn checkstyle:check  # lint (falha em warning)
mvn clean install     # ciclo completo
```

---

## Anti-Patterns

| Anti-Pattern                                                | Correção                                                          |
| ----------------------------------------------------------- | ----------------------------------------------------------------- |
| `@Service` / `@Component` em `domain..` ou `application..`   | Registrar como `@Bean` em `infrastructure/config`                 |
| Import de Spring, AWS SDK, Retrofit ou OkHttp no núcleo      | Isolar atrás de porta em `application/port/out`                   |
| `com.fasterxml.jackson..` em `domain..`                      | Serialização só em DTO de `presentation` ou payload de `infrastructure` |
| Classe concreta em `application.port..`                      | Porta é sempre `interface`                                        |
| Caso de uso sem sufixo `UseCase`                             | Renomear — `NamingConventionTest` quebra o build                  |
| Controller injetando adaptador ou `DynamoDbTable`            | Injetar apenas caso de uso e mapper                               |
| Regra de negócio dentro do caso de uso                       | Mover para `domain/policy`                                        |
| `String cnpj` atravessando camadas                           | Value object `Cnpj` com `Cnpj.of(...)`                            |
| Validação de invariante repetida fora do value object        | Validar uma vez na fábrica estática                               |
| DTO de API usado em `application` ou `domain`                | Converter no `EligibilityApiMapper`                               |
| `CnpjPermissionItem` vazando do adaptador                    | Converter no `CnpjPermissionMapper`                               |
| Conversão manual campo a campo no controller                 | Mapper MapStruct com todos os campos cobertos                     |
| `null` para indicar registro ausente                         | `Optional.empty()`                                                |
| Stacktrace ou nome de classe no corpo da resposta            | `code` estável + mensagem genérica; detalhe só no `log.error`     |
| `catch` que engole a exceção e devolve default               | Traduzir em `*UnavailableException` e deixar o handler responder  |
| Literal de string onde existe enum                           | Membro de `DenialReason`                                          |
| Número ou string mágica no código                            | Constante nomeada (`private static final`)                        |
| Credencial em código, `application.yml` ou log               | Variável de ambiente / cadeia padrão do SDK                       |
| `@Value("${...}")` espalhado                                 | Classe `*Properties` com `@ConfigurationProperties`               |
| Schema DynamoDB alterado só no `scripts/seed-dynamo.sh`      | Fonte de verdade é `infra/terraform/dynamodb.tf`; seed acompanha  |
| Permissão IAM ampla no adaptador novo                        | Menor privilégio explícito em `infra/terraform/iam.tf`            |
| API de Java 17+ ou pacote `jakarta.*`                        | Java 11 e `javax.*` (Spring Boot 2.7)                             |
