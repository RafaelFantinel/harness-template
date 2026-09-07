---
name: architecture
description: >
  Especialista na arquitetura hexagonal (Clean/Ports & Adapters) do eligibility-service — Java 11,
  Spring Boot 2.7, Maven, DynamoDB e Retrofit. SEMPRE leia esta skill ANTES de propor qualquer
  correção ou plano que envolva: criação de pacote ou classe, nova porta de saída, novo adaptador,
  novo caso de uso, novo endpoint, dependência entre camadas (`domain`, `application`,
  `infrastructure`, `presentation`), registro de bean, properties de infraestrutura, ou qualquer
  mudança estrutural. As regras aqui são enforced por ArchUnit e Checkstyle no build — raciocinar a
  partir de convenções genéricas de Spring sem ler esta skill vai produzir planos que quebram o
  build (ex.: anotar caso de uso com `@Service`, injetar adaptador no controller). Gatilhos: criar
  classe/pacote, "avaliar limite de camada", "checagem de conformidade", "architecture assessment",
  "arquitetura hexagonal", "ports and adapters", "porta", "adaptador", "caso de uso", "value
  object", "policy", "mapper", "ArchUnit", "camadas", "dependency rule", "boundaries", "revisar PR
  de arquitetura".
---

# Especialista em Arquitetura — eligibility-service

Arquitetura **hexagonal (Ports & Adapters)** em pacotes por camada, num único módulo Maven.
Stack: **Java 11** (`javax.*`, nada de Java 17+ nem `jakarta.*`), **Spring Boot 2.7**, Lombok,
MapStruct, Retrofit/OkHttp, AWS SDK v2 (DynamoDB Enhanced), JUnit 5 + AssertJ + Mockito, ArchUnit,
Checkstyle.

## Filosofia Central

- **Núcleo puro**: `domain` e `application` não conhecem framework, I/O nem serialização
- **Tudo que é externo entra por uma porta**: interface em `application/port/out`, implementada por um adaptador em `infrastructure/<tecnologia>/`
- **Dependências apontam para dentro**: `presentation → infrastructure → application → domain`
- **Os testes ArchUnit são a fonte de verdade** das regras estruturais, não esta documentação. Divergiu? O teste vence — e esta skill deve ser corrigida.

## Estrutura Real do Projeto

```
src/main/java/br/com/acme/eligibility/
├── domain/                       # Regra de negócio pura — zero framework
│   ├── model/                    # Value objects e modelos (Cnpj, Regiao, Dicom,
│   │                             #   CnpjPermission, ProductToggle, EligibilityDecision, DenialReason)
│   ├── policy/                   # Regras de decisão (EligibilityPolicy)
│   └── exception/                # DomainValidationException
├── application/                  # Casos de uso — zero framework
│   ├── usecase/                  # *UseCase (CheckEligibilityUseCase)
│   ├── port/out/                 # Interfaces de saída (TogglePort, CnpjPermissionPort)
│   └── config/
├── infrastructure/               # Adaptadores e wiring do Spring
│   ├── dynamo/                   # CnpjPermissionDynamoAdapter, *Item, *Mapper, DynamoProperties
│   ├── toggle/                   # ToggleHttpAdapter, ToggleApi, *Mapper, *Properties, Fake*
│   └── config/                   # @Configuration: beans, Retrofit, Dynamo, properties
└── presentation/                 # Borda HTTP
    ├── api/                      # EligibilityController, EligibilityApiMapper
    │   └── dto/                  # *Dto — nunca cruzam para application/domain
    ├── exception/                # ApiExceptionHandler
    └── EligibilityApplication.java   # main

src/test/java/br/com/acme/eligibility/
├── architecture/                 # ArchUnit — fonte de verdade estrutural
└── <mesmo pacote da classe testada>/   # espelha o pacote de produção
```

**Regras de layout:**
- Espelhe o pacote de produção em `src/test/java/**`. Não há pasta `tests/` separada nem sufixo de pasta por tipo de teste.
- Um conceito de negócio = um tipo em `domain/model`.
- Um adaptador por tecnologia = um pacote em `infrastructure/<tecnologia>/`, com seu `*Properties`, `*Item`/`*Response` e `*Mapper` co-localizados.
- Nada de pacote `util`, `helper`, `common` ou `shared` genérico.

> ⚠️ **Discrepância conhecida do build**: o `pom.xml` raiz declara cinco módulos
> (`eligibility-domain`, `eligibility-application`, `eligibility-infrastructure`,
> `eligibility-presentation`, `eligibility-architecture-tests`) que **não existem em disco** — todo o
> código vive num único `src/` na raiz. Qualquer comando Maven falha com
> `Child module ... does not exist`. **O layout em disco descrito acima é a referência.** Não
> "conserte" o `pom.xml` nem faça o split físico por conta própria: decida com o usuário.

## Fundamentos Teóricos

| Pattern | Fonte | O que o eligibility-service adota |
|---------|-------|-----------------------------------|
| Ports & Adapters | A. Cockburn (2005) | Toda dependência externa entra por interface em `application/port/out` |
| Clean Architecture | R. Martin (2017) | Dependency rule enforced por `LayeredArchitectureTest` |
| Bounded Context / Value Object | E. Evans (DDD, 2003) | `Cnpj`, `Regiao`, `Dicom` validam na fábrica estática `of(...)` |
| Domain Policy | E. Evans, cap. 5 | Decisão de negócio em `domain/policy`, fora do caso de uso |
| Humble Object | G. Meszaros (xUnit Patterns) | Controller e adaptadores sem lógica — testáveis por unidade |
| Fitness Functions | Ford/Parsons/Kua (Building Evolutionary Architectures) | ArchUnit + Checkstyle quebrando o build |

## Os 12 Princípios

| #   | Princípio                          | Criticidade | Regra chave                                                                |
| --- | ---------------------------------- | ----------- | -------------------------------------------------------------------------- |
| 1   | **Regra de Dependência**           | 🔴 CRÍTICO  | `presentation → infrastructure → application → domain`; nunca ao contrário  |
| 2   | **Núcleo Livre de Framework**      | 🔴 CRÍTICO  | `domain` e `application` sem Spring, AWS SDK, Retrofit, OkHttp, Jackson     |
| 3   | **Porta é Interface de Saída**     | Alta        | Tudo em `application.port..` é `interface`, nomeada `*Port`                 |
| 4   | **Adaptador por Tecnologia**       | Alta        | `infrastructure/<tecnologia>/`, implementa uma porta, é `@Component`        |
| 5   | **Controller Enxuto**              | Alta        | Só valida DTO, chama caso de uso, mapeia resposta — nunca toca adaptador    |
| 6   | **DTO Não Atravessa**              | Alta        | `presentation.api.dto` fica na borda; conversão é do `EligibilityApiMapper` |
| 7   | **Persistência Não Vaza**          | Alta        | `*Item`/`*Response` são detalhe do adaptador; `*Mapper` converte p/ domínio |
| 8   | **Validação no Value Object**      | Alta        | Estado inválido é impossível: `Cnpj.of(...)` lança `DomainValidationException` |
| 9   | **Erro Traduzido na Borda**        | 🔴 CRÍTICO  | Resposta genérica (`INTERNAL_ERROR`); nunca vaze stacktrace/classe/infra    |
| 10  | **Núcleo Instanciado por Config**  | Alta        | Caso de uso e policy viram bean em `infrastructure/config`, não via `@Service` |
| 11  | **Properties por Adaptador**       | Média       | `*Properties` no pacote do adaptador, registrado em `infrastructure/config` |
| 12  | **Regra Vive no Teste**            | Alta        | Regra estrutural nova = novo `@ArchTest`, não só um parágrafo de doc        |

Detalhamento, exemplos de código e anti-exemplos: `references/principles.md`.

## Top 8 Violações Críticas

1. 🔴 **Import de framework no núcleo** — `org.springframework..`, `software.amazon..`, `retrofit2..`, `okhttp3..`, `javax.persistence..` ou `com.fasterxml.jackson..` em `domain`/`application` → `CleanDomainTest` reprova o build
2. 🔴 **Dependência invertida entre camadas** — `application` importando `infrastructure`, ou `domain` importando qualquer coisa acima → `LayeredArchitectureTest`
3. 🔴 **Controller tocando adaptador** — `presentation.api` importando `infrastructure.dynamo..` ou `infrastructure.toggle..` → `NamingConventionTest`; injete o caso de uso
4. 🔴 **Vazamento de detalhe interno na resposta HTTP** — stacktrace, nome de classe, mensagem do SDK ou do serviço externo no corpo do erro
5. 🟠 **Classe concreta em `application.port..`** — porta tem que ser `interface` (`PORTS_ARE_INTERFACES`)
6. 🟠 **Caso de uso sem sufixo `UseCase`** ou `@RestController` sem sufixo `Controller` → `NamingConventionTest`
7. 🟠 **`@Component`/`@Service` no núcleo** — o núcleo é instanciado por `@Bean` em `infrastructure/config` (ver P10)
8. 🟠 **DTO ou `*Item` cruzando camada** — `EligibilityRequestDto` chegando ao caso de uso, ou `CnpjPermissionItem` saindo do pacote `dynamo`

## Árvore de Decisão: Qual Reference Carregar

```
TIPO DE TAREFA                                    → CARREGAR
──────────────────────────────────────────────────────────────────────────
Criar modelo de domínio / value object / policy   → references/scaffolding.md (Parte 1)
Criar caso de uso                                 → references/scaffolding.md (Parte 2)
Criar porta + adaptador                           → references/scaffolding.md (Parte 3) + adapters.md
Criar endpoint / DTO / mapper de API              → references/scaffolding.md (Parte 4)
Integrar API externa (HTTP/Retrofit)              → references/adapters.md (Parte 1)
Persistência DynamoDB / tabela / índice novo      → references/adapters.md (Parte 2)
Timeout, erro de rede, resiliência, observabilid. → references/adapters.md (Parte 3)
Avaliar conformidade de arquitetura               → references/verification.md
Escrever/ajustar regra ArchUnit                   → references/verification.md (Seção 2)
Pontuação de maturidade / relatório               → references/verification.md (Seção 3)
Entender um princípio específico                  → references/principles.md
Decidir onde uma classe nova deve morar           → references/principles.md (P1–P4) + este arquivo
```

## Instruções por Caso de Uso

### Criar Código Novo

Carregue `references/scaffolding.md` e siga a ordem: **domínio → porta → caso de uso → adaptador → configuração de bean → endpoint → testes**.

1. Classifique o que está sendo criado: regra de negócio (`domain`), orquestração (`application`), I/O (`infrastructure`) ou borda HTTP (`presentation`)
2. Se envolve qualquer sistema externo, comece pela **porta** — a interface é a decisão de design; o adaptador é detalhe
3. Escreva o teste unitário junto, no pacote espelhado em `src/test/java/**`
4. Se criou regra estrutural nova, adicione um `@ArchTest` (P12)
5. Rode a verificação de `references/verification.md`

### Integrar Sistema Externo ou Mexer em Persistência

Carregue `references/adapters.md`. Patterns chave: encapsulamento do cliente Retrofit, timeout explícito,
tradução de erro para exceção do pacote do adaptador, `*Properties` por variável de ambiente,
`*Mapper` MapStruct com `unmappedTargetPolicy=ERROR`, e o par
`infra/terraform` + `scripts/seed-dynamo.sh` sempre alinhados.

### Avaliar Conformidade de Arquitetura

Carregue `references/verification.md`. Rode `mvn test` (ArchUnit), `mvn checkstyle:check` e os
comandos de detecção por `grep`; pontue cada princípio; produza relatório priorizado P0/P1/P2.

### Entender um Princípio Específico

Carregue `references/principles.md` — cada princípio traz definição, regras para agentes de IA,
exemplo correto e anti-exemplo, e qual teste o enforce.

## Checagem Rápida de Anti-Patterns

Antes de gerar qualquer código, verifique:

**Camadas (P1–P4):**
- [ ] Nenhum import de `org.springframework..`, `software.amazon..`, `retrofit2..`, `okhttp3..`, `javax.persistence..`, `com.fasterxml.jackson..` em `domain` ou `application`
- [ ] Nenhuma camada importa uma camada mais externa
- [ ] Toda dependência externa passa por uma `interface` `*Port` em `application/port/out`
- [ ] Adaptador vive em `infrastructure/<tecnologia>/` e implementa exatamente uma porta

**Borda (P5–P9):**
- [ ] Controller injeta caso de uso + mapper de API; nada de adaptador nem SDK
- [ ] DTO só existe em `presentation/api/dto`; `*Item`/`*Response` só no pacote do adaptador
- [ ] Value object valida na fábrica estática e lança `DomainValidationException`
- [ ] Resposta de erro é genérica; exceção de infra é traduzida no `ApiExceptionHandler`

**Wiring e convenção (P10–P12):**
- [ ] Caso de uso e policy expostos por `@Bean` em `infrastructure/config` (sem estereótipo Spring no núcleo)
- [ ] `*Properties` no pacote do adaptador, valores por variável de ambiente com default em `application.yml` — nunca credencial em código ou YAML
- [ ] Sufixos: `*UseCase`, `*Port`, `*Controller`, `*Adapter`, `*Mapper`, `*Properties`, `*Dto`, `*Item`
- [ ] Java 11 apenas (`javax.*`); mapper MapStruct cobre todos os campos de destino
- [ ] Teste unitário no pacote espelhado, cobrindo happy path e os edge cases do domínio (CNPJ inválido, região desconhecida, toggle desligado, toggle indisponível, CNPJ não cadastrado, CNPJ bloqueado)
- [ ] Regra estrutural nova tem `@ArchTest` correspondente
