# Referência de Verificação

Comandos de build, regras ArchUnit, detecções por `grep`, pontuação de maturidade e checklists.

---

# Seção 1: O Build É a Primeira Verificação

```bash
mvn clean install     # compila + testes + Checkstyle + ArchUnit
mvn test              # unitários + ArchUnit
mvn checkstyle:check  # falha em severidade "warning"
mvn verify            # ciclo completo
```

> ⚠️ Hoje esses comandos falham com `Child module ... does not exist`: o `pom.xml` raiz declara
> cinco módulos (`eligibility-domain`, `eligibility-application`, `eligibility-infrastructure`,
> `eligibility-presentation`, `eligibility-architecture-tests`) que não existem em disco. **Alinhe o
> `pom.xml` com o layout real, ou faça o split físico — decisão do usuário, não sua.** Enquanto isso,
> use as detecções por `grep` da Seção 3 e leia os testes ArchUnit para conferir as regras.

## O que o Checkstyle enforça (severidade `warning` = build quebrado)

| Regra | Limite |
|-------|--------|
| `LineLength` | 120 (ignora `package`, `import`, URLs) |
| `MethodLength` | 50 linhas |
| `ParameterNumber` | 6 |
| `NestedIfDepth` | 2 |
| `CyclomaticComplexity` | 10 |
| `Indentation` | 4 (wrap 8) |
| `CustomImportOrder` | `STATIC` depois `THIRD_PARTY`, alfabético, linha em branco entre grupos |
| `AvoidStarImport`, `UnusedImports`, `RedundantImport` | — |
| `JavadocType` (escopo `public`) | Javadoc obrigatório em tipo público |
| `IllegalCatch`, `IllegalThrows` | Sem `catch (Exception/Throwable)` |
| `VisibilityModifier` | Campo não pode ser `protected` nem package-private |
| `FinalClass`, `HideUtilityClassConstructor`, `InterfaceIsType` | — |

Supressões: `config/checkstyle/suppressions.xml` (hoje só `HideUtilityClassConstructor` na
`*Application.java`). Ampliar supressão é decisão de arquitetura — justifique no PR.

---

# Seção 2: Regras ArchUnit — a Fonte de Verdade

Local: `src/test/java/br/com/acme/eligibility/architecture/`.
`archRule.failOnEmptyShould=true` está ligado: **regra que não casa com nenhuma classe quebra o build.**

## Regras vigentes

| Teste | Regra | Garante |
|-------|-------|---------|
| `LayeredArchitectureTest` | `LAYERS_ARE_RESPECTED` | `Presentation` não é acessada por ninguém; `Infrastructure` só por `Presentation`; `Application` só por `Infrastructure`/`Presentation`; `Domain` por todas as de fora (P1) |
| `CleanDomainTest` | `DOMAIN_HAS_NO_FRAMEWORK` | `domain..` sem `org.springframework..`, `software.amazon..`, `retrofit2..`, `okhttp3..`, `javax.persistence..`, `com.fasterxml.jackson..` (P2) |
| `CleanDomainTest` | `APPLICATION_HAS_NO_FRAMEWORK` | `application..` sem `org.springframework..`, `software.amazon..`, `retrofit2..`, `okhttp3..` (P2) |
| `NamingConventionTest` | `CONTROLLERS_ARE_SUFFIXED` | `@RestController` termina em `Controller` (P5) |
| `NamingConventionTest` | `PORTS_ARE_INTERFACES` | Tudo em `application.port..` é `interface` (P3) |
| `NamingConventionTest` | `USE_CASES_ARE_SUFFIXED` | Tudo em `application.usecase..` termina em `UseCase` |
| `NamingConventionTest` | `CONTROLLERS_DO_NOT_TOUCH_ADAPTERS` | `presentation.api..` não depende de `infrastructure.dynamo..` nem `infrastructure.toggle..` (P5) |

## Escrevendo uma regra nova

```java
package br.com.acme.eligibility.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Javadoc explicando a convenção que a regra protege. */
@AnalyzeClasses(packages = "br.com.acme.eligibility", importOptions = ImportOption.DoNotIncludeTests.class)
class <Nome>Test {

    @ArchTest
    static final ArchRule <NOME_EM_MAIUSCULO> = classes()
            .that().resideInAPackage("br.com.acme.eligibility.<pacote>..")
            .should().<condicao>();
}
```

Convenções do projeto:
- `@AnalyzeClasses(packages = "br.com.acme.eligibility", importOptions = ImportOption.DoNotIncludeTests.class)`
- Campo `static final ArchRule` em `SCREAMING_SNAKE_CASE`, anotado com `@ArchTest`
- Agrupe por assunto no arquivo existente (camada, pureza, nomenclatura) antes de criar arquivo novo
- **Garanta que a regra tem alvo** — `failOnEmptyShould=true` reprova regra que não casa com nada

## Candidatas úteis ainda não implementadas

Considere adicionar quando o caso aparecer (P12):

```java
// Adaptadores implementam alguma porta
classes().that().resideInAPackage("..infrastructure.dynamo..").and().haveSimpleNameEndingWith("Adapter")
        .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                "uma porta de saida", ...));

// Núcleo sem estereótipo Spring (P10)
noClasses().that().resideInAnyPackage("..domain..", "..application..")
        .should().beAnnotatedWith(org.springframework.stereotype.Component.class);

// DTO não sai da borda (P6)
noClasses().that().resideInAnyPackage("..domain..", "..application..")
        .should().dependOnClassesThat().resideInAPackage("..presentation.api.dto..");

// *Item de persistência não vaza (P7)
noClasses().that().resideInAnyPackage("..domain..", "..application..", "..presentation..")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("Item");
```

Antes de adicionar, cheque com os `grep` da Seção 3 se a base já está limpa — senão a regra nasce vermelha.

---

# Seção 3: Detecções por `grep`

Rode a partir da raiz do projeto. **Saída vazia = sem violação**, salvo indicação em contrário.

## Pureza do núcleo (P2) — rode primeiro

```bash
# 1. Framework no domínio (a violação mais crítica)
grep -rE "^import (org\.springframework|software\.amazon|retrofit2|okhttp3|javax\.persistence|com\.fasterxml\.jackson)" \
  src/main/java/br/com/acme/eligibility/domain/

# 2. Framework na aplicação
grep -rE "^import (org\.springframework|software\.amazon|retrofit2|okhttp3)" \
  src/main/java/br/com/acme/eligibility/application/

# 3. Estereótipo Spring no núcleo (P10)
grep -rE "@(Service|Component|Repository|Autowired)\b" \
  src/main/java/br/com/acme/eligibility/domain/ src/main/java/br/com/acme/eligibility/application/

# 4. @Value do Spring no núcleo — o do Lombok é permitido, então cheque o import, não a anotação
grep -rE "^import org\.springframework\.beans\.factory\.annotation\.Value" \
  src/main/java/br/com/acme/eligibility/domain/ src/main/java/br/com/acme/eligibility/application/
```

> `@Value` do **Lombok** (`lombok.Value`) é o idioma dos modelos de domínio deste projeto
> (`ProductToggle`, `EligibilityDecision`, `EligibilityRequest`, `CnpjPermission`) — não é violação.
> A violação é `org.springframework.beans.factory.annotation.Value`.

## Direção das dependências (P1)

```bash
# Domínio importando qualquer camada de fora
grep -rE "^import br\.com\.acme\.eligibility\.(application|infrastructure|presentation)" \
  src/main/java/br/com/acme/eligibility/domain/

# Aplicação importando camada de fora
grep -rE "^import br\.com\.acme\.eligibility\.(infrastructure|presentation)" \
  src/main/java/br/com/acme/eligibility/application/

# Infraestrutura importando a borda HTTP
grep -rE "^import br\.com\.acme\.eligibility\.presentation" \
  src/main/java/br/com/acme/eligibility/infrastructure/
```

## Portas e adaptadores (P3, P4)

```bash
# Porta que não é interface
grep -rLE "^public interface" src/main/java/br/com/acme/eligibility/application/port/

# Caso de uso sem o sufixo
find src/main/java/br/com/acme/eligibility/application/usecase -name '*.java' ! -name '*UseCase.java'

# Porta sem adaptador correspondente
for port in src/main/java/br/com/acme/eligibility/application/port/out/*Port.java; do
  name=$(basename "$port" .java)
  grep -rql "implements $name" src/main/java/br/com/acme/eligibility/infrastructure/ \
    || echo "porta sem adaptador: $name"
done
```

## Borda HTTP (P5, P6, P7, P9)

```bash
# Controller tocando adaptador ou SDK (escopo: presentation/api — ver nota abaixo)
grep -rE "^import (br\.com\.acme\.eligibility\.infrastructure|software\.amazon|retrofit2|okhttp3)" \
  src/main/java/br/com/acme/eligibility/presentation/api/

# DTO cruzando para o núcleo
grep -rE "^import br\.com\.acme\.eligibility\.presentation\.api\.dto" \
  src/main/java/br/com/acme/eligibility/domain/ src/main/java/br/com/acme/eligibility/application/

# Tipo de persistência vazando do pacote do adaptador
grep -rE "^import br\.com\.acme\.eligibility\.infrastructure\.(dynamo|toggle)\." \
  src/main/java/br/com/acme/eligibility/domain/ \
  src/main/java/br/com/acme/eligibility/application/ \
  src/main/java/br/com/acme/eligibility/presentation/

# Vazamento de detalhe em resposta de erro (inspecione manualmente os hits)
grep -rnE "getStackTrace|printStackTrace|getClass\(\)\.getName\(\)|exception\.getMessage\(\)" \
  src/main/java/br/com/acme/eligibility/presentation/
```

> **Exceção legítima:** o `ApiExceptionHandler` (`presentation.exception`) importa
> `infrastructure.toggle.ToggleUnavailableException` — é o ponto onde a falha de infraestrutura
> vira status HTTP (P9), e `LayeredArchitectureTest` permite `Presentation → Infrastructure`.
> A regra `CONTROLLERS_DO_NOT_TOUCH_ADAPTERS` vale só para `presentation.api..`, por isso o `grep`
> acima é escopado nesse pacote. O que o handler **não** pode fazer é repassar a mensagem da exceção
> para o corpo da resposta.

## Java 11 e stack (regressões comuns)

```bash
# jakarta.* não existe neste stack (Spring Boot 2.7 / Java 11)
grep -rE "^import jakarta\." src/main/java/

# API de Java 17+ frequentemente introduzida por engano
grep -rnE "\brecord\s+[A-Z]|\bsealed\b|\.toList\(\)|instanceof [A-Z][A-Za-z]* [a-z]" src/main/java/
```

Os hits do segundo comando pedem inspeção — `instanceof` com pattern matching e `Stream.toList()`
não compilam em Java 11.

## Segurança

```bash
# Segredo hard-coded (inspecione cada hit)
grep -rniE "(password|secret|token|api[_-]?key|accesskey)\s*=\s*\"" src/main/java/ src/main/resources/

# Nunca leia nem exponha .env; confira apenas que está ignorado
grep -n "^\.env" .gitignore
```

## Terraform e seed alinhados

```bash
# Atributos da tabela no Terraform vs no seed
grep -nE "name|hash_key|range_key|attribute" infra/terraform/dynamodb.tf
grep -nE "AttributeName|KeySchema" scripts/seed-dynamo.sh

# Ações IAM concedidas — devem ser o mínimo (hoje GetItem/Query)
grep -nE "dynamodb:[A-Za-z]+" infra/terraform/iam.tf
```

---

# Seção 4: Pontuação de Maturidade

## Processo

1. Rode a Seção 1 (build) e a Seção 3 (detecções)
2. Pontue cada princípio de 1 a 10
3. Aplique os pesos abaixo e normalize para 100
4. Classifique o nível e gere recomendações P0/P1/P2

## Pesos

| Princípio | Peso | Nota |
|-----------|------|------|
| P1 Regra de Dependência | 1.5 | Maior peso — quebra a arquitetura inteira |
| P2 Núcleo Livre de Framework | 1.5 | Idem |
| P3 Porta é Interface de Saída | 1.2 | |
| P4 Adaptador por Tecnologia | 1.0 | |
| P5 Controller Enxuto | 1.0 | |
| P6 DTO Não Atravessa | 0.8 | |
| P7 Persistência Não Vaza | 0.8 | |
| P8 Validação no Value Object | 0.9 | |
| P9 Erro Traduzido na Borda | 1.3 | Impacto de segurança |
| P10 Núcleo Instanciado por Config | 0.8 | |
| P11 Properties por Adaptador | 0.6 | |
| P12 Regra Vive no Teste | 0.6 | |

Peso total: 12,0 → normalize: `(soma ponderada / 120) * 100`.

## Níveis

| Nível | Pontuação | Características |
|-------|-----------|-----------------|
| **Imaturo** | 0–40 | Framework no núcleo, dependência invertida, vazamento em resposta de erro |
| **Em Desenvolvimento** | 41–65 | Camadas em geral respeitadas, mas com portas ausentes ou controller gordo |
| **Maduro** | 66–85 | Zero violação crítica; ArchUnit cobre as regras principais; build verde |
| **Avançado** | 86–100 | Regras estruturais todas cobertas por `@ArchTest`; segurança e resiliência verificadas |

## Template de relatório

```markdown
# Relatório de Conformidade de Arquitetura

**Data**: [data]
**Serviço**: eligibility-service (hexagonal, Java 11 / Spring Boot 2.7)

## Sumário Executivo
- **Nível de maturidade**: [Imaturo/Em Desenvolvimento/Maduro/Avançado]
- **Pontuação**: X/100
- **Build**: `mvn clean install` [verde / vermelho — motivo]

## Conformidade por Princípio (P1–P12)
[princípio a princípio, com a saída do comando de detecção]

## Recomendações por Prioridade
### P0 — Crítica  (pureza do núcleo, direção de dependência, vazamento em erro, segredo exposto)
### P1 — Alta     (porta ausente, controller gordo, DTO/Item atravessando, cobertura de teste)
### P2 — Média    (nomenclatura, properties, Checkstyle, regra ArchUnit faltante)
```

---

# Seção 5: Checklists

## Antes de abrir PR

```
□ mvn clean install verde (ou a discrepância do pom.xml explicitamente registrada)
□ mvn checkstyle:check verde
□ mvn test verde — inclusive os três testes ArchUnit
□ Detecções da Seção 3 sem hits em pureza do núcleo e direção de dependência
□ Nenhum import jakarta.* nem API de Java 17+
□ Nenhum segredo em código, application.yml ou log
□ Testes cobrindo happy path + CNPJ inválido, região desconhecida, toggle desligado,
  toggle indisponível (503), CNPJ não cadastrado, CNPJ bloqueado
□ Chamada externa mockada — nenhum teste bate em serviço real
□ Mudança em DynamoDB alinhou dynamodb.tf, iam.tf e seed-dynamo.sh
□ Convenção estrutural nova tem @ArchTest
□ Pattern novo documentado em docs/coding-patterns.md ou docs/integration-patterns.md
□ README atualizado se o comportamento externo mudou
```

## Feature nova

```
□ Cada classe classificada pela camada correta (scaffolding.md, Parte 0)
□ Dependência externa nova entra por porta em application/port/out
□ Adaptador em infrastructure/<tecnologia>/ com *Properties, *Mapper e exceção próprios
□ Caso de uso e policy expostos por @Bean em infrastructure/config
□ DTO só em presentation/api/dto; conversão pelo mapper de API
□ Erro novo mapeado no ApiExceptionHandler com corpo genérico
□ Teste unitário no pacote espelhado em src/test/java/**
```

---

# Seção 6: Automação (CI)

```yaml
- name: Build, lint e testes
  run: mvn -B clean verify   # compila, Checkstyle e ArchUnit no mesmo ciclo

- name: Pureza do núcleo
  run: |
    if grep -rqE "^import (org\.springframework|software\.amazon|retrofit2|okhttp3)" \
      src/main/java/br/com/acme/eligibility/domain/ \
      src/main/java/br/com/acme/eligibility/application/; then
      echo "❌ framework importado no núcleo"; exit 1
    fi

- name: Stack Java 11
  run: |
    if grep -rq "^import jakarta\." src/main/java/; then
      echo "❌ jakarta.* não existe em Spring Boot 2.7 / Java 11"; exit 1
    fi
```

O `mvn verify` já cobre ArchUnit e Checkstyle; os passos de `grep` são rede de segurança para o
período em que o `pom.xml` estiver desalinhado com o layout em disco.
