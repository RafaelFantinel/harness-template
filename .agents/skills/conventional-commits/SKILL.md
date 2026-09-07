---

name: conventional-commits

description: Gera mensagens de commit semânticas seguindo a especificação Conventional Commits, com tipos, escopos, breaking changes e rodapés apropriados. Use quando os usuários solicitarem "escrever mensagem de commit", "conventional commit", "commit semântico" ou "formatar commit".

---

# Conventional Commits

Escreva mensagens de commit padronizadas e semânticas que permitam versionamento automatizado e geração de changelogs.

## Fluxo Principal

1. **Analisar alterações**: Revise os arquivos staged e as modificações
2. **Determinar o tipo**: Selecione o tipo apropriado de commit (`feat`, `fix`, etc.)
3. **Identificar o escopo**: Componente/módulo afetado, quando aplicável
4. **Escrever a descrição**: Resumo conciso usando o modo imperativo
5. **Adicionar o corpo**: Explicação detalhada opcional
6. **Incluir o rodapé**: Breaking changes e referências a issues


## Co-Author Obrigatório

**Todo commit DEVE incluir Devin como coautor.**

A mensagem de commit deve obrigatoriamente conter o seguinte rodapé:

```text
Co-authored-by: Devin <devin@cognition.ai>
```

### Regras

* **NENHUM commit deve ser criado sem o `Co-authored-by` do Devin**
* O coautor deve ser adicionado mesmo quando a alteração for pequena
* O coautor deve ser adicionado para todos os tipos de commit: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert`, etc.
* O `Co-authored-by` deve aparecer nos **footers** da mensagem
* Não substituir, remover ou omitir o coautor
* Outros coautores podem ser adicionados quando necessário, mas o Devin continua sendo obrigatório

## Formato da Mensagem de Commit

```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## Tipos de Commit

| Tipo       | Descrição                                               | Semver | Exemplo                              |
| ---------- | ------------------------------------------------------- | ------ | ------------------------------------ |
| `feat`     | Nova funcionalidade                                     | MINOR  | `feat: add user authentication`      |
| `fix`      | Correção de bug                                         | PATCH  | `fix: resolve login redirect loop`   |
| `docs`     | Apenas documentação                                     | -      | `docs: update API reference`         |
| `style`    | Formatação, espaços em branco                           | -      | `style: fix indentation in utils`    |
| `refactor` | Alteração de código sem nova funcionalidade ou correção | -      | `refactor: extract validation logic` |
| `perf`     | Melhoria de performance                                 | PATCH  | `perf: optimize database queries`    |
| `test`     | Adição/correção de testes                               | -      | `test: add unit tests for auth`      |
| `build`    | Sistema de build, dependências                          | -      | `build: upgrade to Node 20`          |
| `ci`       | Configuração de CI/CD                                   | -      | `ci: add GitHub Actions workflow`    |
| `chore`    | Tarefas de manutenção                                   | -      | `chore: update .gitignore`           |
| `revert`   | Reverte um commit anterior                              | -      | `revert: undo feature flag change`   |

## Escopos

Os escopos indicam a área da base de código afetada:

```bash
# Escopos de componente/módulo

feat(auth): add OAuth2 support

fix(api): handle timeout errors

docs(readme): add installation steps

# Escopos baseados em arquivo

style(eslint): update linting rules

build(docker): optimize image size

# Escopos por camada

refactor(service): extract user service

test(e2e): add checkout flow tests
```

## Breaking Changes

Marque alterações incompatíveis usando `!` ou o rodapé `BREAKING CHANGE`:

```bash
# Usando a notação !

feat(api)!: change response format to JSON\:API

# Usando o rodapé

feat(api): change response format

BREAKING CHANGE: Response now follows JSON\:API specification.

Clients must update their parsers.
```

## Exemplos de Mensagens de Commit

### Feature Simples

```bash
feat: add dark mode toggle
```

### Feature com Escopo

```bash
feat(ui): add dark mode toggle to settings page
```

### Correção de Bug com Referência a Issue

```bash
fix(auth): resolve session expiration race condition

The session refresh was racing with the expiration check,
causing intermittent logouts.

Fixes #234
```

### Breaking Change

```bash
feat(api)!: migrate to v2 response format

BREAKING CHANGE: All API responses now use camelCase keys
instead of snake_case. Update client parsers accordingly.

Migration guide: https://docs.example.com/v2-migration
```

### Múltiplos Rodapés

```bash
fix(payments): correct tax calculation for EU customers

Updated tax calculation to use customer's billing country
instead of shipping country for digital goods.

Fixes #456

Reviewed-by: Alice

Co-authored-by: Devin <devin@cognition.ai>
```

## Commit de Reversão

```bash
revert: feat(auth): add OAuth2 support

This reverts commit abc123def456.

Reason: OAuth provider has rate limiting issues in production.

Will re-implement with proper caching.
```

## Diretrizes para a Descrição

### Faça

* Use o modo imperativo: `"add"` em vez de `"added"` ou `"adds"`
* Mantenha a descrição abaixo de 72 caracteres
* Comece com letra minúscula
* Não use ponto final
* Seja específico e conciso

### Não Faça

* `"Fixed bug"` — muito vago
* `"Updated stuff"` — não é descritivo
* `"WIP"` — faça o commit quando estiver pronto
* `"misc changes"` — divida em commits separados

### Bons Exemplos

```bash
feat: add email verification flow

fix: prevent duplicate form submissions

refactor: extract payment processing to service

perf: cache user preferences in memory

docs: add API authentication examples
```

### Exemplos Ruins

```bash
# Muito vago

fix: fixed it

update: updates

# Tempo verbal incorreto

feat: added new feature

fix: fixes the bug

# Muito longo

feat: add a new feature that allows users to export their data in multiple formats including CSV, JSON, and XML
```

## Diretrizes para o Corpo

Quando incluir um corpo:

* As alterações precisam de contexto ou explicação
* Existe uma lógica complexa que não é autoexplicativa
* Breaking changes exigem informações de migração
* Existem várias alterações relacionadas no mesmo commit

```text
fix(cache): invalidate user cache on profile update

Previously, profile updates were not reflected until cache expiry.

This caused confusion when users updated their avatar and didn't
see the change immediately.

The fix adds cache invalidation after successful profile updates
and ensures CDN purge for static assets.
```

## Tokens de Rodapé

| Token             | Finalidade                    | Exemplo                          |
| ----------------- | ----------------------------- | ------------------------------   |
| `Fixes`           | Fecha uma issue               | `Fixes #123`                   |
| `Closes`          | Fecha uma issue               | `Closes #456`                  |
| `Refs`            | Referencia uma issue          | `Refs #789`                    |
| `BREAKING CHANGE` | Indica alteração incompatível | `BREAKING CHANGE: description`   |
| `Reviewed-by`     | Crédito ao revisor            | `Reviewed-by: Name`              |
| `Co-authored-by`  | Definir coautor               | `Co-authored-by: Devin <devin@cognition.ai>` |

## Integração com Ferramentas

### Configuração do Commitlint

```javascript
// commitlint.config.js

module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat', 'fix', 'docs', 'style', 'refactor',
        'perf', 'test', 'build', 'ci', 'chore', 'revert'
      ]
    ],
    'scope-case': [2, 'always', 'kebab-case'],
    'subject-case': [2, 'always', 'lower-case'],
    'subject-max-length': [2, 'always', 72],
    'body-max-line-length': [2, 'always', 100]
  }
};
```

### Hook de Pre-commit com Husky

```bash
# .husky/commit-msg

#!/bin/sh

. "$(dirname "$0")/_/husky.sh"

npx --no-install commitlint --edit "$1"
```

### Configuração do Package.json

```json
{
  "devDependencies": {
    "@commitlint/cli": "^18.0.0",
    "@commitlint/config-conventional": "^18.0.0",
    "husky": "^8.0.0"
  },
  "scripts": {
    "prepare": "husky install"
  }
}
```

## Integração com Semantic Release

Conventional Commits permite o versionamento automatizado:

```yaml
# .releaserc.yml

branches:
  - main

plugins:
  - "@semantic-release/commit-analyzer"
  - "@semantic-release/release-notes-generator"
  - "@semantic-release/changelog"
  - "@semantic-release/npm"
  - "@semantic-release/git"
```

## Gerador de Mensagens de Commit

Ao analisar alterações, gere uma mensagem de commit:

```bash
# 1. Verificar alterações staged

git diff --cached --name-only

# 2. Analisar o tipo de alteração

# - Arquivos novos = provavelmente feat

# - Arquivos de teste modificados = test

# - Documentação modificada = docs

# - Palavras-chave relacionadas a bugs = fix

# 3. Identificar o escopo a partir do caminho

# src/components/Button.tsx → components ou ui

# src/services/auth.ts → auth ou services

# 4. Gerar a mensagem

feat(ui): add loading state to Button component
```



## Boas Práticas

1. **Uma alteração lógica por commit**: Não misture features com correções
2. **Faça commits cedo e com frequência**: Mantenha commits pequenos e focados
3. **Escreva pensando nos revisores**: As mensagens devem explicar o porquê, não apenas o quê
4. **Referencie issues**: Vincule tickets/issues quando aplicável
5. **Use escopos de forma consistente**: Estabeleça convenções para o time
6. **Revise antes de fazer o commit**: Use `git diff --cached` para verificar as alterações

### Validação obrigatória

Toda mensagem de commit deve:

* [ ] Começar com um tipo válido (`feat`, `fix`, `docs`, etc.)
* [ ] Usar o modo imperativo na descrição
* [ ] Manter a descrição abaixo de 72 caracteres
* [ ] Incluir escopo quando aplicável
* [ ] Marcar breaking changes com `!` ou rodapé
* [ ] Referenciar issues relacionadas no rodapé
* [ ] Fornecer corpo para alterações complexas
* [ ] Seguir as convenções de escopo do time
* [ ] Incluir o coautor obrigatório (`Co-authored-by: Devin <devin@cognition.ai>`)