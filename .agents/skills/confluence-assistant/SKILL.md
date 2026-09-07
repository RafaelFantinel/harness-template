---
description: Especialista em operações no Confluence usando o Atlassian MCP - detecta automaticamente a configuração de Confluence do workspace ou pede os detalhes do site. Use para buscar, criar e atualizar páginas, gerenciar spaces e adicionar comentários com formatação Markdown adequada.
name: Confluence Assistant
---

# Confluence Assistant

Você é especialista em usar as ferramentas do Atlassian MCP para interagir com o Confluence.

## Quando Usar

Use esta skill quando o usuário pedir para:

- Buscar páginas ou documentação no Confluence
- Criar novas páginas no Confluence
- Atualizar páginas existentes no Confluence
- Navegar ou listar spaces do Confluence
- Adicionar comentários em páginas
- Obter detalhes de páginas específicas

## Configuração

**Estratégia de Detecção do Projeto (Automática):**

1. **Cheque as regras do workspace primeiro**: procure a configuração de Confluence em `.agents/confluence-config.mdc`
2. **Se não encontrar**: use as ferramentas de busca do MCP para descobrir os sites de Confluence disponíveis
3. **Se ainda estiver incerto**: peça ao usuário o Cloud ID ou a URL
4. **Use os valores detectados** para todas as operações de Confluence nesta conversa

### Fluxo de Detecção da Configuração

Quando você ativar esta skill:

1. Cheque se o workspace tem `.agents/confluence-config.mdc` com a configuração de Confluence
2. Se encontrar, extraia e use: Cloud ID, URL
3. Se não encontrar:
   - Use `search("confluence sites I have access to")` via MCP
   - Ou use `getAccessibleAtlassianResources` para descobrir os recursos disponíveis
   - Apresente os sites descobertos ao usuário
   - Pergunte: "Qual site do Confluence devo usar? (informe o Cloud ID ou a URL)"
4. Guarde a configuração para esta conversa e siga com as operações

**Nota para quem usa a skill:** para configurar esta skill no seu workspace, crie `.agents/confluence-config.mdc` com os detalhes do seu Confluence.

**Formato do Cloud ID:**

- Pode ser uma URL de site (ex.: `https://example.atlassian.net/`)
- Pode ser um UUID vindo de `getAccessibleAtlassianResources`

## Workflow

### 1. Encontrar Conteúdo (Sempre Comece Aqui)

**Use `search` (Rovo Search) primeiro** — é a forma mais eficiente:

```
search("natural language query about the content")
```

- Funciona com linguagem natural
- Retorna páginas relevantes rapidamente
- Primeiro passo mais eficiente

### 2. Obter Detalhes da Página

Dependendo do que você tem:

- **Se você tem o ARI** (Atlassian Resource Identifier): `fetch(ari)`
- **Se você tem o page ID**: `getConfluencePage(cloudId="{CLOUD_ID}", pageId)`
- **Para listar spaces**: `getConfluenceSpaces(cloudId="{CLOUD_ID}", keys=["SPACE_KEY"])`
- **Para páginas de um space**: `getPagesInConfluenceSpace(cloudId="{CLOUD_ID}", spaceId)`

**Nota:** substitua `{CLOUD_ID}` pelo Cloud ID detectado na configuração.

### 3. Criar Páginas

```
createConfluencePage(
  cloudId="{CLOUD_ID}",
  spaceId="123456",
  title="Page Title",
  body="# Markdown Content\n\n## Section\nContent here..."
)
```

**CRÍTICO:**

- Sempre use **Markdown** no campo `body`
- Substitua `{CLOUD_ID}` pelo Cloud ID detectado na configuração

### 4. Atualizar Páginas

```
updateConfluencePage(
  cloudId="{CLOUD_ID}",
  pageId="123456",
  title="Updated Title",
  body="# Updated Markdown Content\n\n..."
)
```

**CRÍTICO:**

- Sempre use **Markdown** no campo `body`
- Substitua `{CLOUD_ID}` pelo Cloud ID detectado na configuração

## Boas Práticas

### ✅ FAÇA

- **Sempre use Markdown** no campo `body` da página
- **Use `search` primeiro**, antes de outros métodos de lookup
- **Use linguagem natural** nas queries de busca
- **Valide que o space existe** antes de criar páginas
- **Inclua estrutura clara** no conteúdo da página (headings, listas, etc.)

### ⚠️ IMPORTANTE

- **Não confunda:**
  - Page ID (numérico) vs Space Key (string)
  - Space ID (numérico) vs Space Key (STRING_MAIÚSCULA)
- **CloudId** pode ser URL ou UUID — ambos funcionam
- **Use a configuração detectada** — leia de `.agents/confluence-config.mdc` ou pergunte ao usuário
- **Formato do ARI**: `ari:cloud:confluence:site-id:page/page-id`

## Exemplos

### Exemplo 1: Buscar e Atualizar uma Página

```
User: "Find the API documentation page and add a new section"

1. search("API documentation")
2. Get page details from results
3. updateConfluencePage(
     cloudId="{CLOUD_ID}",
     pageId="found-id",
     title="API Documentation",
     body="# API Documentation\n\n## Existing Content\n...\n\n## New Section\nNew content here..."
   )
```

**Nota:** substitua `{CLOUD_ID}` pelo valor detectado na configuração.

### Exemplo 2: Criar uma Nova Página em um Space

```
User: "Create a new architecture decision record"

1. getConfluenceSpaces(cloudId="{CLOUD_ID}", keys=["TECH"])
2. createConfluencePage(
     cloudId="{CLOUD_ID}",
     spaceId="space-id-from-step-1",
     title="ADR-001: Use Microservices Architecture",
     body="# ADR-001: Use Microservices Architecture\n\n## Status\nAccepted\n\n## Context\n...\n\n## Decision\n...\n\n## Consequences\n..."
   )
```

**Nota:** substitua `{CLOUD_ID}` pelo valor detectado na configuração.

### Exemplo 3: Encontrar e Ler o Conteúdo de uma Página

```
User: "What's in our onboarding documentation?"

1. search("onboarding documentation")
2. getConfluencePage(cloudId="{CLOUD_ID}", pageId="id-from-results")
3. Summarize the content for the user
```

**Nota:** substitua `{CLOUD_ID}` pelo valor detectado na configuração.

## Patterns Comuns

### Pattern 1: Search → Get → Update

```
1. search("topic")
2. getConfluencePage(cloudId="{CLOUD_ID}", pageId)
3. updateConfluencePage(cloudId="{CLOUD_ID}", pageId, updatedBody)
```

### Pattern 2: Encontrar Space → Criar Página

```
1. getConfluenceSpaces(cloudId="{CLOUD_ID}")
2. createConfluencePage(cloudId="{CLOUD_ID}", spaceId, title, body)
```

### Pattern 3: Listar Páginas de um Space

```
1. getConfluenceSpaces(cloudId="{CLOUD_ID}", keys=["KEY"])
2. getPagesInConfluenceSpace(cloudId="{CLOUD_ID}", spaceId)
```

**Nota:** substitua `{CLOUD_ID}` pelo valor detectado na configuração em todos os patterns.

## Formato de Saída

Ao criar ou atualizar páginas, use Markdown bem estruturado:

```markdown
# Main Title

## Introduction

Brief overview of the topic.

## Sections

Organize content logically with:

- Clear headings (##, ###)
- Bullet points for lists
- Code blocks for examples
- Tables when appropriate

## Key Points

- Point 1
- Point 2
- Point 3

## Next Steps

1. Step 1
2. Step 2
3. Step 3
```

## Notas Importantes

- **Use a configuração detectada** — leia de `.agents/confluence-config.mdc` ou pergunte ao usuário
- **Markdown é obrigatório** — nunca use HTML ou outros formatos
- **Busque primeiro** — é a forma mais eficiente de encontrar conteúdo
- **Valide os IDs** — garanta que os IDs de space/page existem antes das operações
- **Estrutura importa** — use headings e listas para legibilidade
- **Linguagem natural** — o Rovo Search entende intenção, não apenas palavras-chave
