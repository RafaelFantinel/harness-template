# Skill Confluence Assistant

Esta skill fornece operações especializadas no Confluence usando as ferramentas do Atlassian MCP. Ela detecta automaticamente a configuração de Confluence do workspace a partir do contexto ou pede os detalhes do site.

## Requisitos de Configuração

A skill exige que os seguintes valores de configuração estejam disponíveis no contexto do seu workspace:

### Valores Obrigatórios

- **Cloud ID** — o Cloud ID da sua Atlassian (formato UUID ou URL do site)
- **URL** — a URL do seu site Atlassian (ex.: `https://example.atlassian.net/`)

### Formato do Cloud ID

O Cloud ID pode ser informado em dois formatos:

- **URL do site**: `https://your-site.atlassian.net/`
- **UUID**: um UUID obtido de `getAccessibleAtlassianResources`

Ambos os formatos são aceitos e funcionam com todas as operações do Confluence.

## Onde Configurar

A skill detecta a configuração a partir de múltiplas fontes, o que a torna compatível com IDEs e setups diferentes:

### Opção 1: Config do projeto (`.agents/confluence-config.mdc`)

Se você usa o Cursor, crie um arquivo de rule:

```yaml
---
alwaysApply: false
---

# Confluence Configuration

This workspace uses the following Confluence configuration:

- **Cloud ID:** your-cloud-id-uuid-or-url
- **URL:** https://your-site.atlassian.net/

The Cloud ID can be:
- A site URL (e.g., `https://your-site.atlassian.net/`)
- A UUID from `getAccessibleAtlassianResources`
```

### Opção 2: AGENTS.md

Se você usa outra IDE ou prefere o AGENTS.md, adicione a configuração lá:

```markdown
# Confluence Configuration

- **Cloud ID:** your-cloud-id-uuid-or-url
- **URL:** https://your-site.atlassian.net/

The Cloud ID can be:

- A site URL (e.g., `https://your-site.atlassian.net/`)
- A UUID from `getAccessibleAtlassianResources`
```

### Opção 3: Outras Fontes de Contexto

A skill também detecta a configuração em:

- Arquivos de documentação do workspace
- Arquivos README do projeto
- Qualquer arquivo markdown no seu workspace que contenha a configuração do Confluence

### Opção 4: Pergunta Interativa

Se nenhuma configuração for encontrada, a skill vai:

1. Usar as ferramentas do MCP para descobrir os sites de Confluence disponíveis
2. Usar `getAccessibleAtlassianResources` para listar os recursos disponíveis
3. Pedir que você selecione seu site de Confluence
4. Guardar a seleção para a conversa atual

## Fluxo de Detecção da Configuração

Quando a skill é ativada, ela segue esta ordem de detecção:

1. **Checar o contexto do workspace** — procura a configuração de Confluence em:

   - `.agents/confluence-config.mdc` (Cursor)
   - `AGENTS.md` (qualquer IDE)
   - Outros arquivos de documentação do workspace

2. **Se não encontrar** — usa a busca do MCP para descobrir os sites de Confluence disponíveis:

   - `search("confluence sites I have access to")`
   - `getAccessibleAtlassianResources`

3. **Se ainda estiver incerto** — pede ao usuário o Cloud ID ou a URL

4. **Usa os valores detectados** — aplica a configuração em todas as operações

## Exemplo de Configuração

Um exemplo completo de configuração:

```markdown
# Confluence Configuration

- **Cloud ID:** d58e860b-469d-4463-8f46-684934a5a851
- **URL:** https://techleadsclub.atlassian.net/

The Cloud ID can be:

- A site URL (e.g., `https://techleadsclub.atlassian.net/`)
- A UUID from `getAccessibleAtlassianResources`
```

## Uso

Uma vez configurada, a skill usa automaticamente as configurações do seu site para:

- Buscar páginas e documentação
- Criar novas páginas
- Atualizar páginas existentes
- Listar e navegar spaces
- Adicionar comentários em páginas
- Obter detalhes de páginas

Todas as operações usam o seu Cloud ID configurado automaticamente.

## Troubleshooting

**A skill não encontra a configuração:**

- Garanta que seu arquivo de configuração está na raiz do workspace ou no diretório `.agents/`
- Cheque se o arquivo contém os valores obrigatórios (Cloud ID, URL)
- Verifique se o formato bate com os exemplos acima

**Site errado sendo usado:**

- Cheque no seu arquivo de configuração se o Cloud ID ou a URL estão corretos
- A skill usa a primeira configuração válida que encontrar
- Você pode sobrescrever especificando o site na sua solicitação

**Configuração não detectada:**

- A skill vai perguntar interativamente se nenhuma configuração for encontrada
- Você também pode informar os detalhes do site direto na sua solicitação: "Buscar páginas em https://example.atlassian.net/"

**Problemas com o formato do Cloud ID:**

- Os formatos URL e UUID são ambos aceitos
- Se usar URL, garanta que ela inclui o protocolo (`https://`)
- Se usar UUID, garanta que é o formato correto vindo de `getAccessibleAtlassianResources`

## Compatibilidade

Esta skill funciona com:

- Cursor IDE (via `.agents/`)
- Qualquer IDE que suporte AGENTS.md
- Qualquer workspace com arquivos de configuração acessíveis
- Modo interativo (pergunta a configuração)
