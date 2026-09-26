# eligibility-service

Serviço que responde se um CNPJ pode usar o produto em uma região, combinando um
**serviço externo de toggles** (HTTP, via Retrofit) com uma **tabela DynamoDB** de controle por CNPJ.

## Stack

Java 11 · Maven multi-módulo · Spring Boot 2.7 · Lombok · MapStruct · Retrofit/OkHttp ·
AWS SDK v2 (DynamoDB Enhanced) · ArchUnit · Checkstyle · Terraform (ECS Fargate)

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `eligibility-domain` | Regras e objetos de valor puros. Sem framework. |
| `eligibility-application` | Casos de uso e portas de saída. Sem framework. |
| `eligibility-infrastructure` | Adaptadores: Retrofit (toggles) e DynamoDB. Beans Spring. |
| `eligibility-presentation` | Controller REST, DTOs, tratamento de erro, `main`. |
| `eligibility-architecture-tests` | Regras ArchUnit sobre o artefato inteiro. |

Dependências apontam sempre para dentro: `presentation → infrastructure → application → domain`.
O ArchUnit falha o build se essa direção for invertida ou se domínio/aplicação importarem framework.

## Rota

`POST /v1/elegibilidade`

```json
{ "cnpj": "12.345.678/0001-95", "dicom": "DCM-1", "regiao": "sudeste" }
```

Resposta `200`:

```json
{ "cnpj": "12345678000195", "regiao": "SUDESTE", "eligible": false, "reason": "CNPJ_BLOCKED" }
```

Fluxo: consulta o toggle do produto (`GET /toggles/{product}` com header `X-Cnpj` e query `regiao`/`dicom`); se o produto
estiver desligado para a região, responde negativo **sem** ler o DynamoDB. Caso contrário, lê o item
`(cnpj, regiao)` da tabela de controle e decide.

`reason` (apenas quando `eligible=false`): `PRODUCT_DISABLED_FOR_REGION`, `CNPJ_NOT_REGISTERED`, `CNPJ_BLOCKED`.

Erros: `400 INVALID_REQUEST`, `503 TOGGLE_UNAVAILABLE`, `503 CONTROL_UNAVAILABLE`, `500 INTERNAL_ERROR` (mensagem genérica, sem detalhe interno).

## Rodando local

```bash
mvn clean install                      # build + testes + checkstyle + archunit

# toggles falso embutido, sem rede:
java -jar target/eligibility-service-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=fake
```

No profile `fake` o adaptador `FakeToggleAdapter` responde localmente (região `NORTE` desligada; `UNAVAILABLE` simula 503).
Para o DynamoDB local:

```bash
docker compose up -d                   # LocalStack + tabela populada
DYNAMO_ENDPOINT=http://localhost:4566 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  java -jar target/eligibility-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=fake
```

## Configuração

| Variável | Padrão | Uso |
|---|---|---|
| `TOGGLE_BASE_URL` | `http://localhost:8081` | Base do serviço de toggles |
| `TOGGLE_PRODUCT` | `eligibility-product` | Nome do toggle consultado |
| `TOGGLE_TIMEOUT_SECONDS` | `3` | Timeout de connect/read/call |
| `DYNAMO_TABLE_NAME` | `cnpj-product-control` | Tabela de controle |
| `DYNAMO_ENDPOINT` | *(vazio)* | Endpoint alternativo (LocalStack) |
| `AWS_REGION` | `us-east-1` | Região AWS |

Credenciais AWS vêm da cadeia padrão do SDK (task role no ECS). Nada de segredo em código.

## Infraestrutura

`infra/terraform` provisiona ECS Fargate atrás de um ALB, a tabela DynamoDB (PITR + criptografia),
log group, autoscaling por CPU e IAM de menor privilégio (task role só com `GetItem`/`Query` na tabela).

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # preencher vpc/subnets/imagem
terraform init && terraform plan
```

A imagem vem do `Dockerfile` na raiz (build multi-stage, JRE 11, usuário não-root).

## Qualidade

- `mvn checkstyle:check` — regras em `config/checkstyle/checkstyle.xml`, build falha em violação.
- `mvn -pl eligibility-architecture-tests test` — regras de camada, nomenclatura e isolamento do domínio.
