---
name: spec-driven-eval
description: Pontua o quão completamente uma implementação cumpre um PRD/spec, caso a caso, e produz uma nota final única. Invoque somente quando nomeada explicitamente (ex.: "rodar spec-driven-eval"); não acione automaticamente. Use para benchmark de implementações spec-driven, avaliar acceptance criteria, avaliar se uma feature foi "100% implementada", comparar múltiplas implementações do mesmo PRD, ou auditar implementação + cobertura de testes (unit e e2e) contra requisitos de produto.
disable-model-invocation: true
---

# Avaliação de Implementação Spec-Driven

Avalie um esforço de spec-driven-development (SDD) contra um PRD **caso a caso** (acceptance criterion por acceptance criterion), pontuando **implementação** e **testes** separadamente, e consolide em uma nota final única e comparável. Projetada para benchmark: o mesmo PRD avaliado em frameworks de SDD diferentes — e o mesmo esforço avaliado duas vezes — deve produzir números comparáveis e reprodutíveis.

### Dois sujeitos, duas perguntas

Esta eval responde duas perguntas independentes e mantém seus veredictos separados, porque elas falham de forma independente:

1. **Quão bom é o framework em respeitar e extrair requisitos?** — Ele honrou o PRD e ficou dentro dos limites (*respeito*: lado de implementação do `Final` + Scope `S`), e trouxe à tona os requisitos implícitos que o PRD apenas sugeria, sem ruído (*extração*: Elicitation `E`)?
2. **Quão bom é o harness em garantir que todos foram implementados?** — A suíte de testes prova que todo requisito sancionado foi de fato construído (`T` + Engineering Gates `G`)?

> **A ligação que torna "tudo implementado" bem definido:** o harness é responsabilizado pelo **conjunto completo de requisitos sancionados = `acceptance criteria do PRD ∪ E-additions válidas`**. Um requisito válido que o framework extraiu mas o harness nunca testa é uma falha do *harness* (`T`), não do framework (`E` mantém o crédito de extração). A extração define o alvo de verificação.

A pontuação é **baseada em checklist**: todo critério é decomposto em checks binários atômicos (MET / UNMET), cada um respaldado por evidência `file:line`. A decomposição binária é a escolha de design que torna a nota reprodutível — escalas graduadas/Likert (`isso é 3 ou 4?`) são a fonte dominante de discordância entre avaliadores; checks binários elevam a concordância entre avaliadores a aproximadamente o nível humano. O crédito parcial é **derivado** da fração de checks atendidos, nunca julgado numa escala contínua.

## Quando usar

- "Avalie/pontue este PRD caso a caso e dê uma nota final"
- "Esta story/feature foi implementada 100%?"
- Benchmark de múltiplas implementações spec-driven do mesmo PRD
- Auditoria de implementação **e** cobertura de testes (unit + e2e) contra acceptance criteria

## Entradas necessárias

1. **O PRD** (intenção de produto como ground-truth) — os acceptance criteria das user stories são a unidade de avaliação.
2. **A implementação** (código de produção).
3. **Os testes** (unit + e2e).
4. **Os artefatos derivados do SDD** — `spec.md` e `tasks.md` (ACs refinados, IDs de requisito, requisitos derivados). Necessários para os eixos de Elicitation `E` e Scope `S` (eles avaliam *esses* artefatos contra o PRD). Quando ausentes, `Final`/`T` ainda rodam, mas reporte `E`/`S` como `n/a — sem spec derivada`. O **PRD continua sendo a fonte de verdade** do que conta como "esperado"; a spec derivada é o que é avaliado quanto a respeito e extração.

**Delimitando o diff.** Antes de pontuar, use o git para identificar quais arquivos mudaram nesta implementação. Essa **superfície de diff** é o escopo primário de busca para toda evidência `file:line` nos passos 4 e 8:

```bash
git diff <base>..<head> --name-only   # branch or PR
git diff --name-only HEAD             # uncommitted changes
git status --short                    # include untracked new files
```

Registre a superfície de diff no relatório. Evidência fora dela ainda é válida (ex.: um arquivo pré-existente foi modificado), mas anote quando um check depender de arquivos fora do diff — isso pode indicar que a base escolhida estava errada.

---

## Início rápido

Novo em rodar isso de ponta a ponta? Veja [quickstart.md](quickstart.md) para o fluxo de 4 sessões de chat (congelar baseline → planejar → implementar → avaliar) com prompts prontos para colar. Leia quando precisar do passo a passo operacional; o restante deste arquivo é a metodologia de pontuação.

---

## Regras centrais (leia primeiro)

Estas cinco regras governam toda pontuação. Existem para tornar a nota auditável e reprodutível.

1. **Evidência ou zero.** Todo check MET DEVE citar evidência como `file:line` (ou `file:startLine-endLine`). Sem evidência localizada ⇒ o check é UNMET. Nunca conceda crédito a partir de suposições ou do PRD reafirmando a intenção.
2. **Buscar-antes-de-zerar (anti falso-negativo).** Antes de marcar um check como UNMET por "não encontrado", registre a busca efetivamente realizada — começando pelos arquivos da superfície de diff do passo de delimitação, depois os termos de grep/glob tentados e quaisquer arquivos/diretórios adicionais inspecionados. Um check pontuado UNMET deve confirmar que o comportamento está ausente da superfície de diff, não apenas de um grep improvisado sobre o repo inteiro. UNMET significa *buscado e genuinamente ausente*, não *não olhei*. Se nenhuma busca for mostrada, o check está **ainda não pontuado**, não UNMET.
3. **Leia o caminho de ponta a ponta — inclusive o formato dos dados.** Um check é MET somente se você rastreou o caminho real do código/teste, não porque o nome de um símbolo bate. Para um artefato emitido/retornado/persistido, **inspecione o próprio objeto de payload construído**, não apenas o call site — um `emit(...)`/`return ...` presente não prova que o campo nomeado está no payload (veja a regra de Conjunção). Um teste que *exercita* um comportamento mas não o *asserta* **não** atende a um check de verificação.
4. **Juiz ≠ autor em benchmarks.** Ao avaliar para comparar implementações, o modelo avaliador deve ser diferente do modelo que escreveu o código; juízes LLM super-recompensam a própria saída (viés de auto-preferência). Se forem o mesmo, sinalize em *Premissas* e trate os checks limítrofes como UNMET.
5. **O avaliador é read-only sobre o sujeito.** Nunca modifique o código sob avaliação — nem para corrigir um gate falhando, nem para "ajudar", nem para erros triviais de tipo. Um gate vermelho continua vermelho: registre `✗`, aplique o `Adjusted Final` e coloque a correção necessária na lista de fixes do relatório. Modificar o sujeito durante a avaliação contamina o benchmark (a nota deixa de medir a saída do framework) e invalida a superfície de diff. Se uma correção foi aplicada por acidente, reverta e pontue o estado original.

---

## Regras de reprodutibilidade

A nota deve ser estável: o **mesmo PRD + mesmo commit** deve render o **mesmo `Final`** entre execuções e avaliadores. Obedeça a todas:

1. **Congele a lista de ACs E seu checklist (a maior fonte de drift).** A enumeração de ACs e a granularidade dos checks são decididas **uma vez** e fixadas, porque `I`, `T` e o denominador do `Story_score` dependem de ambas:
   - Se existir um `spec.md` com IDs de requisito estáveis, a lista de ACs e os IDs estão congelados — use-os verbatim; não redivida nem funda.
   - Derive o checklist binário de cada AC uma vez e persista em `<spec-folder>/evaluations/_ac-baseline.md` (IDs + texto verbatim do AC + os I-checks e T-checks). **Toda implementação daquele PRD é pontuada contra o checklist idêntico.**
   - Um AC = uma afirmação testável de intenção; um check = uma proposição atômica e observável de sim/não. Não colapse comportamentos distintos em um check, nem divida um comportamento em vários.
2. **Prioridade vem do PRD, nunca inferida silenciosamente.** Use os rótulos explícitos P0/P1/P2 do PRD. Se uma story não tiver rótulo, marque sua prioridade como `ASSUMED` no relatório e liste em *Premissas*; nunca deixe uma prioridade inferida mover silenciosamente os pesos `3/2/0`.
3. **Arredonde uma vez, no fim.** Carregue precisão total pela aritmética; arredonde somente o `AC_score`, `Story_score` e `Final` reportados para **2 casas decimais**. A atribuição de banda usa o `Final` arredondado.
4. **Compute, não calcule.** A consolidação (`Σw`, cada `Story_score`, `Final`, `Adjusted Final`) DEVE ser computada executando um script (ex.: `node -e` / `python3 -c`) que recebe as frações `I`/`T` por AC e a tabela de pesos de prioridade como entrada, com a saída do script colada no relatório. Nunca faça a aritmética de cabeça ou à mão — um denominador (`Σw`) somado manualmente é um modo de falha conhecido que desloca silenciosamente a banda da nota. `Σw` deve ser derivado dentro do script a partir da mesma tabela de prioridade mostrada no relatório, não digitado como literal.
5. **Ensemble de auto-consistência (k = 3).** Avalie o checklist **três vezes de forma independente** com temperatura baixa/zero e tome a **maioria MET/UNMET por check** antes de computar qualquer número. A votação por maioria sobre k=3 remove a maior parte das oscilações estocásticas a baixo custo. Se as três passadas discordarem em um check, esse check é limítrofe — mantenha o veredicto majoritário e anote; discordância persistente significa que a redação do check é ambígua (afie-a no baseline, veja Calibração).

---

## Modelo de pontuação

### Unidade de pontuação: o acceptance criterion (AC), decomposto em checks binários

Para cada AC dentro do escopo, o checklist congelado guarda dois conjuntos de checks atômicos, cada um pontuado **MET (1) / UNMET (0)**:

**Checks de implementação (`I`-checks)** — um por comportamento observável distinto que o AC afirma. Um check é MET somente se o código de produção realiza aquele comportamento, com evidência `file:line`. Inclua apenas cláusulas comportamentalmente observáveis (um verbo que o AC declara: cria / retorna / rejeita / persiste / concede / assume-por-padrão). Polimento não funcional (redação, logging) **não** é um check — anote separadamente para que nunca mova a nota.

Ao parsear um AC em checks (feito uma vez, no momento de congelar o baseline), aplique as duas regras abaixo para que os verbos não sejam a única coisa capturada:

**Regra de conjunção / campos de payload.** Quando um AC enumera múltiplos itens — unidos por *"e"*, *","*, *"com [campo]"*, *"incluindo"* — *especialmente no sujeito ou no payload de um artefato emitido/retornado/persistido*, trate **cada campo ou entidade nomeada como seu próprio I-check**. Não pare em confirmar que o método pai é chamado: para cada campo nomeado, **abra o objeto de dados real construído em `file:line` e verifique que o campo está presente nele** (pontue contra o formato do payload, não contra o call site).
*Exemplo:* "emitir um trigger associado ao usuário **e** à data de fim do trial" → dois I-checks: (1) o payload do trigger carrega `userId`, (2) o payload do trigger carrega `trialEndsAt`. Confirmar que `emit(...)` é alcançado **não** satisfaz o check (2) se o objeto de payload omite o campo.

**Regra de disjunção / escolhido-pelo-produto.** Quando um AC apresenta alternativas ("A ou B", "pausar ou cancelar", "imediatamente ou no fim do período"), decida a leitura **uma vez e congele-a no baseline** (a leitura em si não pode variar entre execuções):
- *Caminhos independentes* — ambos são comportamentos separadamente alcançáveis (ex.: "cancelar imediatamente **ou** no fim do período"): **um I-check por caminho**, cada um MET/UNMET de forma independente.
- *Controlado pelo produto / configurável* — o AC enquadra a escolha como dirigida pelo produto (ex.: "aplicar o comportamento **escolhido pelo produto**: pausar ou cancelar"): **dois I-checks** — (1) o comportamento recomendado/padrão está implementado, e (2) a alternativa é *alcançável sem mudança de código* (chave de config / flag / env). Hard-coding de uma opção sem chave para a outra ⇒ check (2) UNMET. Uma feature diferente que por acaso alcança a outra opção (ex.: cancelamento iniciado pelo usuário) **não** satisfaz (2) — deve ser o *mesmo* ponto de decisão controlado pelo produto.
- Se *"escolhido pelo produto"* for ele próprio ambíguo entre *configurável em runtime* e *escolhido em design-time*, resolva pela redação do PRD/spec e registre a leitura resolvida no baseline; só exija a alcançabilidade do caminho não padrão quando o AC enquadrar a escolha como controlada em runtime/pelo produto.

**Regra de wiring / ingress (efeitos colaterais entregues de forma assíncrona).** Quando um efeito observável é entregue de forma assíncrona por um evento de entrada (ex.: webhook do Stripe → handler → status persistido), adicione um **I-check dedicado de wiring** *além do* I-check de comportamento: o endpoint de entrada de fato **recebe, verifica e despacha o evento de entrada — por tipo — para o handler.** Isso é distinto do teste `T-outcome` de que o handler produz o estado correto: um handler totalmente testado atrás de um endpoint morto, não registrado ou mal roteado ainda falha o check de wiring. Sem ele, um handler correto porém inalcançável ganharia crédito total (o ponto cego que a neutralidade de ponto de entrada do `T-outcome` abre — permitir que o teste contorne o HTTP exige que *outra coisa* prove que a rota existe). Pontue o I-check de wiring contra o controller/rota que mapeia o tipo de evento para o handler (verificação de assinatura/autenticidade + despacho), não contra os detalhes internos do handler. Congele um I-check de wiring em todo AC cujo efeito observável chega por um evento assíncrono de entrada.

```
I = (# I-checks MET) / (# I-checks)
```

**Checks de teste (`T`-checks)** — checks de verificação nos níveis exigidos. Os níveis exigidos são fixados por política, não julgados por AC:

- lógica pura / de negócio ⇒ **unit obrigatório**
- efeito colateral observável de HTTP / contrato / persistência ⇒ **e2e / integração obrigatório**
- um AC com ambos ⇒ **ambos obrigatórios**

**`T-outcome` — efeitos de persistência / assíncronos (baseado em resultado, neutro quanto ao ponto de entrada).** Quando o AC afirma um *estado resultante* — redigido como "resulta em / persistido / status exibido / reflete a mudança / atualiza o status" — o check de verificação recai sobre o **estado resultante real**, não sobre uma chamada. Rotule esses checks como **`T-outcome`** (distinto de um `T-e2e` de resposta síncrona simples). A política tem quatro partes:

- **Asserte o resultado real.** MET exige assertar o estado realmente persistido (uma linha real no banco) ou o payload realmente retornado, contra infra real. Um teste que apenas exercita o caminho sem assertar o estado resultante é UNMET (Regra central 3).
- **O ponto de entrada é escolha da implementação (neutro quanto ao ponto de entrada).** Dirija o componente real por **qualquer** ponto de entrada e asserte o estado resultante: o endpoint HTTP, OU invocando o handler / consumidor de fila / método de service diretamente contra o banco real. Um design assíncrono (ack rápido e processa depois) **não** precisa ser testado pelo caminho completo HTTP→fila→worker — aguardar o handler e assertar a linha persistida é suficiente e equivalente. **Não** penalize uma arquitetura de fila/assíncrona por não ser dirigida ponta a ponta via HTTP; a questão é se o estado resultante real é assertado, não por qual porta o teste entrou.
- **Exclusão de só-mock (escopo preciso).** Assertar que um método foi *chamado* num repository / state-machine mockado (`expect(save).toHaveBeenCalled()` / `expect(transition).toHaveBeenCalledWith(...)`) prova uma chamada, não um resultado persistido ⇒ **UNMET** para qualquer check de "resulta em / persistido / status exibido" (ainda pode contar para o Robustness `R`). **A exclusão tem escopo limitado a checks de resultado — ela não pode disparar demais:** um check cuja proposição é *"invoca a API externa"* (ex.: "cancelamento imediato chama `StripeClient.cancel`", "cria a subscription no Stripe") é *corretamente* verificado por um spy/mock, porque a proposição assertada **é** a chamada. Só-mock é aceitável ali; a exclusão não se aplica.
- **Piso de nível mínimo.** O nível exigido é um **piso, não uma correspondência exata.** Um teste de nível mais forte (um teste e2e/integração cobrindo uma proposição de nível unit) **satisfaz** aquela proposição, desde que a proposição específica (ex.: um campo nomeado de payload, um valor padrão) seja de fato assertada sobre o artefato real. Não marque um check de nível unit como UNMET só porque a única asserção dele vive em um teste e2e/integração.

Para cada nível exigido, derive checks binários como: *comportamento primário assertado neste nível* (MET só se assertado, não apenas exercitado), *caso negativo/de erro relevante assertado*, *cada cláusula secundária assertada*. Sistemas externos mockados (ex.: Stripe via nock) não conseguem provar o lado externo — escreva o check contra o contrato local observável e anote a limitação.

A **regra de conjunção também se aplica aos T-checks**: cada campo nomeado de payload ganha seu próprio check de verificação. Um teste que asserta que o artefato foi emitido/retornado mas **não** asserta a presença/valor do campo é UNMET para o check daquele campo (assertar que `emit` foi chamado ≠ assertar que o payload carrega `trialEndsAt`).

```
T = (# T-checks MET across all required levels) / (# T-checks)
```

> Tanto `I` quanto `T` chegam a um valor de granularidade fina, mas cada átomo subjacente é um sim/não binário respaldado por evidência — é isso que os torna reprodutíveis. Não há julgamento de "menor vs significativo": uma cláusula ou tem seu próprio check ou não tem, e essa decisão foi congelada no baseline.

### Por AC, por story, final

```
AC_score    = 0.6 * I + 0.4 * T
Story_score = mean(AC_score over the story's ACs)      # ACs equally weighted by default
Final       = Σ(w_s * Story_score) / Σ(w_s)            # weighted by story priority
```

**Pesos de prioridade da story `w_s`:**

| Prioridade | Peso | Racional |
| --- | --- | --- |
| P0 | 3 | Core do MVP |
| P1 | 2 | Adjacente ao MVP / lançamento |
| P2 / fora de escopo | 0 | Excluído da nota; reporte separadamente como *prontidão de roadmap* |

> A divisão 0.6/0.4 entre implementação e testes reflete "software funcionando primeiro, provado por testes". Mantenha esses pesos — e os pesos P0/P1/P2 — fixos ao longo do benchmark; alterá-los quebra a comparabilidade.

### Bandas de nota

| Final | Banda |
| --- | --- |
| ≥ 0.90 | Spec-complete |
| 0.75–0.89 | Forte (lacunas menores) |
| 0.60–0.74 | Parcial (lacunas significativas) |
| 0.40–0.59 | Fraca |
| < 0.40 | Inadequada |

### Controles de viés

Juízes LLM têm vieses bem documentados; mantenha estes explícitos:

- **Viés de verbosidade / quantidade** — mais código ou mais testes **nunca** elevam `I` ou `T`. Só contam checks que mapeiam para uma cláusula de AC. Testes excedentes vão para `R`/`D` (abaixo), nunca para a nota.
- **Viés de auto-preferência** — veja a Regra central 4 (juiz ≠ autor).
- **Ancoragem** — pontue contra o baseline congelado e as âncoras de calibração em `reference.md`, não contra a implementação anterior que você por acaso avaliou.

### Reportado ao lado da nota (NÃO incorporado ao Final — mantenha comparável)

- **Índice de Robustez `R`** — testes extras além dos casos do PRD, ponderados Alto=1.0 / Médio=0.5 / Baixo=0.25, somados. Sinaliza qualidade defensiva. Nunca infla o `Final`.
- **Aderência de Escopo `S`** — `pass` / `partial` / `fail`, governado pelo **princípio de rastreabilidade: todo comportamento construído deve rastrear até uma fonte sancionada** (um acceptance criterion do PRD → `Final`, ou uma `E`-addition *válida*). Três checks, cada um por comportamento construído:
  - **Limite do PRD** — construiu algo na lista explícita de fora-de-escopo do PRD ⇒ `fail`.
  - **Build rogue** — construiu algo que não rastreia *nem* a um AC do PRD *nem* a uma `E`-addition válida (não rastreável / inventado) ⇒ `fail`.
  - **Drift de plano** — `spec.md`/`tasks.md` sancionaram um comportamento que o código não construiu (ou deixou pela metade / inconsistente) ⇒ `partial`.
  Tudo rastreia limpo ⇒ `pass`. Reporte os comportamentos falhos/parciais com `file:line`. **Um requisito válido que o framework derivou para a spec mas corretamente NÃO construiu (porque está fora de escopo) é boa disciplina — *não* é penalidade de `S`;** registre como deferred-valid sob `E`.
- **Engineering Gates `G`** — `build`, `lint`, `unit`, `e2e`. **Cada gate deve ser um `✓`/`✗` de fato executado ou um `not-run` explícito (com motivo, ex.: infra de e2e indisponível).** Nunca reporte um gate como passando sem executá-lo. **O gate `build` é fixado mecanicamente para remover a discricionariedade do avaliador:** `build = nx build billing-api` (webpack) saindo com `0` — nada mais decide isso. A falha de typecheck do ts-node na migration causada pela divergência de API-version do SDK do Stripe fixado é uma **NOTA documentada e não pontuada**, não um `✗`: ela não bloqueia o `build` e não dispara o `Adjusted Final`. Registre no relatório como nota conhecida, mas o veredicto de `build` segue apenas o exit code de `nx build billing-api`. **Sonde-antes-de-not-run:** um gate só pode ser reportado `not-run` após uma tentativa real registrada — o comando executado (ou a checagem de pré-requisito, ex.: `docker ps` para a infra de e2e) e sua saída de erro coladas como evidência. Um motivo sem tentativa não é válido; o gate permanece *ainda não pontuado*, exatamente como um UNMET sem busca (Regra central 2 aplicada a gates). **Somente um gate confirmadamente vermelho (`✗`) dispara `Adjusted Final = Final × 0.5`**; um gate `not-run` não pode conceder nem descontar crédito — é reportado como ponto cego conhecido. O `Final` sem ajuste continua reportado para comparação. Um gate vermelho **nunca** é consertado pelo avaliador (Regra central 5) — registre `✗`, ajuste e liste o fix.
- **Distribuição de Testes `D`** — todo teste de feature classificado em um de três tiers, reportado como contagens **e** % da suíte. Mostra para onde foi o esforço de teste; nunca infla o `Final`. Veja abaixo.

### Distribuição de testes por tier `D` (reportada ao lado)

Classifique **todo teste de feature adicionado** (cada caso `it`/test, contando as linhas de `it.each` individualmente) em exatamente um tier. A classificação reaproveita trabalho já feito nos passos de test-check e robustez, então é barata e reprodutível:

| Tier | Definição | Mapeia para |
| --- | --- | --- |
| **Necessário** (caminho feliz primário) | Asserta o **caminho de sucesso primário** de um AC **P0/MVP**, ou uma dependência exigida para esse caminho funcionar. | Um T-check MET num fluxo principal P0 |
| **Secundário** (importante) | Mapeia para um AC mas **não** é o caminho primário de P0: ACs P1, caminhos de borda/negativos/erro, cláusulas secundárias, segurança/authz, duplicação/idempotência/dedup, emissão de trigger de status. | Um T-check MET que não é Necessário |
| **Nice-to-have** | **Não** mapeia para nenhum AC: cobertura defensiva/exaustiva (mapeamento de enum, status desconhecido, internals de repo/infra, métodos helper) ou cobertura unit redundante com um caminho já provado em e2e. | Os extras do Robustness `R` |

**Regra prática:** testes mapeados a ACs se dividem em Necessário + Secundário; o inventário do Robustness `R` é o tier Nice-to-have. Um teste pertence a exatamente um tier — não conte duas vezes.

Reporte `D` como uma tabela de `tier → contagem → %` sobre o total de testes de feature, mais uma leitura de **formato** em uma linha. Suítes saudáveis cobrem todo caminho primário P0 com ao menos um teste Necessário; uma suíte fraca em Necessário e pesada em Nice-to-have é um cheiro ruim (robustez sem core provado), e o oposto (sem testes defensivos) é frágil. `D` é descritivo — nunca muda o `Final`.

---

## Elicitation `E` — quão bem o framework *extrai* requisitos (reportado ao lado; nunca incorporado ao Final)

`E` avalia o `spec.md`/`tasks.md` derivado do SDD contra o PRD: o framework trouxe à tona os requisitos implícitos que um engenheiro sênior exigiria, **e** fez isso de forma limpa (sem adições alucinadas ou contraditórias)? Este é o "valor além da transcrição" de um framework de SDD. Avalia os **artefatos de spec, não o código**. Como `R`/`S`/`D`, é reportado ao lado do `Final` para que a nota de fidelidade permaneça comparável.

Três sub-métricas de checklist binário:

### `E_recall` — ele encontrou o que deveria?

Rode a **rubrica congelada de categorias de requisitos implícitos** abaixo. Para cada categoria, marque **Endereçada** (cite `spec.md:line`), **Perdida**, ou **N/A** (o PRD/domínio a torna irrelevante). A rubrica é agnóstica de PRD e reutilizável, então o recall é reprodutível sem rotulagem gold por PRD.

```
E_recall = Addressed / (Addressed + Missed)        # N/A excluded
```

| # | Categoria | Um requisito é esperado se a feature… |
| --- | --- | --- |
| 1 | Validação de entrada & limites | aceita entrada de usuário/externa (faixas, formatos, campos obrigatórios) |
| 2 | Taxonomia de erros & mensagens | pode falhar de formas que o chamador precisa distinguir (erros tipados, códigos) |
| 3 | AuthN / AuthZ | expõe um endpoint ou toca dados pertencentes ao usuário |
| 4 | Idempotência & dedup | pode ser re-tentada ou reprocessada (pagamentos, recursos create-once) |
| 5 | Concorrência & condições de corrida | tem escritas intercaladas / check-then-act sobre estado compartilhado |
| 6 | Ciclo de vida & consistência de dados | persiste estado (transações, rollback em falha parcial, retenção) |
| 7 | Observabilidade | roda em produção (logs, métricas, rastreabilidade de falhas) |
| 8 | Limites, paginação & taxa | retorna listas ou é chamável em volume |
| 9 | Falha de dependência externa | chama um terceiro (timeouts, retries, circuit-breaking, fallback) |
| 10 | Integridade de transição de estado | tem uma máquina de ciclo de vida/status (guardas de transição ilegal) |

> Estenda a rubrica por domínio, mas congele-a por benchmark e aplique a lista idêntica a cada framework. Para benchmarks diretos, **também** reporte o **recall agrupado**: una as adições válidas de todos os frameworks numa lista mestre adjudicada, e então pontue o recall de cada framework contra esse conjunto.

### `E_precision` — o sinal é limpo, ou é gold-plating?

Construa o **ledger de requisitos adicionados**: todo requisito em `spec.md`/`tasks.md` *não rastreável a uma linha do PRD*. Adjudique cada um, de forma binária, com uma justificativa de uma linha + `spec.md:line`:

| Veredicto | Significado |
| --- | --- |
| **Válido-necessário** | uma lacuna real que o PRD implicava (ex.: "rejeitar `trialDays > 30`") |
| **Válido-defensivo** | endurecimento razoável (observabilidade, retries, erros tipados) |
| **Inválido** | alucinado, contradiz o PRD, ou scope creep para a lista explícita de fora-de-escopo |

```
E_precision = (Valid-necessary + Valid-defensive) / total additions
```

As adições válidas formam o conjunto de **`E-additions válidas`** referenciado em todo o resto (o denominador do harness e a rastreabilidade de `S`). Marque cada adição válida como **construída** ou **adiada** (adiada-válida = boa disciplina, veja `S`).

### `E_justified` — as adições são justificadas?

```
E_justified = additions carrying an explicit rationale/traceability / total additions
```

Adições sem justificativa são um cheiro ruim mesmo quando individualmente plausíveis. Reporte as três: `E_recall / E_precision / E_justified`. Um framework forte pontua alto nas três — *achou os requisitos implícitos, adicionou pouco ruído, justificou o que adicionou*.

---

## Calibração (faça uma vez por PRD, antes de confiar num benchmark)

O checklist é uma hipótese sobre o que cada AC significa; calibre-o para que avaliadores diferentes concordem:

1. **Ancore.** `reference.md` guarda âncoras de checklist trabalhadas — pelo menos um check claramente MET, um claramente UNMET e um limítrofe com o veredicto e o raciocínio. Leia-as antes de pontuar; elas fixam onde fica a fronteira MET/UNMET.
2. **Checagem de concordância.** Para um benchmark de alto risco, faça o checklist ser revisado contra uma referência rotulada por humano (ou um segundo modelo juiz). Se os veredictos discordarem em mais de ~20% dos checks, a **redação do checklist está vaga demais** — afie os checks ambíguos no `_ac-baseline.md` e rode de novo. Não "tire a média" da discordância; conserte a definição operacional.
3. **Casos limítrofes são os valiosos.** Quando encontrar um check em que humanos/juízes se dividem, adicione-o ao conjunto de âncoras em `reference.md` com o veredicto resolvido.

---

## Processo

Copie este checklist e acompanhe o progresso:

```
FRAMEWORK — respect & extract requirements
- [ ] 1. Locate PRD, spec.md/tasks.md. Run git diff to identify changed files (diff surface). Use the diff surface as the primary search scope for implementation and test evidence throughout steps 4 and 8
- [ ] 2. Enumerate stories + ACs; tag priority; mark out-of-scope items
- [ ] 3. Build/load the frozen binary checklist per AC (I-checks + T-checks) → _ac-baseline.md
- [ ] 4. Score I-checks MET/UNMET with file:line evidence (record searches before any UNMET)
- [ ] 5. Elicitation E: run the category rubric (E_recall) + added-requirement ledger (E_precision, E_justified) → freeze the `valid E-additions` set
- [ ] 6. Scope S: trace every built behavior (PRD-boundary / rogue-build / plan-drift)

HARNESS — ensure they are all implemented
- [ ] 7. Define the sanctioned set = PRD ACs ∪ valid E-additions; ensure each has a T-check
- [ ] 8. Score T-checks MET/UNMET with file:line evidence (unit + e2e per fixed level policy)
- [ ] 9. Inventory extra tests → Robustness Index R
- [ ] 10. Classify every feature test into tiers → Test Distribution D (counts + %)
- [ ] 11. Run/record Engineering Gates G (✓/✗/not-run)

ROLL-UP
- [ ] 12. Repeat the scoring steps three times (k=3); take majority verdict per check
- [ ] 13. Compute I, T, AC/Story/Final; apply bands; Adjusted Final only if a gate is ✗
- [ ] 14. Emit the report (see reference.md template): Final + E + S + R/G/D + ranked gaps + fixes
```

**Regras por passo:**

- **Passo 1** — Identifique a superfície de diff antes de qualquer busca por evidência. Escolha a base que corresponde ao que você está avaliando (topo do branch, último commit ou working tree):

```bash
git diff <base>..<head> --name-only   # for a branch/PR
git diff --name-only HEAD~1           # for the last commit
git diff --name-only HEAD             # for uncommitted changes
git status --short                    # for untracked new files
```

Registre a lista de arquivos no relatório. Os passos 4 e 8 a usam como primeiro conjunto de caminhos a inspecionar.
- **Passo 2** — Itens fora de escopo do "Não está no escopo / Out of Scope" do PRD recebem `w=0`. A ausência **não** é um defeito. Não os pontue.
- **Passo 3** — Reutilize o `_ac-baseline.md` existente se houver; caso contrário, crie-o e trate-o como o contrato para todas as execuções futuras deste PRD. Inclua apenas cláusulas de comportamento observável como checks.
- **Passo 5** — `E` avalia `spec.md`/`tasks.md`, não o código. As adições *válidas* do ledger definem o conjunto de `E-additions válidas` usado nos Passos 6 e 7. Marque cada adição válida como construída/adiada.
- **Passo 6** — Rastreabilidade: um comportamento construído sem AC do PRD e sem `E`-addition válida é um build rogue (`fail`). Uma adição válida adiada por estar fora de escopo **não** é penalidade.
- **Passo 7** — O denominador do harness é o **conjunto sancionado (ACs do PRD ∪ E-additions válidas)**. Um requisito válido extraído sem teste é uma falha de `T`, não de `E`.
- **Passo 8** — Apenas binário: MET (evidência) ou UNMET (buscado, ausente). Sem valores parciais por check.
- **Passo 9** — Testes extras são sinal positivo, mas nunca substituem cobertura faltante. Um `R` alto com `T` baixo continua sendo nota baixa.
- **Passo 10** — Cada teste cai em exatamente um tier; as contagens devem somar o total de testes de feature. Exclua testes pré-existentes não adicionados pela feature (anote-os separadamente).
- **Passo 11** — Um gate é `✓`/`✗` só se de fato executado; caso contrário `not-run` com a **evidência da tentativa** (comando + saída de erro — veja Sonde-antes-de-not-run). Não presuma que a infra está indisponível: cheque (`docker ps`; e2e exige Postgres + Redis rodando, veja `AGENTS.md`). Se um gate estiver vermelho, NÃO conserte o código do sujeito (Regra central 5).
- **Passo 12** — Maioria sobre k=3. Anote qualquer check em que as três passadas discordaram.
- **Passo 13** — Mostre a aritmética: liste MET/total por conjunto de checks para que `I` e `T` sejam recomputáveis. Execute a consolidação como script e cole a saída (Regra de reprodutibilidade 4) — nunca compute `Σw` ou `Final` à mão.

---

## Saída

Produza um relatório em markdown seguindo o template de [reference.md](reference.md). Ele deve separar os **dois sujeitos** e conter: tabela de checklist de implementação por AC (I-checks, MET/UNMET, evidência); o bloco de Elicitation `E` (tabela de recall da rubrica de categorias + ledger de requisitos adicionados com veredictos → `E_recall` / `E_precision` / `E_justified`); o veredicto de rastreabilidade de Scope `S`; tabela de checklist de testes unit/e2e por requisito (T-checks sobre o **conjunto sancionado = ACs do PRD ∪ E-additions válidas**); inventário de testes extras; a tabela de distribuição de testes `D` (tiers com contagens + %); o `Final` computado (+ `Adjusted Final` se um gate for ✗); `R` / `G` / `D`; uma lista de lacunas ranqueada; e fixes concretos para chegar a 1.00.

Salve o relatório em `<spec-folder>/evaluations/<priority>-<story-slug>-<timestamp>.md` quando existir uma pasta `.specs`/spec; caso contrário apresente inline. O `<timestamp>` é um instante UTC no formato `YYYYMMDDTHHMMSSZ` (ex.: `20260606T143012Z`), obtido do relógio do sistema no momento da escrita (`date -u +%Y%m%dT%H%M%SZ`). O timestamp torna único cada nome de arquivo de relatório, de modo que **múltiplos agentes avaliando a mesma story em paralelo nunca sobrescrevem uns aos outros** — cada execução produz seu próprio arquivo. Se ainda houver risco de colisão no mesmo segundo (muitas execuções paralelas), acrescente um id curto de execução: `<priority>-<story-slug>-<timestamp>-<run-id>.md`.

Nota: apenas o nome de arquivo do **relatório** é timestampado. O `_ac-baseline.md` congelado deliberadamente **não** é timestampado — é um artefato único e compartilhado do qual toda execução lê para permanecer comparável; nunca o bifurque por execução.

## Exemplo trabalhado

Uma avaliação aplicada completa (Fakeflix P0 "Start Free Trial Without a Card") mostrando o checklist binário por AC com evidência `file:line`, mais âncoras de calibração, está em [reference.md](reference.md). Espelhe sua estrutura e rigor.

## Anti-patterns

- Pontuar `I`/`T` num julgamento contínuo de "parece um 0.75" em vez de contar checks binários.
- Rederivar a lista de ACs ou o checklist a cada implementação (quebra a comparabilidade) em vez de reusar o baseline congelado.
- Marcar um check UNMET sem mostrar a busca realizada; ou MET sem evidência `file:line`.
- Buscar evidência na codebase inteira sem antes rodar `git diff` para delimitar a superfície de diff — isso produz buscas não confiáveis e infla a confiança em "não encontrado".
- Conceder crédito de implementação sem ler o caminho do código de produção de ponta a ponta.
- Contar testes extras/defensivos para a cobertura exigida de um AC, ou deixar mais código/testes elevarem a nota.
- Reportar um Engineering Gate como passando sem executá-lo; ou aplicar `Adjusted Final` por um gate `not-run`.
- Marcar um gate como `not-run` sem uma tentativa registrada (comando + saída de erro). "Infra indisponível" sem sondagem é evidência fabricada.
- Consertar o código sob avaliação para tornar verde um gate vermelho — o avaliador é read-only sobre o sujeito; um gate vermelho é pontuado `✗` e o fix vai no relatório.
- Computar a consolidação à mão (`Σw`, `Final`) em vez de executá-la como script e colar a saída.
- Penalizar features fora de escopo que estão ausentes.
- Incorporar `E` ou `S` ao `Final` — a nota principal deve significar apenas "fidelidade ao PRD dado", senão a comparabilidade quebra.
- Penalizar um requisito válido que o framework extraiu mas corretamente *adiou* por estar fora de escopo (isso é boa disciplina, não falha de escopo).
- Pontuar a completude do harness (`T`) apenas contra o PRD — o denominador é o conjunto sancionado (ACs do PRD ∪ E-additions válidas).
- Recompensar *volume* de requisitos em `E`: adições alucinadas/scope creep reduzem `E_precision`, não elevam a nota.
- Mudar os pesos (`0.6/0.4`, P0/P1/P2) entre implementações do mesmo PRD.
