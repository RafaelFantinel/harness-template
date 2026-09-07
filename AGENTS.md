# Eligibility Service - Instruções para Agentes

## Instruções específicas de ambiente

### Infraestrutura

- **LocalStack 3.4** (apenas DynamoDB) roda via Docker (`docker compose up -d`), exposto na porta `4566`. Precisa estar de pé antes de qualquer teste ou execução que toque o DynamoDB.
- O script `scripts/seed-dynamo.sh` é montado em `/etc/localstack/init/ready.d/` e cria/popula a tabela `cnpj-product-control` (chave `cnpj` HASH + `regiao` RANGE) automaticamente na subida do container.
- Não existe banco relacional, fila nem cache no projeto. As únicas dependências externas são o **serviço de toggles** (HTTP) e o **DynamoDB**.
- Credenciais AWS vêm da cadeia padrão do SDK (task role no ECS). Para LocalStack, usar `AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test`. **Nunca** colocar credencial em código ou em `application.yml`.

### Rodando a aplicação

- Build completo: `mvn clean install` (compila, roda testes, checkstyle e ArchUnit).
- Subir o serviço sem rede externa: `java -jar <jar>-boot.jar --spring.profiles.active=fake`. No profile `fake`, o `FakeToggleAdapter` responde localmente (região `NORTE` desligada).
- Com DynamoDB local: `DYNAMO_ENDPOINT=http://localhost:4566 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test java -jar ... --spring.profiles.active=fake`.
- Escuta na porta **8080**. Healthcheck em `/actuator/health` (probes habilitados).
- Configuração toda por variável de ambiente com default no `application.yml`: `TOGGLE_BASE_URL`, `TOGGLE_PRODUCT`, `TOGGLE_TIMEOUT_SECONDS`, `DYNAMO_TABLE_NAME`, `DYNAMO_ENDPOINT`, `AWS_REGION`.

### Testes

- Tudo roda por Maven: `mvn test` (unitários + ArchUnit), `mvn checkstyle:check` (falha em `warning`), `mvn verify` para o ciclo completo.
- Testes unitários vivem em `src/test/java/**` espelhando o pacote de produção (`domain/policy`, `application/usecase`, `presentation/api`, `infrastructure/toggle`).
- Testes de arquitetura em `src/test/java/br/com/acme/eligibility/architecture/` — são a **fonte de verdade** das regras estruturais, não a documentação.
- `archRule.failOnEmptyShould=true` está ligado: regra ArchUnit que não casa com nenhuma classe quebra o build.
- Chamadas HTTP ao serviço de toggles devem ser mockadas (MockWebServer/OkHttp ou stub do `TogglePort`). Nunca bater em serviço real em teste.

### Cuidados principais

- **Java 11** (`maven.compiler.release=11`) e Spring Boot 2.7. Não usar API de Java 17+ nem `jakarta.*` — o stack ainda é `javax.*`.
- Lombok e MapStruct rodam como annotation processors na mesma cadeia (`lombok-mapstruct-binding`). Ao mexer no `maven-compiler-plugin`, manter a ordem dos `annotationProcessorPaths`.
- MapStruct está com `unmappedTargetPolicy=ERROR`: campo de destino não mapeado quebra a compilação. Mapper novo precisa cobrir todos os campos.
- Domínio e aplicação **não podem** importar Spring, AWS SDK, Retrofit, OkHttp ou Jackson. O `CleanDomainTest` reprova o build.
- O controller não pode depender de `infrastructure.dynamo..` nem `infrastructure.toggle..` — só de portas e casos de uso (`NamingConventionTest`).
- Mensagem de erro de saída é genérica (`INTERNAL_ERROR`). Nunca vazar stacktrace, nome de classe ou detalhe de infraestrutura na resposta HTTP.
- Terraform em `infra/terraform` usa IAM de menor privilégio (task role só com `GetItem`/`Query`). Ao adicionar acesso a novo recurso, ampliar a policy de forma explícita e mínima.

### Planejar e implementar

- O planejamento deve usar a skill `xxxxxxx`.
- A implementação deve usar a skill `xxxxx`, seguida estritamente.

---

## Princípios de Arquitetura

**Estrutura (Clean/Hexagonal por camada):**

- `domain` = regra de negócio pura (modelos, value objects, policies, exceções de domínio). Sem framework.
- `application` = casos de uso e portas de saída (`port/out`). Sem framework.
- `infrastructure` = adaptadores (Retrofit para toggles, DynamoDB Enhanced), configuração de beans, properties.
- `presentation` = controller REST, DTOs, mappers de API, tratamento de erro e `main`.

Dependências apontam sempre para dentro: `presentation → infrastructure → application → domain`.

**Organização de pacotes:**

- Um conceito de negócio = um tipo no `domain/model` (`Cnpj`, `Regiao`, `Dicom`, `CnpjPermission`, `ProductToggle`, `EligibilityDecision`, `DenialReason`).
- Toda dependência externa entra por uma porta em `application/port/out`, implementada por um adaptador em `infrastructure/<tecnologia>/`.
- DTO de API só existe em `presentation/api/dto` e nunca atravessa para `application` ou `domain` — a conversão é do `EligibilityApiMapper`.
- Item de persistência (`CnpjPermissionItem`) é detalhe de `infrastructure/dynamo` e não vaza para o núcleo; a conversão é do `CnpjPermissionMapper`.
- Properties de infraestrutura ficam em classes `*Properties` no pacote do adaptador, registradas em `infrastructure/config`.

**Convenções aplicadas por ArchUnit:**

1. Camadas respeitadas (`LayeredArchitectureTest`)
2. Domínio sem framework | 3. Aplicação sem framework (`CleanDomainTest`)
4. `@RestController` termina em `Controller` | 5. Tudo em `application.port..` é `interface`
6. Tudo em `application.usecase..` termina em `UseCase` | 7. Controller não toca adaptador (`NamingConventionTest`)

### Carregamento progressivo de documentação

**IMPORTANTE**: carregue apenas o documento relevante à tarefa atual. NÃO carregue toda a documentação de uma vez.

#### Árvore de decisão: o que ler (ordem de prioridade)

**Tarefas de implementação (escrever código):**

- **Criar controller, caso de uso ou adaptador de persistência** → `docs/coding-patterns.md`
  - Portas e adaptadores, controller enxuto, mapeamento DTO↔domínio, nomenclatura, isolamento do domínio
- **Integrar API externa, cliente HTTP, observabilidade** → `docs/integration-patterns.md`
  - Encapsulamento de cliente Retrofit, timeouts, tradução de erro para `ToggleUnavailableException`, logs e métricas

> Os dois arquivos em `docs/` estão **vazios** hoje. Até serem preenchidos, a fonte de verdade das regras estruturais são os testes em `src/test/java/br/com/acme/eligibility/architecture/`. Se você derivar um padrão novo durante uma implementação, escreva-o no doc correspondente.

**Tarefas de arquitetura/design** → skill `architecture` (`.agents/skills/architecture/SKILL.md`).

> A skill cobre a arquitetura hexagonal deste serviço: princípios P1–P12 (`references/principles.md`), criação de classes por camada (`references/scaffolding.md`), adaptadores Retrofit/DynamoDB (`references/adapters.md`) e conformidade via ArchUnit/Checkstyle (`references/verification.md`). Em caso de divergência, os testes ArchUnit vencem a documentação.

#### Referência rápida por tipo de tarefa

| Tipo de tarefa                     | Doc principal                  | Observação                                  |
| ---------------------------------- | ------------------------------ | ------------------------------------------- |
| Novo modelo/value object de domínio | `docs/coding-patterns.md`      | Seção de nomenclatura e validação           |
| Novo caso de uso                    | `docs/coding-patterns.md`      | Sufixo `UseCase`, sem framework             |
| Nova porta + adaptador              | `docs/coding-patterns.md`      | Porta é interface em `application/port/out` |
| Novo endpoint / DTO                 | `docs/coding-patterns.md`      | Controller enxuto, mapper de API            |
| Integração com API externa          | `docs/integration-patterns.md` | Encapsulamento de cliente Retrofit          |
| Timeout, retry, erro de rede        | `docs/integration-patterns.md` | Seções de resiliência                       |
| Tabela/índice novo no DynamoDB      | `docs/integration-patterns.md` | Ajustar também `infra/terraform` e IAM      |
| Avaliar limite de camada            | Testes ArchUnit                | `architecture/*Test.java`                   |
| Checagem de conformidade            | Testes ArchUnit + Checkstyle   | `mvn test` e `mvn checkstyle:check`         |

---

## Regras Gerais

- Não faça alterações fora do escopo solicitado.
- Não remova código existente sem justificar a necessidade.
- Prefira alterações pequenas e incrementais.
- Não invente APIs, configurações ou comportamentos que não existam no projeto.
- Preserve as convenções existentes do projeto.


## Arquitetura

- Respeitar a separação entre domínio, aplicação e infraestrutura.
- Não acessar diretamente a infraestrutura a partir da camada de domínio.
- Reutilizar serviços existentes antes de criar novos.
- Não duplicar regras de negócio.
- Novas dependências devem ser justificadas antes de adicionadas.


## Segurança

- Nunca expor secrets, tokens ou credenciais.
- Nunca inserir credenciais diretamente no código.
- Não desabilitar mecanismos de segurança para contornar erros.
- Não commitar arquivos `.env` contendo credenciais.
- Dados sensíveis devem utilizar os mecanismos de configuração/secret management existentes.
- Nunca ignore as proteções do repositório ou os processos de revisão obrigatórios.

## Git

- Nunca fazer commit diretamente nas branches `main` ou `dev`.
- Toda alteração deve ser desenvolvida em uma branch de trabalho.
- Todo commit deve seguir a skill `git-commit`.
- Não fazer push diretamente para `main` ou `dev`.
- Todo commit deve incluir:
  `Co-authored-by: Devin <devin@cognition.ai>`
- Nunca criar um commit sem o coautor Devin.
- Não fazer commits fora do escopo da tarefa solicitada.
- Toda branch deve começar com `feature/descricao-breve`, independente do tipo.
- Não fazer force push


### Context7 MCP

Sempre use o Context7 quando precisar de geração de código, passos de setup/configuração ou documentação de biblioteca/API. Ou seja: use as ferramentas do Context7 MCP para resolver o id da lib e buscar a doc automaticamente, sem eu precisar pedir.

### Escrevendo planos de implementação

Você escreve código de qualidade e manutenível, evitando overengineering. Seja pragmático e siga primeiro as diretrizes dos docs, em vez de seguir cegamente padrões de mercado.

Planos de implementação devem sempre incluir build, lint e testes. Para isso, use `mvn clean install`, `mvn checkstyle:check` e `mvn test`.

### Implementação e testes

IMPORTANTE: sempre inclua testes cobrindo os caminhos importantes. Os planos precisam prever uma suíte que cubra happy paths e edge cases — especialmente CNPJ inválido, região desconhecida, toggle desligado, toggle indisponível (`503`), CNPJ não cadastrado e CNPJ bloqueado. Testes de alta qualidade que deem confiança e cubram a maior parte da implementação.

### Persistência e infraestrutura

Nunca altere o schema da tabela DynamoDB apenas no `scripts/seed-dynamo.sh` — a definição real vive em `infra/terraform/dynamodb.tf`, e o seed precisa acompanhar. Toda permissão nova de IAM vai em `infra/terraform/iam.tf`, sempre com o mínimo privilégio.

---