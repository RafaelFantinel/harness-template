---
name: create-e2e-tests
description: Cria testes e2e seguindo os patterns e convenções do projeto. Use ao criar testes e2e, escrever testes e2e, adicionar testes e2e, ou quando o usuário mencionar testes end-to-end.
---

# Create E2E Tests

> **Layout canônico de testes** — unit specs em `<aggregate>/__test__/<file>.spec.ts` (por aggregate); testes e2e em `__test__/e2e/` (raiz do package ou do subdomain) por fluxo. Veja `.specs/features/unit-tests-aggregate-folder-migration/` e `.specs/features/test-flat-restructure/`.

## Início Rápido

Ao criar testes, siga este workflow:

1. **Unit ou e2e?** — só mocks → unit (`<aggregate>/__test__/`); DB/Redis/HTTP → e2e (`__test__/e2e/`)
2. Determine o tipo do package (flat vs baseado em subdomain)
3. Crie o arquivo de teste no local correto com o sufixo correto
4. Suba a app NestJS, a config e o client de banco (somente e2e)
5. Configure os lifecycle hooks (beforeEach, afterEach, afterAll)
6. Escreva os testes seguindo o pattern Arrange-Act-Assert
7. Use test factories para criar dados; limpe as tabelas do banco após cada teste

## Patterns de Localização dos Testes

### Testes unitários — dentro de `<aggregate>/__test__/`

Coloque as unit specs **dentro** de `__test__/` na pasta do aggregate (não ao lado dos `.service.ts` / `.entity.ts` de produção):

```
package/{module}/{aggregate}/__test__/{file}.spec.ts
```

Exemplos de módulo flat:

| Arquivo fonte | Teste unitário |
|---------------|----------------|
| `subscription/subscription.service.ts` | `subscription/__test__/subscription.service.spec.ts` |
| `user/user-management.service.ts` | `user/__test__/user-management.service.spec.ts` |

Baseado em subdomain (`content`, `analytics`):

```
package/{module}/{subdomain}/{aggregate}/__test__/{file}.spec.ts
```

| Arquivo fonte | Teste unitário |
|---------------|----------------|
| `content/management/lifecycle/content-lifecycle.service.ts` | `content/management/lifecycle/__test__/content-lifecycle.service.spec.ts` |

**Regras:**
- Sufixo: `.spec.ts` (o Jest descobre por padrão)
- Imports da spec para o código de produção sobem um diretório: `from '../subscription.service'` (não `./` ao lado do service)
- Testes unitários puros usam apenas mocks (sem DB, sem Redis, sem `createNestApp`)
- **Não** use `__test__/unit/` nem outro aninhamento extra dentro do aggregate — apenas `__test__/*.spec.ts`

**Hierarquia:** `__test__/` local do aggregate = unit (classe única / mocks). `__test__/e2e/` na raiz do package ou do subdomain = integração atravessando boundaries.

**Detecção:** `find package -name "*.service.spec.ts" -not -path "*/__test__/*"` deve retornar **zero** para packages migrados (todas as unit specs de service vivem dentro de `__test__/`).

### Testes e2e — centralizados por fluxo

Coloque as specs e2e em `__test__/e2e/` **diretamente** — sem subpasta de feature para uma spec única:

```
package/{module}/__test__/e2e/<flow>.e2e-spec.ts
```

**Sufixo:** `.e2e-spec.ts` — distingue o setup pesado de integração das `.spec.ts` unitárias.

**Exemplos (packages flat):**
- `package/billing/__test__/e2e/subscription.e2e-spec.ts`
- `package/billing/__test__/e2e/subscription-lifecycle.e2e-spec.ts`
- `package/billing/__test__/e2e/invoice.e2e-spec.ts`
- `package/identity/__test__/e2e/authentication.e2e-spec.ts`
- `package/recommendations/__test__/e2e/recommendations.e2e-spec.ts`

**Múltiplos fluxos, mesma feature:** nomeie por fluxo e mantenha os arquivos no mesmo nível:

```
package/billing/__test__/e2e/
├── subscription.e2e-spec.ts
├── subscription-lifecycle.e2e-spec.ts
└── subscription-billing.e2e-spec.ts
```

**Subpasta em e2e SOMENTE quando ≥2–3 specs compartilham helpers locais** (raro):

```
package/billing/__test__/e2e/subscription/
├── subscription-lifecycle.e2e-spec.ts
├── subscription-billing.e2e-spec.ts
└── helpers.ts          # shared only within this group
```

Padrão: **sem subpasta** — uma spec = um arquivo em `__test__/e2e/<flow>.e2e-spec.ts`.

### Módulos baseados em subdomain (`content`, `analytics`)

Cada subdomain tem seu próprio `__test__/e2e/` na **raiz do subdomain** (boundary de CQRS):

```
package/{module}/{subdomain}/__test__/e2e/<flow>.e2e-spec.ts
```

**Exemplos:**
- `package/analytics/ingestion/__test__/e2e/ingestion.e2e-spec.ts`
- `package/analytics/aggregation/__test__/e2e/aggregation.e2e-spec.ts`
- `package/content/management/__test__/e2e/management-movie.e2e-spec.ts`
- `package/content/catalog/__test__/e2e/list-videos.e2e-spec.ts`

**Fluxos entre subdomains:** coloque o e2e no subdomain que é "dono" do fluxo (fluxos de leitura → `catalog/`, ciclo de vida → `management/`).

### Infraestrutura de teste compartilhada

| Propósito | Localização |
|-----------|-------------|
| Test factories (compartilhadas entre e2e) | `package/{module}/__test__/factory/{name}.test-factory.ts` |
| Helpers/setup compartilhados | `package/{module}/__test__/support/` (crie quando necessário) |

Packages com subdomain podem importar factories da raiz do package: `'../../../../__test__/factory/...'`.

## Detecção do Tipo de Package

Olhe a estrutura de pastas do package:
- **Módulo flat** — sem pastas de subdomain na raiz do package (`billing`, `identity`, `recommendations`). Um único `__test__/e2e/` na raiz do package.
- **Módulo baseado em subdomain** — pastas de subdomain nomeadas (`ingestion/`, `management/`, `catalog/`). Cada subdomain tem seu próprio `__test__/e2e/`.

## Template de Imports Obrigatórios

### E2e de módulo flat

Arquivo: `package/billing/__test__/e2e/<flow>.e2e-spec.ts`

```typescript
import { faker } from '@faker-js/faker';
import { HttpStatus, INestApplication } from '@nestjs/common';
import { TestingModule } from '@nestjs/testing';
import { createNestApp, Tables } from '@tlc/shared-lib/test';
import { ConfigModule, ConfigService } from '@tlc/shared-module/config';
import { randomUUID } from 'crypto';
import knex, { Knex } from 'knex';
import nock, { cleanAll } from 'nock';
import request from 'supertest';
import { billingConfigFactory, BillingModule } from '../../billing.module';
import { BillingConfig } from '../../config';
import { planFactory } from '../factory/plan.test-factory';
```

Profundidade dos imports a partir de `__test__/e2e/`:
- Módulo + config: `'../../'` (raiz do package)
- Factory: `'../factory/...'` (`__test__/factory/`)

### E2e de módulo baseado em subdomain

Arquivo: `package/analytics/ingestion/__test__/e2e/<flow>.e2e-spec.ts`

```typescript
import { faker } from '@faker-js/faker';
import { HttpStatus, INestApplication } from '@nestjs/common';
import { TestingModule } from '@nestjs/testing';
import { createNestApp, Tables } from '@tlc/shared-lib/test';
import { ConfigModule, ConfigService } from '@tlc/shared-module/config';
import { randomUUID } from 'crypto';
import knex, { Knex } from 'knex';
import nock, { cleanAll } from 'nock';
import request from 'supertest';
import { analyticsConfigFactory, AnalyticsModule } from '../../../analytics.module';
import { AnalyticsConfig } from '../../../config';
import { featureFactory } from '../../../../__test__/factory/feature.test-factory';
```

Profundidade dos imports a partir de `{subdomain}/__test__/e2e/`:
- Módulo + config: `'../../../'` (raiz do package)
- Enum/types compartilhados: `'../../../shared/...'`
- Factory da raiz do package: `'../../../../__test__/factory/...'`

Substitua os nomes de módulo/config/factory pelos equivalentes do seu package.

## Pattern de Setup (beforeAll)

O setup padrão cria a app NestJS, o config service e o client de banco:

```typescript
describe('Subscription e2e test', () => {
  let app: INestApplication;
  let module: TestingModule;
  let testDbClient: Knex;

  beforeAll(async () => {
    const nestTestSetup = await createNestApp([
      ConfigModule.forRoot({
        load: [billingConfigFactory],
      }),
      BillingModule,
    ]);
    app = nestTestSetup.app;
    module = nestTestSetup.module;
    const configService = module.get<ConfigService<BillingConfig>>(ConfigService);
    testDbClient = knex({
      client: 'pg',
      connection: `${configService.get('billing.database.url')}`,
      searchPath: ['public'],
    });
  });
```

**Pontos chave:**
- Use `createNestApp` de `@tlc/shared-lib/test`
- Inclua `ConfigModule.forRoot` com a config factory do seu módulo
- Inclua seu módulo no array de imports
- Extraia `app` e `module` do objeto retornado
- Crie o client Knex usando a URL de banco do módulo vinda da config
- Substitua `billing.database.url` pelo caminho de config do seu módulo

Referência: `package/billing/__test__/e2e/subscription.e2e-spec.ts`

## Pattern dos Lifecycle Hooks

### beforeEach

Defina fake timers para testes dependentes de data:

```typescript
beforeEach(async () => {
  jest.useFakeTimers({ advanceTimers: true }).setSystemTime(new Date('2023-01-01'));
});
```

### afterEach

Limpe as tabelas do banco na ordem inversa de dependência (tabelas filhas primeiro) e limpe os mocks do nock:

```typescript
afterEach(async () => {
  await testDbClient(Tables.Subscription).del();
  await testDbClient(Tables.Plan).del();
  cleanAll(); // Clear all nock mocks
});
```

**Importante:**
- Delete as tabelas na ordem inversa de dependência para evitar violações de foreign key
- Sempre chame `cleanAll()` para limpar os mocks HTTP do nock após cada teste

### afterAll

Feche a app e o módulo:

```typescript
afterAll(async () => {
  await app.close();
  module.close();
});
```

## Pattern de Mock de JWT

Para endpoints autenticados, mocke a verificação do JWT:

```typescript
const fakeUserId = faker.string.uuid();
jest.mock('jsonwebtoken', () => ({
  verify: jest.fn((_token: string, _secret: string, _options: unknown, callback: (err: Error | null, decoded: unknown) => void) => {
    callback(null, { sub: fakeUserId });
  }),
}));
```

Depois use `Bearer fake-token` nos headers de Authorization:

```typescript
.set('Authorization', `Bearer fake-token`)
```

Referência: `package/billing/__test__/e2e/subscription.e2e-spec.ts`

## Mock de HTTP com Nock

Para mockar requisições HTTP externas (chamadas de API a serviços de terceiros ou chamadas entre módulos), use o nock:

### Import

```typescript
import nock, { cleanAll } from 'nock';
```

### Pattern Básico

```typescript
nock('https://api.example.com', {
  encodedQueryParams: true,
  reqheaders: {
    Authorization: (): boolean => true,
  },
})
  .defaultReplyHeaders({ 'access-control-allow-origin': '*' })
  .get('/endpoint')
  .query({ param: 'value' })
  .reply(200, { data: 'response' });
```

### Exemplo: Mockar API Externa

```typescript
it('calls external API', async () => {
  nock('https://api.themoviedb.org/3', {
    encodedQueryParams: true,
    reqheaders: { Authorization: (): boolean => true },
  })
    .defaultReplyHeaders({ 'access-control-allow-origin': '*' })
    .get('/search/keyword')
    .query({ query: 'Test Video', page: '1' })
    .reply(200, { results: [{ id: '1' }] });

  const res = await request(app.getHttpServer())
    .post('/admin/movie')
    .send({ title: 'Test Video' });

  expect(res.status).toBe(HttpStatus.CREATED);
});
```

### Exemplo: Mockar Chamadas Entre Módulos

```typescript
it('calls another module via HTTP', async () => {
  nock('https://localhost:3000', {
    encodedQueryParams: true,
    reqheaders: { Authorization: (): boolean => true },
  })
    .defaultReplyHeaders({ 'access-control-allow-origin': '*' })
    .get(`/subscription/user/${userId}/active`)
    .reply(200, { isActive: true });

  const res = await request(app.getHttpServer())
    .get('/profile')
    .set('Authorization', `Bearer fake-token`);

  expect(res.status).toBe(HttpStatus.OK);
});
```

Sempre chame `cleanAll()` no `afterEach`.

Referência: `package/content/management/__test__/e2e/management-movie.e2e-spec.ts`

## Pattern de Estrutura do Teste

Siga o pattern Arrange-Act-Assert:

```typescript
it('creates a resource', async () => {
  // Arrange
  const plan = planFactory.build({ name: 'Basic', amount: 10.0 });
  await testDbClient(Tables.Plan).insert(plan);

  // Act
  const res = await request(app.getHttpServer())
    .post('/subscription')
    .set('Authorization', `Bearer fake-token`)
    .send({ planId: plan.id });

  // Assert
  expect(res.status).toBe(HttpStatus.CREATED);
  expect(res.body).toMatchObject({
    id: expect.any(String),
    planId: plan.id,
  });
});
```

**Boas práticas:**
- Use blocos `describe` para organizar testes relacionados
- Use test factories (`planFactory.build()`) em vez de dados hardcoded
- Insira dados de teste usando `testDbClient(Tables.TableName).insert()`
- Faça as requisições usando `request(app.getHttpServer())`
- Faça assert dos status codes usando o enum `HttpStatus`
- Use `expect.any(String)` para campos gerados como IDs e timestamps

## Pattern de Limpeza do Banco

Sempre limpe as tabelas após cada teste usando o enum `Tables`:

```typescript
afterEach(async () => {
  await testDbClient(Tables.InvoiceLineItem).del();
  await testDbClient(Tables.Invoice).del();
  await testDbClient(Tables.Subscription).del();
  await testDbClient(Tables.Plan).del();
});
```

**Regras:**
- Use o enum `Tables` de `@tlc/shared-lib/test`
- Delete tabelas filhas antes das tabelas pai
- Delete todas as tabelas usadas no teste
- Use `await` em todas as deleções

Referência: `package/billing/__test__/e2e/invoice.e2e-spec.ts`

## Exemplos

### Exemplo 1: Teste CRUD Simples

Referência: `package/billing/__test__/e2e/subscription.e2e-spec.ts`

```typescript
it('creates a subscription', async () => {
  const plan = planFactory.build({
    name: 'Basic',
    description: 'Basic monthly plan',
    currency: 'USD',
    amount: 10.0,
    interval: PlanInterval.Month,
    trialPeriod: 7,
  });
  await testDbClient(Tables.Plan).insert(plan);

  const res = await request(app.getHttpServer())
    .post('/subscription')
    .set('Authorization', `Bearer fake-token`)
    .send({ planId: plan.id });

  expect(res.status).toBe(HttpStatus.CREATED);
  expect(res.body).toEqual({
    id: expect.any(String),
    createdAt: expect.any(String),
    updatedAt: expect.any(String),
    deletedAt: null,
    endDate: null,
    userId: fakeUserId,
    planId: plan.id,
    status: SubscriptionStatus.Active,
    startDate: expect.any(String),
    autoRenew: true,
  });
});
```

### Exemplo 2: Teste com Relacionamentos

Referência: `package/billing/__test__/e2e/invoice.e2e-spec.ts`

```typescript
it('should get invoice by id', async () => {
  const plan = planFactory.build({ name: 'Basic', amount: 10.0, interval: PlanInterval.Month });
  await testDbClient(Tables.Plan).insert(plan);

  const subscription = subscriptionFactory.build({
    userId: fakeUserId,
    planId: plan.id,
    status: SubscriptionStatus.Active,
  });
  await testDbClient(Tables.Subscription).insert(subscription);

  const invoice = invoiceFactory.build({
    userId: fakeUserId,
    subscriptionId: subscription.id,
    status: InvoiceStatus.Open,
    subtotal: 10.0,
    totalTax: 1.0,
    total: 11.0,
    amountDue: 11.0,
  });
  await testDbClient(Tables.Invoice).insert(invoice);

  const res = await request(app.getHttpServer())
    .get(`/invoices/${invoice.id}`)
    .set('Authorization', `Bearer fake-token`);

  expect(res.status).toBe(HttpStatus.OK);
  expect(res.body).toMatchObject({
    id: invoice.id,
    status: InvoiceStatus.Open,
    total: 11.0,
  });
});
```

### Exemplo 3: Teste de Caso de Erro

Referência: `package/billing/__test__/e2e/subscription.e2e-spec.ts`

```typescript
it('throws error if the plan does not exist', async () => {
  const res = await request(app.getHttpServer())
    .post('/subscription')
    .set('Authorization', `Bearer fake-token`)
    .send({ planId: randomUUID() });

  expect(res.status).toBe(HttpStatus.NOT_FOUND);
});
```

## Patterns Comuns

### Usando Test Factories

```typescript
const plan = planFactory.build({
  name: 'Basic',
  amount: 10.0,
  interval: PlanInterval.Month,
});
```

As factories fornecem defaults, mas permitem overrides. Use `build()` para criar instâncias.

### Definindo Fake Timers

```typescript
beforeEach(async () => {
  jest.useFakeTimers({ advanceTimers: true }).setSystemTime(new Date('2023-01-01'));
});
```

### Testando com Autenticação

```typescript
.set('Authorization', `Bearer fake-token`)
```

O mock de JWT retorna `fakeUserId` do setup do mock.

### Testando Respostas de Erro

```typescript
expect(res.status).toBe(HttpStatus.NOT_FOUND);
expect(res.status).toBe(HttpStatus.BAD_REQUEST);
expect(res.status).toBe(HttpStatus.UNAUTHORIZED);
```

## Anti-Patterns a Evitar

**Não coloque arquivos `.spec.ts` unitários ao lado do código de produção na raiz do aggregate:**
- Use `<aggregate>/__test__/<file>.spec.ts`

**Não crie subpastas de feature para specs e2e únicas:**
- Use `__test__/e2e/<flow>.e2e-spec.ts` diretamente
- Subpasta só quando ≥2–3 specs relacionadas compartilham helpers locais

**Não use o sufixo `.spec.ts` para e2e:**
- Use `.e2e-spec.ts` para sinalizar o setup de integração com DB/Redis/HTTP

**Não esqueça a limpeza do banco:**
- Sempre limpe as tabelas no `afterEach`
- Delete na ordem inversa de dependência

**Não use timestamps reais:**
- Use fake timers no `beforeEach`

**Não hardcode IDs:**
- Use `faker.string.uuid()` ou `randomUUID()`
- Use test factories para criar dados

**Não esqueça de fechar os recursos:**
- Sempre feche a app e o módulo no `afterAll`

**Não esqueça de limpar os mocks do nock:**
- Sempre chame `cleanAll()` no `afterEach`

**Não pule o ConfigModule:**
- Sempre inclua `ConfigModule.forRoot` com a sua config factory

**Não misture dados de teste:**
- Cada teste deve ser independente; limpe todos os dados após cada teste

## Recursos Adicionais

- Specs de layout de teste: `.specs/features/test-flat-restructure/` (layout e2e), `.specs/features/unit-tests-aggregate-folder-migration/` (unit specs em `<aggregate>/__test__/`)
- Estrutura de módulos: `.agents/skills/architecture/SKILL.md`
- Utilitário de setup de teste: `package/shared/lib/test/test-e2e.setup.ts`
- Enum Tables: `package/shared/lib/test/enum/tables.enum.ts`
- Exemplo de test factory: `package/billing/__test__/factory/plan.test-factory.ts`
