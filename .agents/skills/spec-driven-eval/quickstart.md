# Início rápido — rodando a eval de ponta a ponta

Este é o passo a passo operacional do `spec-driven-eval`. O `SKILL.md` principal é a metodologia de pontuação; este arquivo é o workflow que produz algo para pontuar e depois o avalia.

Rode a eval como **4 sessões de chat separadas**, em ordem. Use uma sessão *nova* para cada uma, para que o contexto não vaze de um estágio para o próximo — a nota nunca pode ver como o código foi produzido. Os estágios compartilham estado apenas pelo **git** (o branch da implementação + uma ref base) e pelo arquivo de **baseline congelado**.

Como cada sessão começa limpa, **anexe a guideline `spec-driven-eval` às sessões de eval (1 e 4)** — elas a seguem. As sessões 2 e 3 seguem as instruções do *próprio framework sob teste*.

Preencha estes placeholders para a sua avaliação: `<PATH TO YOUR PRD>` · `<YOUR SPEC FOLDER>` (onde vivem o baseline + relatórios) · `<FRAMEWORK>` · `<BASE_REF>` (o commit/branch de onde a implementação partiu) · `<RUN_BRANCH>`.

## O que o PRD precisa conter

O passo de congelamento do baseline (Sessão 1) lê três coisas do PRD, então garanta que o seu as tenha: **user stories agrupadas por prioridade explícita** (`P0`/`P1`/`P2`), **acceptance criteria numerados** por story (a unidade de pontuação), e uma **lista explícita de fora-de-escopo** (para que features ausentes não sejam penalizadas). Abaixo há um PRD fictício mínimo com esse formato — copie a estrutura e troque pela sua feature.

**Escreva o PRD a partir de uma feature completa e bem conhecida, e cubra todas as suas necessidades de negócio.** O PRD *é* a fonte de verdade — toda nota é medida contra ele. Um PRD raso ou parcial (stories faltando, ACs vagos, sem limites explícitos) convida ao **scope drift**: frameworks constroem coisas que o PRD nunca sancionou (prejudicando a aderência de escopo) ou pulam necessidades que o PRD nunca declarou (prejudicando a fidelidade) e, como a intenção faltante não está no baseline congelado, a nota resultante não é nem justa nem reprodutível. Baseie-o numa feature que você entenda bem o suficiente para enumerar por completo — cada story, cada AC, cada limite explícito — para que o baseline capture a intenção inteira e os números permaneçam comparáveis entre execuções.

```markdown
# PRD — Wishlist

## P0 — Add item to wishlist
As a logged-in user, I want to save an item to my wishlist so I can find it later.

**Acceptance criteria**
- AC1: A logged-in user can add an item; the response returns the saved item with its `wishlistId` and `createdAt`.
- AC2: Adding an item the user already has does not create a duplicate; the existing entry is returned.
- AC3: An unauthenticated request is rejected with `401`.

## P1 — Remove item from wishlist
As a logged-in user, I want to remove an item so my list stays relevant.

**Acceptance criteria**
- AC1: A logged-in user can remove an item they own; the item no longer appears in their list.
- AC2: Removing an item the user does not own returns `404` and changes nothing.

## P2 — Share wishlist via public link
(Lower priority — graded at weight 0; absence is not a defect.)

**Acceptance criteria**
- AC1: A user can generate a read-only public link to their wishlist.

## Out of scope
- Wishlist item price-drop notifications.
- Sharing to external social networks.
```

Notas que mapeiam para como isso é pontuado:

- **Os rótulos de prioridade dirigem os pesos** — `P0` → 3, `P1` → 2, `P2`/fora de escopo → 0. Rotule cada story; uma story sem rótulo é sinalizada como `ASSUMED`.
- **Conjunções viram checks separados** — o "`wishlistId` **e** `createdAt`" do AC1 são dois I-checks (um por campo). Explicite cada campo que você espera num payload retornado/persistido/emitido.
- **A lista de fora-de-escopo delimita o escopo** — construir algo dela conta contra a aderência de escopo; *não* construir é o correto.

## Sessão 1 — Congelar o baseline (uma vez por PRD)

Abra um chat novo (com a guideline anexada) e cole:

```
Following this spec-driven-eval guideline, do ONLY the baseline-freeze steps
(locate PRD → enumerate stories + acceptance criteria → build the binary checklist)
for the PRD at <PATH TO YOUR PRD>: enumerate every story + acceptance criterion,
tag priority from the PRD's explicit P0/P1/P2 labels, decompose each AC into binary
I-checks and T-checks, and write <YOUR SPEC FOLDER>/evaluations/_ac-baseline.md.
Do not grade any implementation.
```

Reutilize esse arquivo de baseline verbatim em toda execução posterior — nunca o rederive. Ele é a âncora de comparabilidade de toda a avaliação.

## Antes da Sessão 2 — limpe o working tree (NÃO pule)

Depois de cortar seu branch de execução a partir da base, você **precisa** limpar o working tree *antes* de planejar. Isso é crítico para uma execução justa: `git checkout` **nunca** remove arquivos ignorados pelo git, então sobras de uma execução anterior sobrevivem no seu branch supostamente "limpo" e **vazam silenciosamente a resposta da execução anterior** para a nova.

Limpe, no mínimo:

- **Saída compilada / de build** — ex.: diretórios de build, saída transpilada, diretórios de coverage e cache.
- **Artefatos de plano SDD gerados antes** — o `spec.md`/`tasks.md` do framework anterior e seus diretórios de trabalho.
- **Drift de dependências** — reconcilie as dependências às versões travadas (uma instalação limpa com lockfile congelado).

Faça isso *depois* de cortar o branch e *antes* de planejar, para que a limpeza não apague o novo plano.

## Sessão 2 — Planejar

Abra um chat novo no branch limpo:

```
I'm on branch <RUN_BRANCH> (cut from <BASE_REF>).
Read <FRAMEWORK>'s own planning instructions and follow them exactly. Using ONLY
the PRD at <PATH TO YOUR PRD> as input, run <FRAMEWORK>'s planning flow to produce
its spec + tasks artifacts. Plan only — do not write any production code.
```

## Sessão 3 — Implementar

Abra um chat novo no mesmo branch:

```
I'm on branch <RUN_BRANCH>. Read <FRAMEWORK>'s own implementation instructions and
follow them. Implement STRICTLY from the spec.md/tasks.md on this branch — do NOT
read the PRD. Write production code plus unit and e2e tests.
```

Manter isso cego ao PRD é deliberado: quem implementa só deve ver o plano do framework, para que a nota meça o framework, não o modelo preenchendo lacunas a partir do PRD.

## Sessão 4 — Avaliar (a guideline)

Abra um chat novo no branch implementado (com a guideline anexada):

```
Following this spec-driven-eval guideline, grade the implementation on the current
branch against base <BASE_REF>. REUSE the frozen
<YOUR SPEC FOLDER>/evaluations/_ac-baseline.md verbatim — do not re-enumerate.
Score I-checks and T-checks with file:line evidence over the diff
(git diff <BASE_REF>..HEAD), run E/S/R/G/D, do k=3, compute Final with a script, and
write a timestamped report to <YOUR SPEC FOLDER>/evaluations/. Do NOT modify the code
under evaluation.
```

## Avaliando vários frameworks contra um PRD

Congele o baseline uma vez (Sessão 1), depois repita as Sessões 2–4 por framework com um avaliador *novo* a cada vez, e agregue os `Final`s. O mesmo baseline + o mesmo avaliador em todas as execuções é o que mantém os números comparáveis.
