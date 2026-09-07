# Referência dos Princípios de Arquitetura

Os 12 princípios da arquitetura hexagonal do eligibility-service, com regras, exemplo correto,
anti-exemplo e o teste que faz o enforcement.

## Resumo

| # | Princípio | Tipo | Criticidade | Enforcement |
|---|-----------|------|-------------|-------------|
| 1 | Regra de Dependência | Camada | 🔴 CRÍTICO | `LayeredArchitectureTest` |
| 2 | Núcleo Livre de Framework | Camada | 🔴 CRÍTICO | `CleanDomainTest` |
| 3 | Porta é Interface de Saída | Camada | Alta | `NamingConventionTest` |
| 4 | Adaptador por Tecnologia | Camada | Alta | Revisão + `LayeredArchitectureTest` |
| 5 | Controller Enxuto | Borda | Alta | `NamingConventionTest` + Checkstyle |
| 6 | DTO Não Atravessa | Borda | Alta | Revisão |
| 7 | Persistência Não Vaza | Borda | Alta | Revisão |
| 8 | Validação no Value Object | Domínio | Alta | Teste unitário |
| 9 | Erro Traduzido na Borda | Borda | 🔴 CRÍTICO | Revisão + teste de controller |
| 10 | Núcleo Instanciado por Config | Wiring | Alta | `CleanDomainTest` |
| 11 | Properties por Adaptador | Wiring | Média | Revisão |
| 12 | Regra Vive no Teste | Processo | Alta | Revisão de PR |

---

## P1: Regra de Dependência 🔴

As dependências apontam sempre para dentro: `presentation → infrastructure → application → domain`.

**Regras:**
- ✅ `domain` não importa nada do projeto além dele mesmo
- ✅ `application` importa apenas `domain`
- ✅ `infrastructure` importa `application` e `domain`
- ✅ `presentation` pode importar todas as camadas de dentro
- ❌ Nunca faça uma camada interna conhecer uma externa (nem por interface "de conveniência")

```java
// ✅ CORRETO: a aplicação depende da abstração que ela mesma define
package br.com.acme.eligibility.application.usecase;

import br.com.acme.eligibility.application.port.out.TogglePort;   // mesma camada
import br.com.acme.eligibility.domain.model.EligibilityDecision;  // camada de dentro

// ❌ ERRADO: aplicação conhecendo o adaptador
import br.com.acme.eligibility.infrastructure.toggle.ToggleHttpAdapter;
```

**Enforcement:** `LayeredArchitectureTest.LAYERS_ARE_RESPECTED`.
`Presentation` não pode ser acessada por ninguém; `Infrastructure` só por `Presentation`;
`Application` só por `Infrastructure` e `Presentation`.

---

## P2: Núcleo Livre de Framework 🔴

`domain` e `application` são Java puro. Nada de Spring, AWS SDK, Retrofit, OkHttp, Jackson ou JPA.

**Regras:**
- ✅ Lombok é permitido no núcleo (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j` — SLF4J é API, não framework de I/O)
- ✅ `java.util`, `java.time`, `java.util.regex` à vontade
- ❌ Nenhum import de `org.springframework..`, `software.amazon..`, `retrofit2..`, `okhttp3..` (bloqueados em `domain` e `application`)
- ❌ Nenhum import de `javax.persistence..` nem `com.fasterxml.jackson..` (bloqueados em `domain`)
- ❌ Sem anotação de serialização (`@JsonProperty`) ou de validação de framework (`@NotNull`) em modelo de domínio — validação é de P8

```java
// ✅ CORRETO: domain/model/ProductToggle.java — nenhum import de framework
// ❌ ERRADO: @JsonProperty("enabled") em ProductToggle — acopla o domínio ao wire format
```

**Enforcement:** `CleanDomainTest.DOMAIN_HAS_NO_FRAMEWORK` e `CleanDomainTest.APPLICATION_HAS_NO_FRAMEWORK`.

---

## P3: Porta é Interface de Saída

Toda dependência externa entra por uma interface declarada pela **aplicação**, em
`application/port/out`, nomeada `*Port`.

**Regras:**
- ✅ A porta fala o vocabulário do domínio — parâmetros e retorno são tipos de `domain/model`
- ✅ Uma porta por capacidade externa, com o menor número possível de métodos
- ✅ Tudo em `application.port..` é `interface` — sem classe, sem enum, sem constante
- ❌ Nunca exponha tipo de SDK, DTO ou `*Item` na assinatura da porta
- ❌ Nunca nomeie a porta pela tecnologia (`DynamoPort` ❌ → `CnpjPermissionPort` ✅)

```java
// ✅ CORRETO: application/port/out/CnpjPermissionPort.java
public interface CnpjPermissionPort {
    Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao);
}

// ❌ ERRADO: vocabulário da infraestrutura vazando na porta
public interface CnpjPermissionPort {
    Optional<CnpjPermissionItem> getItem(Key key);
}
```

**Enforcement:** `NamingConventionTest.PORTS_ARE_INTERFACES`.

---

## P4: Adaptador por Tecnologia

Cada porta é implementada por um adaptador em `infrastructure/<tecnologia>/`, junto do que só
importa àquela tecnologia.

**Regras:**
- ✅ Um pacote por tecnologia: `infrastructure/dynamo/`, `infrastructure/toggle/`
- ✅ O pacote agrupa adaptador + `*Properties` + `*Item`/`*Response` + `*Mapper` + exceção específica
- ✅ Adaptador é `@Component` e implementa exatamente uma porta
- ✅ Implementação alternativa por `@Profile` (ex.: `FakeToggleAdapter` com `@Profile("fake")`, `ToggleHttpAdapter` com `@Profile("!fake")`)
- ❌ Sem regra de negócio no adaptador — ele traduz e chama, nada mais

```java
// ✅ CORRETO: infrastructure/dynamo/CnpjPermissionDynamoAdapter.java
@Component
public class CnpjPermissionDynamoAdapter implements CnpjPermissionPort {
    private final DynamoDbTable<CnpjPermissionItem> table;
    private final CnpjPermissionMapper mapper;

    @Override
    public Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao) {
        Key key = Key.builder().partitionValue(cnpj.getValue()).sortValue(regiao.getValue()).build();
        return Optional.ofNullable(table.getItem(key)).map(mapper::toDomain);
    }
}
```

Detalhes por tecnologia: `references/adapters.md`.

---

## P5: Controller Enxuto

O controller recebe o DTO validado, chama o caso de uso e mapeia a resposta. Nada mais.

**Regras:**
- ✅ Injeta caso de uso + mapper de API
- ✅ `@RestController` termina em `Controller`
- ✅ Um método por rota, curto (Checkstyle: `MethodLength` máx. 50, `CyclomaticComplexity` máx. 10)
- ❌ Nunca injete adaptador, cliente HTTP, `DynamoDbEnhancedClient` ou porta diretamente
- ❌ Sem `if` de regra de negócio no controller — isso é `domain/policy`

```java
// ✅ CORRETO
@PostMapping
public ResponseEntity<EligibilityResponseDto> check(@Valid @RequestBody EligibilityRequestDto requestDto) {
    EligibilityDecision decision = checkEligibilityUseCase.execute(mapper.toDomain(requestDto));
    return ResponseEntity.ok(mapper.toResponse(decision));
}
```

**Enforcement:** `NamingConventionTest.CONTROLLERS_ARE_SUFFIXED` e
`NamingConventionTest.CONTROLLERS_DO_NOT_TOUCH_ADAPTERS` (nada em `presentation.api..` pode depender
de `infrastructure.dynamo..` ou `infrastructure.toggle..`).

---

## P6: DTO Não Atravessa

`*Dto` existe apenas em `presentation/api/dto` e representa o contrato HTTP — que muda por motivos
diferentes do domínio.

**Regras:**
- ✅ Validação de formato (`@Valid`, `@NotBlank`) fica no DTO; validação de invariante fica no value object (P8)
- ✅ A conversão DTO ↔ domínio é do `EligibilityApiMapper` (MapStruct)
- ❌ Caso de uso nunca recebe nem devolve DTO
- ❌ Modelo de domínio nunca é serializado direto na resposta

---

## P7: Persistência Não Vaza

`CnpjPermissionItem` (DynamoDB) e `ToggleResponse` (HTTP) são detalhes do adaptador.

**Regras:**
- ✅ `*Item`/`*Response` ficam no pacote do adaptador, junto do `*Mapper` que os converte
- ✅ `@Mapper` do MapStruct com `unmappedTargetPolicy=ERROR` — campo de destino não mapeado quebra a compilação
- ❌ Esses tipos nunca aparecem em `application` nem em `domain`
- ❌ Nunca reutilize o `*Item` como resposta da API

```java
// ✅ CORRETO: infrastructure/dynamo/CnpjPermissionMapper.java — Item → domínio, com value objects
@Mapper
public interface CnpjPermissionMapper {
    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "toCnpj")
    @Mapping(target = "regiao", source = "regiao", qualifiedByName = "toRegiao")
    CnpjPermission toDomain(CnpjPermissionItem item);
}
```

---

## P8: Validação no Value Object

Estado inválido é impossível de representar: a validação acontece na construção, dentro do domínio.

**Regras:**
- ✅ Construtor privado + fábrica estática `of(...)`
- ✅ Normalize na entrada (`Cnpj` guarda 14 dígitos, sem máscara)
- ✅ Classe `final`, imutável — `@Getter @EqualsAndHashCode @ToString` (como `Cnpj`) ou `@Value` do Lombok (como `ProductToggle`, `EligibilityDecision`, `EligibilityRequest`, `CnpjPermission`)
- ✅ Falha lança `DomainValidationException`
- ❌ Sem `String` crua atravessando o núcleo onde existe value object

```java
// ✅ CORRETO: domain/model/Cnpj.java
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
```

**Cuidado de segurança:** a mensagem da exceção vira resposta de erro — mantenha-a descritiva do
campo, nunca do valor recebido nem do estado interno.

---

## P9: Erro Traduzido na Borda 🔴

Falha de infraestrutura vira exceção do pacote do adaptador; o `ApiExceptionHandler` traduz para uma
resposta HTTP genérica.

**Regras:**
- ✅ Exceção específica por adaptador (`ToggleUnavailableException`), lançada no ponto da falha
- ✅ Mapeamento centralizado em `presentation/exception/ApiExceptionHandler`
- ✅ Detalhe técnico só no log estruturado (com correlação), nunca no corpo da resposta
- ❌ **NUNCA** vaze stacktrace, nome de classe, mensagem do SDK, host, tabela ou status do serviço externo na resposta
- ❌ Sem `catch (Exception)` genérico dentro do domínio (Checkstyle `IllegalCatch` também barra)

```java
// ✅ CORRETO: infrastructure/toggle/ToggleHttpAdapter.java
if (!response.isSuccessful() || response.body() == null) {
    throw new ToggleUnavailableException("toggle service returned status " + response.code());
}
// ... e o handler responde INTERNAL_ERROR / 503 genérico, com o detalhe apenas no log
```

**Nota:** `ApiExceptionHandler` (em `presentation.exception`) importar
`infrastructure.toggle.ToggleUnavailableException` é legítimo — é justamente o ponto de tradução, e
`LayeredArchitectureTest` permite `Presentation → Infrastructure`. A restrição
`CONTROLLERS_DO_NOT_TOUCH_ADAPTERS` vale só para `presentation.api..` (P5).

---

## P10: Núcleo Instanciado por Config

Casos de uso e policies são POJOs; o Spring os instancia por `@Bean` em `infrastructure/config`.
Isso é o que permite P2 na prática.

**Regras:**
- ✅ `@Configuration` em `infrastructure/config` declara o `@Bean` e injeta as portas
- ✅ O caso de uso recebe as dependências por construtor (`@RequiredArgsConstructor`)
- ❌ Sem `@Service`, `@Component`, `@Repository`, `@Autowired` ou `org.springframework...@Value` em `domain` ou `application`
- ⚠️ `@Value` do **Lombok** (`lombok.Value`) é permitido e é o idioma dos modelos deste projeto — não confunda com o `@Value` do Spring

```java
// ✅ CORRETO: infrastructure/config/UseCaseConfiguration.java
@Bean
public CheckEligibilityUseCase checkEligibilityUseCase(TogglePort togglePort,
                                                       CnpjPermissionPort cnpjPermissionPort,
                                                       EligibilityPolicy eligibilityPolicy) {
    return new CheckEligibilityUseCase(togglePort, cnpjPermissionPort, eligibilityPolicy);
}
```

---

## P11: Properties por Adaptador

Configuração de infraestrutura fica em `*Properties` no pacote do adaptador, registrada em
`infrastructure/config`.

**Regras:**
- ✅ Um `*Properties` por adaptador (`ToggleProperties`, `DynamoProperties`)
- ✅ Todo valor vem de variável de ambiente com default em `application.yml`
  (`TOGGLE_BASE_URL`, `TOGGLE_PRODUCT`, `TOGGLE_TIMEOUT_SECONDS`, `DYNAMO_TABLE_NAME`,
  `DYNAMO_ENDPOINT`, `AWS_REGION`)
- ✅ Credencial AWS vem da cadeia padrão do SDK (task role no ECS)
- ❌ **NUNCA** credencial, token ou segredo em código ou em `application.yml`

---

## P12: Regra Vive no Teste

A documentação descreve; o teste garante. Regra estrutural nova sem `@ArchTest` é decoração.

**Regras:**
- ✅ Nova convenção estrutural = novo `@ArchTest` em `src/test/java/br/com/acme/eligibility/architecture/`
- ✅ `archRule.failOnEmptyShould=true` está ligado: regra que não casa com nenhuma classe **quebra o build** — ao escrever a regra, garanta que ela tem alvo
- ✅ Divergência entre doc e teste: o teste vence; corrija a doc no mesmo PR
- ❌ Não relaxe uma regra ArchUnit para fazer código novo passar sem discutir com o usuário

Como escrever a regra: `references/verification.md`, Seção 2.

---

## Hierarquia em Conflito

**Princípios de camada (P1–P4) prevalecem sobre conveniência de borda e wiring (P5–P11).**

| Conflito | Resolução |
|----------|-----------|
| Reaproveitar o `*Item` como DTO evitaria um mapper | Duplique o tipo (P6, P7) — contrato HTTP e schema de tabela mudam por motivos diferentes |
| Anotar o caso de uso com `@Service` reduziria boilerplate | Mantenha o `@Bean` (P2, P10) — o núcleo puro é o que sustenta o teste unitário sem contexto Spring |
| Injetar o adaptador no controller encurtaria a chamada | Injete o caso de uso (P1, P5) — `NamingConventionTest` reprova |
| Devolver a mensagem do serviço externo ajudaria no debug | Log estruturado sim, resposta HTTP não (P9) |
| Um único `*Port` "genérico" para dois sistemas | Uma porta por capacidade (P3) — porta genérica vira acoplamento oculto |

**Regra prática:** se obedecer P5–P11 quebraria P1, P2 ou P3, pare e siga o princípio de camada.
