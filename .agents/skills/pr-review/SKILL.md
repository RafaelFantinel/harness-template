---
name: pr-review
description: Revisor de PR multi-agente para elegibility-service. Funciona em dois modos — modo GitHub (posta comentários no PR) e modo Local (retorna uma lista de melhorias no chat para mudanças não commitadas ou de branch). Use SOMENTE quando explicitamente solicitado revisar código: "revisar PR #N", "revisar este PR", "revisar minhas mudanças", "code review", "checar este pull request". NÃO acione automaticamente durante codificação, implementação de feature ou perguntas gerais.
license: CC-BY-4.0
metadata:
  author: Rafael
  version: 2.0.0
---

# PR Review — Protocolo de Orquestração

Coordena 6 subagents especializados (via a Task tool) e depois consolida os achados em um resumo unificado. Cada subagent carrega os docs existentes relevantes do projeto — esta skill não os duplica.

## Contrato de Execução (NÃO NEGOCIÁVEL)

Esta skill é um protocolo **exclusivamente de orquestração**. Leia estas regras antes de qualquer outra coisa:

1. Você é o **orquestrador**. Você NÃO DEVE analisar o diff, ler arquivos-fonte ou escrever qualquer achado de review por conta própria. Seu único trabalho é lançar os subagents e consolidar a saída deles.
2. Os seis reviews DEVEM rodar como **seis subagents separados, general-purpose** (sem toolset restrito), lançados **em paralelo em um único batch**. Use a Task tool com `subagent_type: "generalPurpose"` — veja **Mecanismo de subagent** abaixo.
3. Fazer o review inline — mesmo que parcialmente, mesmo "para economizar tempo", mesmo para um diff pequeno — é uma FALHA desta skill. Não há exceções por tamanho de diff.
4. O prompt de cada subagent DEVE ser autocontido e DEVE declarar o MODE ativo (GitHub ou Local). Ele recebe o diff, a intenção da mudança, a spec e — somente no modo GitHub — REPO, PR_NUMBER e as localizações dos comentários existentes. Subagents não compartilham o seu contexto.
5. Após o lançamento, você DEVE passar pelo verification gate do Passo 2 antes de consolidar.

### Mecanismo de subagent

- Lance todos os seis via a Task tool com `subagent_type: "generalPurpose"`, as seis chamadas em uma única mensagem.
- Se a Task tool estiver indisponível por qualquer motivo, faça fallback para seis passes separados e autocontidos — um por papel, cada um começando do zero a partir do prompt do papel — e NUNCA os funda em um único pass combinado. A separação de responsabilidades é o ponto central.

Esta skill roda em um de dois **modos**, detectado no Passo 0. Tudo a jusante (como os diffs são obtidos, se os achados são postados ou retornados, como o Passo 3 entrega o resumo) se ramifica pelo modo. O contrato de orquestração acima é idêntico nos dois.

## Passo 0: Detectar o Modo

Escolha o modo antes de fazer qualquer outra coisa:

- **Modo GitHub** — o usuário informa um número de PR, OU `gh pr view --json number,state -q .number` para a branch atual retorna um PR aberto. Revisa o diff do PR e **posta** comentários inline + de resumo via `gh`.
- **Modo Local** — nenhum PR identificado (nenhum número fornecido e o `gh pr view` não encontra nada), OU o usuário pede para revisar "minhas mudanças" / trabalho local / não commitado, OU o `gh` está ausente/não autenticado (`gh auth status` falha). Revisa o diff local e **retorna uma lista de melhorias no chat** — NÃO DEVE chamar `gh` para postar nada.

Regras de decisão:

- Se o usuário pedir explicitamente review local/não commitado → modo Local, mesmo que exista um PR aberto.
- Se um PR for explicitamente nomeado → modo GitHub.
- Se exatamente um dos dois estiver disponível, use-o sem perguntar.
- Se ambos forem plausíveis e o usuário não foi específico, pergunte uma vez qual ele quer.

Depois execute o Passo 1 correspondente abaixo.

## Passo 1 (modo GitHub): Inicializar

1. Obtenha o número do PR do contexto ou pergunte ao usuário.
2. Identifique o repo: `gh repo view --json nameWithOwner -q .nameWithOwner`
3. Obtenha o diff: `gh pr diff {PR_NUMBER}`
4. Carregue os comentários inline existentes: `gh api repos/{REPO}/pulls/{PR_NUMBER}/comments` — monte um conjunto de pares `{path, line}` para evitar repostagem.
5. Leia a intenção do PR: `gh pr view {PR_NUMBER} --json title,body,headRefName,headRefOid`. Guarde o `headRefOid` (o SHA do commit de head) — o Passo 3 o usa para montar links de arquivo.
6. Verifique se há um ticket Jira vinculado no nome da branch (padrão `[A-Z]+-[0-9]+`).

## Passo 1 (modo Local): Inicializar

1. Não há número de PR, REPO nem comentários existentes — pule todas as chamadas `gh`.
2. Escolha a fonte do diff:
   - Se `git status --porcelain` não estiver vazio → revise a working tree: `git diff HEAD` (mudanças rastreadas). Anote também os arquivos untracked de `git status --porcelain` para que um subagent possa sinalizar testes obviamente ausentes, mas não exija o conteúdo deles.
   - Caso contrário → revise a branch contra sua base: `BASE=$(git merge-base HEAD origin/main 2>/dev/null || git merge-base HEAD main)` e então `git diff $BASE...HEAD`. Substitua `main` por `master` se `main` não existir.
3. Derive a intenção da mudança do `git log` da branch (subjects desde a base) e do pedido do usuário — não há corpo de PR.
4. Para a spec, passe o nome da branch e os subjects de commit recentes para o Subagent 2, para que ele possa fazer fuzzy-match de arquivos de spec commitados em `.specs/`, `docs/`, etc. Pule a trilha do Jira a menos que o usuário forneça um ticket ID.

## Passo 2: Lançar os Subagents em Paralelo (OBRIGATÓRIO)

Lance **exatamente seis subagents em um batch paralelo** (veja **Mecanismo de subagent**) — Security, Requirements, E2E Test Coverage, Architecture, Regression, Performance. Passe o **MODE** (GitHub ou Local), o diff, a intenção da mudança e a spec no prompt de cada subagent. No modo GitHub, passe também REPO, PR_NUMBER e as localizações dos comentários existentes. Cada prompt deve dizer ao subagent como entregar os achados naquele modo (veja **Roteamento de Saída**).

**Use o agent type genérico para todo subagent.** Lance todos os seis subagents (e o subagent de consolidação do Passo 3) com o agent type padrão `generalPurpose`. **Não** substitua por um agent customizado do projeto ou do usuário (ex.: um agent-persona de "security" ou "architecture") em nenhum papel, mesmo quando um deles pareça topicamente próximo do foco do subagent. Os critérios de review são definidos por esta skill e pelos docs de referência do projeto que cada subagent carrega — eles pertencem ao projeto, não ao julgamento próprio de qualquer agent individual. Um agent customizado carrega suas próprias prioridades e prompt, o que sobrescreveria silenciosamente as regras abaixo. Cada subagent deve seguir apenas as instruções da sua seção neste arquivo, mais os docs que aquela seção mandar carregar.

**Nomeie todo subagent.** Dê a cada subagent um `name` estável para que ele possa ser re-endereçado entre turnos via o parâmetro `resume` da Task tool. Use `review-{type}`, onde `{type}` corresponde ao marker do subagent (`<!-- review:{type} -->`):

| Subagent | name |
| --- | --- |
| 1. Security | `review-security` |
| 2. Requirements & Definition of Done | `review-requirements` |
| 3. E2E Test Coverage | `review-e2e` |
| 4. Architecture & Coding Patterns | `review-architecture` |
| 5. Regression & Hallucination Detection | `review-regression` |
| 6. Performance | `review-performance` |
| Consolidação (Passo 3) | `review-consolidation` |

**Verification gate — não prossiga até que tudo seja verdadeiro:**

- [ ] Lancei 6 subagents, não menos.
- [ ] Todos os 6 rodaram como subagents independentes em um batch paralelo (ou, se não existir mecanismo de subagent, seis passes separados e do zero), nunca um único pass fundido.
- [ ] Escrevi zero achados de review por conta própria.

Se qualquer caixa estiver desmarcada, pare e relance corretamente antes do Passo 3. Depois que os seis terminarem, execute o Passo 3.

---

## Labels de Severidade (todos os subagents usam estas)

- 🚨 Critical — bugs ou erros de lógica que causarão falhas
- 🔒 Security — vulnerabilidades de segurança ou exposição de dados
- ⚡ Performance — preocupações significativas de performance
- ⚠️ Warning — code smells ou problemas de manutenibilidade
- 💡 Suggestion — melhorias opcionais

---

## Regras Universais (todo subagent deve seguir)

As regras 1–3 e 8 governam a **postagem de comentários e valem somente no modo GitHub**. As regras 4–7 valem sempre, nos dois modos.

1. **Allowlist de comentários:** só poste comentários inline em linhas do diff que começam com `+` (excluindo `+++`).
2. **Pule duplicatas:** se `{path, line}` dentro de ±3 linhas já tiver um comentário, pule.
3. **Marque como resolvido:** responda `[RESOLVED] This appears resolved by the recent changes.` em comentários existentes cujo problema foi corrigido.
4. **Guarda contra falso positivo:** só reporte achados com ≥80% de confiança. Pule quando estiver incerto.
5. **Destaque positivo:** inclua pelo menos um aspecto bem feito da mudança antes de listar os problemas.
6. **Tom:** específico, acionável, colegial. Explique POR QUE algo é um problema.
7. **Nunca** aprove, peça mudanças (request-changes) ou modifique arquivos. (Modo GitHub: use apenas `--comment`.)
8. **Marker:** comece o corpo de todo comentário inline com `<!-- review:{type} -->` (invisível na visualização renderizada, usado pelo subagent de consolidação).

---

## Roteamento de Saída (por modo)

Os checks por agent abaixo são idênticos nos dois modos — só a **entrega** difere. Cada subagent deve seguir o roteamento do modo ativo:

- **Modo GitHub:** poste os achados como comentários inline usando o **formato de comentário** de cada agent (markers incluídos). Aplique as regras 1–3 e 8. O Subagent 2 posta seu resumo via `gh pr comment`.
- **Modo Local:** NÃO chame `gh`. Retorne os achados ao orquestrador como uma lista markdown, uma entrada por achado, usando as mesmas labels de severidade e os mesmos corpos do formato de comentário, **mas sem o marker HTML e sem etapa de postagem em linha**. Cada entrada deve citar `path:line` para que o orquestrador possa agrupá-las. O Subagent 2 retorna seu resumo de requisitos como texto.

Em cada seção por agent abaixo, leia "poste um comentário inline" como "poste (GitHub) ou retorne como item de lista (Local)" conforme este roteamento.

---

## Checklist de Conformidade Estrutural

Para as regras modulares e estruturais completas, veja `.agents/skills/architecture/SKILL.md` e `references/principles.md` (P11–P17). Aplique também checks genéricos: testes, comentários, nomenclatura, segurança (`docs/integration-patterns.md`).

- [ ] **Profundidade** — arquivos de negócio em depth ≤2 (flat) ou ≤3 (baseado em subdomain: `content`, `analytics`)
- [ ] **Unit specs** — `package/<module>/<aggregate>/__test__/*.spec.ts` (flat) ou `package/<module>/<subdomain>/<aggregate>/__test__/*.spec.ts` (subdomain); não adjacentes aos fontes de produção na raiz do aggregate
- [ ] **E2e specs** — `__test__/e2e/*.e2e-spec.ts` na raiz do package (flat) ou sob cada subdomain (veja a skill `create-e2e-tests`)
- [ ] **Nada de pasta com um único arquivo** — um arquivo usa sufixo (ex.: `wallet.constants.ts`), não uma pasta `constants/` (P14); `__test__/` dentro de um aggregate é a exceção sancionada para unit specs
- [ ] **Sufixos, não pastas técnicas** — sem `core/`, `http/`, `persistence/` dentro de aggregates; use `.service.ts`, `.entity.ts`, `.controller.ts`
- [ ] **Sem README dentro do aggregate** — documentação fica fora das pastas de negócio
- [ ] **`index.ts` exporta somente o facade** — module + facade (+ tipos públicos); nenhum service ou repository interno
- [ ] **Conformidade P11–P17** — novos aggregates seguem o layout flat-by-aggregate; produção co-localizada por aggregate; unit tests no `__test__/` do aggregate

---

## Subagent 1: Security

**Marker:** `<!-- review:security -->`

Carregue `docs/integration-patterns.md` e foque na seção **Security**. Revise o diff do PR em busca de violações desses padrões de segurança: secrets hardcoded, auth guards ausentes, PII em logs, validação de assinatura de webhook ausente, CORS permissivo demais, clients exportados através de fronteiras de módulo, campos sensíveis em DTOs de resposta e concatenação de query crua.

**Segundo pass:** releia o diff completo de cima a baixo. Liste todo arquivo ou hunk sobre o qual você não comentou. Para cada arquivo não coberto, pergunte: "Este arquivo viola alguma regra de segurança do meu escopo?" Só pule um arquivo quando puder declarar explicitamente por que ele está limpo.

**Formato de comentário:**
```
<!-- review:security -->
🔒 Security — [Título curto]
[Qual é o problema e por que ele importa]
**Recommendation:** [Correção específica]
```

No modo Local, retorne isto como item de lista em vez de postar (veja Roteamento de Saída).

---

## Subagent 2: Requirements & Definition of Done

**Marker:** `<!-- review:requirements -->`
**Entrega:** somente um resumo, sem comentários inline — postado via `gh pr comment` (modo GitHub) ou retornado como texto (modo Local).

Use uma abordagem de duas trilhas para encontrar requisitos. Rode as duas trilhas em paralelo; use a que produzir conteúdo.

### Trilha A — Ticket Jira

1. Extraia o ticket ID do nome da branch (padrão `[A-Z]+-[0-9]+`). No modo Local, pule esta trilha a menos que o usuário forneça um ticket ID.
2. Se encontrado, busque: `curl -su "$JIRA_USER:$JIRA_API_TOKEN" "$JIRA_BASE_URL/rest/api/2/issue/$TICKET_ID?fields=summary,description"`
3. Faça o parse de acceptance criteria, user stories e itens de checklist da DoD.

### Trilha B — Arquivos de Spec no Repo

1. Procure qualquer referência a arquivos de spec ou de tasks. Modo GitHub: varra o título e o corpo do PR. Modo Local: use o nome da branch e os subjects de commit recentes passados pelo orquestrador, no lugar do corpo do PR. Padrões comuns:
   - Caminhos explícitos: `.specs/`, `docs/`, `*.spec.md`, `*-tasks.md`, `*-spec.md`
   - Links markdown: `[...](path/to/file.md)`
   - Menções inline: `spec: path/to/file`, `tasks: path/to/file`
2. Procure também um diretório `.specs/` na raiz do repo — se existir, verifique se algum arquivo dentro dele corresponde ao nome da branch, ao ticket ID ou ao nome da feature (fuzzy match no stem do arquivo).
3. Para cada arquivo candidato encontrado, leia com `cat {path}` e extraia: acceptance criteria, itens de checklist de tasks e quaisquer goals ou non-goals declarados.

### Lógica de Resolução

| Trilhas com conteúdo | Ação |
|---|---|
| Ambas A e B | Mescle os requisitos das duas fontes; anote a fonte de cada item |
| Só A | Use os requisitos do Jira |
| Só B | Use os requisitos do arquivo de spec |
| Nenhuma | Reporte: "⚠️ No Jira ticket or spec file found — requirements verification skipped." e pare |

Compare os requisitos mesclados com o diff e então entregue o resumo: o modo GitHub posta com `gh pr comment {PR_NUMBER} --body '...'`; o modo Local retorna como texto ao orquestrador.

**Segundo pass:** depois de redigir o resumo, releia a lista completa de requisitos item a item e pergunte: "Avaliei este critério contra o diff?" Para qualquer item ainda não avaliado, encontre a seção relevante do diff e marque explicitamente ✅, ❌ ou 🔲.

**Formato do resumo:**
```markdown
<!-- review:requirements -->
## 📋 Requirements Review

**Sources:** {ex.: "Jira: FAKE-123" | "Spec: .specs/recommendations-v2.md" | "Both"}

### ✅ Implemented
### ❌ Missing or Incomplete
### 🔲 Definition of Done
- [x] coberto  - [ ] não coberto
### 💬 Notes
```

---

## Subagent 3: E2E Test Coverage

**Marker:** `<!-- review:e2e -->`

Carregue `.agents/skills/create-e2e-tests/SKILL.md`. Use esses padrões como referência do que são testes corretos. Revise o diff do PR quanto a: e2e tests ausentes em novos endpoints (🚨 Critical), problemas de qualidade de teste (localização de arquivo errada, cleanups ausentes, sem JWT mock, nock ausente) e anti-patterns (IDs hardcoded, status codes crus, sem factory, sem asserts no response body).

**Segundo pass:** releia o diff completo de cima a baixo. Liste todo endpoint novo ou modificado, método de controller e queue consumer sobre os quais você não comentou. Para cada handler não coberto, pergunte: "Existe um e2e test correspondente cobrindo o happy path e ao menos um caso de erro?" Só pule um handler quando puder declarar explicitamente por que a cobertura e2e já existe ou não se aplica.

**Formato de comentário:**
```
<!-- review:e2e -->
[🚨/⚠️/💡] — [Título curto]
[Descrição da lacuna ou do anti-pattern]
**Recommendation:** [Pattern a seguir conforme a skill create-e2e-tests]
```

No modo Local, retorne isto como item de lista em vez de postar (veja Roteamento de Saída).

---

## Subagent 4: Architecture & Coding Patterns

**Marker:** `<!-- review:architecture -->`

### Fase 0 — Carregue todos os documentos de referência

Carregue todo documento listado abaixo antes de tocar no diff. Não pule nenhum.

1. `docs/coding-patterns.md`
2. `docs/integration-patterns.md`
3. `.agents/skills/architecture/SKILL.md`
4. `.agents/skills/architecture/references/principles.md`
5. `.agents/skills/architecture/references/verification.md`
6. `.agents/skills/architecture/references/subdomain-persistence.md`
7. `.agents/skills/architecture/references/module-scaffolding.md`
8. `.agents/skills/security-pr-checklist-skill/SKILL.md`

Depois varra o diff quanto à estrutura de diretórios: se algum caminho alterado contiver um diretório `shared/` ao lado de múltiplas pastas irmãs `{subdomain}/`, o PR toca um **módulo baseado em subdomain** — anote isso para a Fase 1.

### Fase 1 — Extraia a lista de regras dos documentos carregados

Não use uma lista hardcoded. Depois de carregar todos os documentos da Fase 0, varra cada um e extraia toda regra explícita para um único checklist numerado. Use estes alvos de extração por documento:

- **`verification.md`** — extraia todo item das seções **New Feature Checklist** e **Pre-Commit Checklist** (procure marcadores de checkbox `□`)
- **`module-scaffolding.md`** — extraia todo item das seções **Post-Generation Checklist** e **Facade Rules**
- **`coding-patterns.md`** — extraia toda regra marcada com `✅` ou `❌` de cada section header
- **`principles.md`** — extraia toda regra marcada com `✅` ou `❌` do bloco **Rules** de cada princípio
- **`subdomain-persistence.md`** — extraia todo item da seção **Verification Checklist for Subdomain Persistence** — inclua somente se o PR tocar um módulo baseado em subdomain

Numere a lista combinada sequencialmente a partir de 1. Essa lista numerada é a sua matriz de avaliação para a Fase 2. Não adicione regras que não estejam nos documentos e não omita nenhuma que você encontrar.

### Fase 2 — Avalie a matriz

Percorra o diff **um arquivo por vez**. Para cada arquivo alterado:

- Para cada regra da lista da Fase 1, decida: **PASS** / **VIOLATION** / **N/A**
- N/A só é válido quando a regra é estruturalmente inaplicável ao tipo de arquivo (ex.: um arquivo de DTO não pode violar regras de `@Transactional`; um arquivo de migration não pode violar leanness de controller)
- Para cada VIOLATION: poste um comentário inline na linha `+` exata do diff que é a evidência. Inclua o número da regra e o documento de origem.

**Segundo pass:** após completar a matriz para todos os arquivos, releia o diff completo de cima a baixo. Liste todo arquivo ou hunk que você não avaliou. Para qualquer arquivo não coberto, rode a matriz novamente. Só pule um arquivo quando puder declarar explicitamente quais regras são N/A e por quê.

**Formato de comentário:**
```
<!-- review:architecture -->
[🚨/⚠️/💡] — [Título curto]
Rule: [Número da regra + qual doc, ex.: "Rule 8 — verification.md New Feature Checklist"]
[O que no diff viola isso — cite a linha ofensora]
**Recommendation:** [Correção exata, snippet de código se < 6 linhas]
```

No modo Local, retorne isto como item de lista em vez de postar (veja Roteamento de Saída).

---

## Subagent 5: Regression & Hallucination Detection

**Marker:** `<!-- review:regression -->`

Revise o diff do PR em busca de mudanças de código não relacionadas ao propósito declarado do PR, ou que apresentem sinais de artefatos gerados por IA. Procure por: código deletado sem relação com a mudança (🚨 Critical), phantom imports referenciando símbolos inexistentes (🚨 Critical), chamadas de método com assinatura errada (🚨 Critical), `TODO` deixado em código de produção, type assertions escondendo erros do compilador, lógica duplicada que já existe no módulo, tratamento de erro ou validação enfraquecidos, erros de queue job engolidos silenciosamente, asserts de teste enfraquecidos e dead code que nunca é chamado.

**Segundo pass:** releia o diff completo de cima a baixo. Liste todo arquivo ou hunk sobre o qual você não comentou. Para cada arquivo não coberto, pergunte: "Este arquivo contém alguma deleção não relacionada, phantom import, lógica duplicada ou assert enfraquecido?" Só pule um arquivo quando puder declarar explicitamente por que nenhuma dessas categorias se aplica.

**Formato de comentário:**
```
<!-- review:regression -->
[🚨/⚠️/💡] — [Título curto]
Type: [unrelated-deletion | phantom-import | hallucination | duplicate | regression | dead-code]
[Descrição específica com evidência citada do diff]
**Recommendation:** [Correção exata]
```

No modo Local, retorne isto como item de lista em vez de postar (veja Roteamento de Saída).

---

## Subagent 6: Performance

**Marker:** `<!-- review:performance -->`

Carregue `docs/coding-patterns.md` (seções Repository Pattern e Transaction Management). Sinalize somente problemas **claramente visíveis no diff** — sem especulação. Procure por: padrões de N+1 query (lookup de repository dentro de loop), `find()` sem limite e sem paginação, `relations` ausente causando N+1 por lazy-load, `await` sequencial para operações independentes que poderiam usar `Promise.all` e múltiplas chamadas de `repository.save()` sem `@Transactional`.

**Segundo pass:** releia o diff completo de cima a baixo. Liste todo método de service, chamada de repository e loop sobre os quais você não comentou. Para cada bloco não coberto, pergunte: "Isto contém um problema de performance claramente visível?" Só pule um bloco quando puder declarar explicitamente por que nenhum dos padrões acima se aplica.

**Formato de comentário:**
```
<!-- review:performance -->
⚡ Performance — [Título curto]
[Descrição com impacto estimado, ex.: "O(N) queries per request"]
**Recommendation:** [Correção com esboço curto de código se < 6 linhas]
```

No modo Local, retorne isto como item de lista em vez de postar (veja Roteamento de Saída).

---

## Passo 3: Consolidação

Depois que os 6 subagents terminarem, consolide. O **modo GitHub** dispara mais um subagent via Task tool para coletar os comentários postados; o **modo Local** consolida os achados que os seis subagents retornaram a você (sem subagent extra, sem `gh`).

**Coletar achados:**

- Modo GitHub:
  1. `gh api repos/{REPO}/pulls/{PR_NUMBER}/comments` — busque todos os comentários inline.
  2. Filtre os que começam com `<!-- review: -->` e faça o parse do type a partir do marker.
  3. Busque os comentários de nível de PR para o resumo `<!-- review:requirements -->`.
- Modo Local:
  1. Junte as listas markdown retornadas por cada um dos seis subagents mais o resumo de requisitos do Subagent 2. Não há comentários postados para buscar.

**Depois, nos dois modos:**

4. Agrupe por severidade: 🔒 Security → 🚨 Critical → ⚡ Performance → ⚠️ Warning → 💡 Suggestion.
5. Deduplique achados no mesmo `{path, line}` (±3 linhas) — cite ambos os agents na entrada.
6. Colete um destaque positivo por agent.
7. **Detecção de lacunas:** obtenha a lista completa de arquivos alterados — `gh pr diff {PR_NUMBER} --name-only` (GitHub) ou `git diff --name-only` contra a mesma base usada no Passo 1 (Local). Cruze com todos os paths dos achados. Para qualquer arquivo com zero achados, adicione-o a uma seção `### 🔍 Files With No Inline Comments`. Omita um arquivo apenas se ele for de config/lock (ex.: `*.json`, `*.yaml`, `*.lock`) ou um arquivo puro de declaração de tipos sem lógica.
8. **Linke cada achado ao arquivo, não à discussão (modo GitHub).** Renderize o `path:line` de todo achado como um link markdown para o arquivo no commit de head do PR, para que clicar abra o arquivo naquela linha: `https://github.com/{REPO}/blob/{headRefOid}/{path}#L{line}` (use o `headRefOid` do Passo 1 e o número de linha do lado novo do diff). Não linke para a âncora do comentário inline / discussão. O modo Local não tem `headRefOid` — mantenha citações `path:line` simples.

**Entregar o resumo:**

9. Modo GitHub: poste com `gh pr review {PR_NUMBER} --comment --body '...'`. Modo Local: imprima o resumo direto no chat como sua resposta final — não chame `gh`.

**Formato do resumo:**
```markdown
## 🤖 Cursor AI Review Summary

| | |
|---|---|
| **Mode** | {GitHub (PR #N) \| Local (working tree \| branch vs base)} |
| **Subagents invoked** | {N} of 6 (Security · Requirements (Jira + Spec) · E2E Coverage · Architecture · Regression · Performance) |
| **Skills loaded** | `.agents/skills/pr-review/SKILL.md`, `.agents/skills/create-e2e-tests/SKILL.md` |
| **Docs loaded** | `docs/coding-patterns.md`, `docs/integration-patterns.md`, `.agents/skills/architecture/SKILL.md` |
| **Findings** | {N} across {M} files |

---

### 🔒 Security ({N})
- [[`path/file.ts:L42`]](https://github.com/{REPO}/blob/{headRefOid}/path/file.ts#L42) Título do achado

### 🚨 Critical ({N})
### ⚡ Performance ({N})
### ⚠️ Warnings ({N})
### 💡 Suggestions ({N})

---
### 🔍 Files With No Inline Comments
- `path/to/file.ts` — nenhum achado de nenhum subagent (verifique manualmente ou rode um review direcionado)

_(Omita esta seção se todos os arquivos de lógica receberam ao menos um comentário.)_

---
### ✅ Highlights
- [Um destaque positivo por agent]

---
> Modo GitHub: veja os comentários inline para detalhes e recomendações. Modo Local: cada achado acima inclui seu `path:line` e a recomendação inline.
```

Se não houver achados em nenhum agent: reporte `✅ No issues found across all review dimensions.` mas ainda inclua a tabela de metadados. (Modo GitHub posta; modo Local imprime no chat.)
