---

name: git-commit
description: 'Execute commits Git com análise de mensagens Conventional Commits, staging inteligente e geração de mensagens. Use quando o usuário solicitar um commit, criar um commit Git ou mencionar "/commit". Suporta: (1) detecção automática de tipo e escopo com base nas alterações, (2) geração de mensagens Conventional Commits a partir do diff, (3) commit interativo com substituições opcionais de tipo/escopo/descrição, (4) staging inteligente de arquivos para agrupamento lógico.'

---

# Git Commit com Conventional Commits

## Visão Geral

Crie commits Git padronizados e semânticos usando a especificação **Conventional Commits**.

Analise o diff real das alterações para determinar o tipo, escopo e mensagem apropriados.

## Formato do Conventional Commit

```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
````

## Tipos de Commit

| Tipo       | Finalidade                                                 |
| ---------- | ---------------------------------------------------------- |
| `feat`     | Nova funcionalidade                                        |
| `fix`      | Correção de bug                                            |
| `docs`     | Alterações somente na documentação                         |
| `style`    | Formatação/estilo, sem alteração de lógica                 |
| `refactor` | Refatoração de código, sem nova funcionalidade ou correção |
| `perf`     | Melhoria de performance                                    |
| `test`     | Adição ou atualização de testes                            |
| `build`    | Alterações no sistema de build/dependências                |
| `ci`       | Alterações de CI/configuração                              |
| `chore`    | Manutenção/tarefas diversas                                |
| `revert`   | Reversão de commit                                         |

## Breaking Changes

Breaking changes podem ser indicadas utilizando `!` após o tipo ou escopo:

```text
feat!: remove endpoint deprecated
```

Ou utilizando o footer `BREAKING CHANGE`:

```text
feat: permite que uma configuração estenda outras configurações

BREAKING CHANGE: comportamento da chave `extends` foi alterado
```

## Coautoria Obrigatória

**TODO commit DEVE possuir um coautor de IA: Devin ou Claude.**

O commit deve conter um footer `Co-authored-by` válido.

Exemplo utilizando Devin:

```text
feat: adiciona autenticação

Co-authored-by: Devin <devin-ai-integration[bot]@users.noreply.github.com>
```

Exemplo utilizando Claude:

```text
feat: adiciona autenticação

Co-authored-by: Claude <noreply@anthropic.com>
```

### Regras

* **NUNCA** criar um commit sem `Co-authored-by`.
* O coautor deve ser **Devin ou Claude**.
* O footer deve ser incluído na mensagem final do commit.
* Se a ferramenta/agente responsável pelo commit for Devin, utilizar Devin como coautor.
* Se a ferramenta/agente responsável pelo commit for Claude, utilizar Claude como coautor.
* Não remover um `Co-authored-by` existente de Devin ou Claude ao modificar a mensagem do commit.
* Se houver dúvida sobre qual identidade utilizar, **não criar o commit até determinar se Devin ou Claude é o agente responsável**.
* O commit deve ser validado antes da execução para garantir que o footer de coautoria esteja presente.

## Fluxo de Trabalho

### 1. Analisar o Diff

```bash
# Se houver arquivos staged, analisar o diff staged
git diff --staged

# Se nada estiver staged, analisar o diff da working tree
git diff

# Verificar também o status
git status --porcelain
```

Determine quais alterações pertencem ao mesmo contexto lógico.

### 2. Fazer Stage dos Arquivos

Se nada estiver staged ou se for necessário reorganizar as alterações:

```bash
# Adicionar arquivos específicos
git add path/to/file1 path/to/file2

# Adicionar por padrão
git add *.test.*
git add src/components/*

# Stage interativo
git add -p
```

Agrupe apenas alterações relacionadas no mesmo commit.

**Nunca faça commit de secrets**, incluindo:

* `.env`
* `credentials.json`
* Chaves privadas
* Tokens
* Senhas
* Credenciais de API
* Outros arquivos contendo informações sensíveis

### 3. Gerar a Mensagem do Commit

Analise o diff para determinar:

* **Tipo**: qual tipo de alteração foi realizada?
* **Escopo**: qual área ou módulo foi afetado?
* **Descrição**: resumo em uma linha do que foi alterado.

A descrição deve:

* Estar no presente.
* Utilizar modo imperativo.
* Ter menos de 72 caracteres sempre que possível.
* Descrever a alteração de forma objetiva.

Exemplo:

```text
feat(auth): adiciona autenticação por token
```

### 4. Adicionar o Coautor

Antes de executar o commit, adicione obrigatoriamente o footer de coautoria.

Exemplo:

```bash
git commit -m "$(cat <<'EOF'
feat(auth): adiciona autenticação por token

Co-authored-by: Devin <devin-ai-integration[bot]@users.noreply.github.com>
EOF
)"
```

Ou:

```bash
git commit -m "$(cat <<'EOF'
feat(auth): adiciona autenticação por token

Co-authored-by: Claude <noreply@anthropic.com>
EOF
)"
```

### 5. Executar o Commit

Para commits simples:

```bash
git commit -m "$(cat <<'EOF'
<type>[scope]: <description>

Co-authored-by: Devin <devin-ai-integration[bot]@users.noreply.github.com>
EOF
)"
```

Para commits com body e footer:

```bash
git commit -m "$(cat <<'EOF'
<type>[scope]: <description>

<body>

Co-authored-by: Devin <devin-ai-integration[bot]@users.noreply.github.com>

<optional footer>
EOF
)"
```

**O commit nunca deve ser executado se o `Co-authored-by` obrigatório não estiver presente.**

## Boas Práticas

* Um commit deve representar uma única alteração lógica.
* Utilize o presente: `add` em vez de `added`.
* Utilize o modo imperativo: `fix bug` em vez de `fixes bug`.
* Referencie issues quando aplicável:

  * `Closes #123`
  * `Refs #456`
* Mantenha a descrição com menos de 72 caracteres.
* Evite misturar alterações não relacionadas no mesmo commit.
* Sempre revise o diff antes de criar o commit.
* Sempre inclua `Co-authored-by` de Devin ou Claude.

## Protocolo de Segurança do Git

* **NUNCA** altere `git config`.
* **NUNCA** execute comandos destrutivos (`--force`, `git reset --hard`, etc.) sem solicitação explícita.
* **NUNCA** utilize `--no-verify` para ignorar hooks, a menos que o usuário solicite explicitamente.
* **NUNCA** faça force push para `main`/`master`.
* **NUNCA** faça commit diretamente em `main` ou `master`.
* **NUNCA** faça commit diretamente na branch `dev`, quando ela for uma branch compartilhada de desenvolvimento.
* Alterações devem ser feitas em uma branch de trabalho apropriada e integradas por Pull Request/Merge Request quando aplicável.
* Se o commit falhar por causa de hooks, corrija o problema e crie um **novo commit**. Não utilize `--amend` automaticamente.
* Antes de criar o commit, confirme que apenas os arquivos relacionados à alteração estão staged.
* Antes de criar o commit, confirme que o `Co-authored-by` obrigatório de Devin ou Claude está presente.


