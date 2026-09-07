# spec-driven-eval — Template de relatório, âncoras de calibração & exemplo trabalhado

## 1. Template de relatório

Copie e preencha. Substitua o texto entre colchetes; mantenha a ordem das seções. `I` e `T` são **derivados** das contagens MET/total, nunca digitados diretamente.

```markdown
# Evaluation — [Priority]: [Story title]

**Feature**: [feature name]
**Source of truth**: [PRD ref] (cross-ref [spec.md IDs] if present)
**AC baseline**: [_ac-baseline.md ref — frozen checklist used]
**Judge model**: [model] (author model: [model] — flag if same)
**Module / paths**: [where the code lives]

## Acceptance criteria
- AC1 — [restate the criterion]
- ACn — ...

## Implementation checklist (binary — MET/UNMET, evidence required for MET)
| AC | I-check (atomic, observable) | Verdict | Evidence (file:line) |
| --- | --- | --- | --- |
| AC1 | I1. [behavior 1] | MET | `path:line` |
| AC1 | I2. [behavior 2] | UNMET | searched: [terms/files], absent |
| ... | ... | ... | ... |

Per-AC: **I = MET / total** → AC1 = x/y = X.XX

---
# FRAMEWORK — extract & respect

## Elicitation E — category rubric (recall)
| # | Category | Verdict | Evidence (spec.md:line) / why N/A |
| --- | --- | --- | --- |
| 1 | Input validation & bounds | Addressed/Missed/N-A | `spec.md:line` |
| ... | ... | ... | ... |

**E_recall = Addressed / (Addressed + Missed) = X.XX**

## Elicitation E — added-requirement ledger (precision + justification)
| # | Requirement added beyond PRD | Verdict | Built? | Justified? | Evidence (spec.md:line) + warrant |
| --- | --- | --- | --- | --- | --- |
| A1 | [requirement] | Valid-necessary | built | yes | `spec.md:line` — [why] |
| A2 | [requirement] | Invalid (creep/hallucination) | — | no | `spec.md:line` — [why] |

**E_precision = valid / total = X.XX** · **E_justified = justified / total = X.XX**
`valid E-additions` set (used by S + harness denominator): [A1, …]

## Scope S — traceability of built behavior
| Built behavior | Traces to | Verdict | Evidence (file:line) |
| --- | --- | --- | --- |
| [behavior] | PRD AC1 / valid add A1 / none | pass / fail (rogue) / fail (PRD out-of-scope) | `path:line` |
| [planned, not built] | spec/tasks | partial (plan drift) | `spec.md:line` |

**S = pass / partial / fail** (deferred-valid out-of-scope additions are NOT penalized)

---
# HARNESS — ensure all implemented

## Test checklist (binary — over the sanctioned set = PRD ACs ∪ valid E-additions)
| Requirement | Source | Level | T-check | Verdict | Evidence (file:line) |
| --- | --- | --- | --- | --- | --- |
| AC1 | PRD | unit | [primary behavior asserted] | MET | `path:line` |
| AC1 | PRD | e2e | [observable contract asserted] | MET | `path:line` |
| A1 | valid add | unit | [extracted requirement asserted] | UNMET | searched, no test |
| ... | ... | ... | ... | ... | ... |

Per-requirement: **T = MET / total** → AC1 = x/y = X.XX  ·  harness completeness = MET / |sanctioned set|

## Extra tests (Robustness — not scored toward ACs)
| # | Extra test | Level | Evidence | Value (High/Med/Low) |
| --- | --- | --- | --- | --- |

## Test distribution by tier (D — reported, not scored)
| Tier | Count | % | Evidence (representative) |
| --- | --- | --- | --- |
| Necessary (P0 primary happy path) | n | xx% | `path:line` |
| Secondary (important) | n | xx% | `path:line` |
| Nice-to-have | n | xx% | `path:line` |
| **Total feature tests** | N | 100% | — |

**Shape**: [one line — e.g. "balanced", "top-light / robustness-heavy", "fragile / no defensive tests"]. (Pre-existing tests excluded: [list/none].)

## Result
| AC | I (MET/total) | T (MET/total) | AC_score = 0.6·I + 0.4·T |
| --- | --- | --- | --- |

| Dimension | Subject | Value |
| --- | --- | --- |
| Story_score / Final (PRD fidelity) | framework+harness | X.XX |
| Elicitation E (recall / precision / justified) | framework | X.XX / X.XX / X.XX |
| Scope Adherence S | framework | pass/partial/fail |
| Harness completeness (T over sanctioned set) | harness | X.XX |
| Engineering Gates G | harness | build/lint/unit/e2e: ✓/✗/not-run |
| Robustness Index R | harness | [sum] |
| Test Distribution D | harness | Necessary xx% / Secondary xx% / Nice-to-have xx% (N tests) |
| Adjusted Final (only if a gate is ✗) | — | Final × 0.5 |
| k=3 disagreements | — | [checks where the 3 passes split, or none] |

**Verdict**: [band + one line]. **Framework**: respects + extracts requirements [read from Final-impl / E / S]. **Harness**: ensures implementation [read from T / G].

## Gaps (ranked) and fixes to reach 1.00
1. [AC] — [UNMET check] → [fix]
```

Para uma consolidação de **PRD inteiro**, acrescente uma seção final:

```markdown
## PRD final grade
| Story | Priority | Weight | Story_score |
| --- | --- | --- | --- |
| [P0 a] | P0 | 3 | X.XX |
| [P1 b] | P1 | 2 | X.XX |
| [P2 c] | P2 | 0 (excluded) | — |

**Final = Σ(w·Story)/Σ(w) = X.XX → [band]**
Roadmap readiness (P2, informational): [notes]

| Whole-PRD reported metrics | Subject | Value |
| --- | --- | --- |
| Elicitation E (recall / precision / justified) | framework | X.XX / X.XX / X.XX |
| Scope Adherence S | framework | pass/partial/fail |
| Harness completeness (T over sanctioned set) | harness | X.XX |
| Engineering Gates G | harness | build/lint/unit/e2e: ✓/✗/not-run |
| Robustness Index R | harness | [sum] |
| Test Distribution D | harness | Necessary xx% / Secondary xx% / Nice-to-have xx% (N tests) |
```

---

## 2. Âncoras de calibração (leia antes de pontuar — elas fixam a fronteira MET/UNMET)

Estes são veredictos de referência. Alinhe seus veredictos ao estilo de raciocínio, não apenas ao resultado. Acrescente novos casos limítrofes aqui sempre que dois avaliadores divergirem em um check.

| Âncora | Check | Veredicto | Por quê |
| --- | --- | --- | --- |
| **Claramente MET** | "Cria uma subscription trialing sem payment method" | **MET** | `stripe.client.ts:59-80` monta os params da subscription sem campo `payment_method` e com `trial_period_days` definido; rastreado de ponta a ponta a partir de `subscription.service.ts:63-106`. Comportamento presente e observável. |
| **Claramente UNMET** | "A resposta 409 de conflito inclui a subscription existente" | **UNMET** | `subscription.service.ts:79-85` lança `ConflictDomainException` e `subscription.controller.ts:50-52` mapeia para 409, mas nenhum payload da subscription existente é anexado. Busquei no corpo da exception + no mapeamento do controller; o dado está genuinamente ausente → UNMET (não parcial). |
| **Limítrofe → resolvido UNMET** | "O padrão de 14 dias de trial é assertado em e2e" | **UNMET** | `subscription-trial.e2e-spec.ts:157-163` *exercita* o caminho padrão (omite `trialDays`) mas nunca asserta que o valor 14 chegou ao Stripe. Exercitado-mas-não-assertado **não** atende um check de verificação (Regra central 3). Se tivesse assertado o valor propagado, seria MET. |
| **Limítrofe → resolvido MET** | "Status trialing concede acesso imediato" | **MET** | `subscription-state-machine.service.ts:28-32,60-62` inclui `Trialing ∈ ACCESS_GRANTED_STATUSES`, rastreado a partir do caminho de sucesso. O comportamento está implementado mesmo que o e2e de endpoint de acesso seja fraco — o check de *implementação* é MET; o check de *teste* correspondente é pontuado separadamente. |
| **Limítrofe → resolvido UNMET** (regra de Conjunção) | "O payload do trigger contém a data de fim do trial" | **UNMET** | O AC dizia "emitir um trigger associado ao usuário **e** à data de fim do trial". O trigger *é* emitido com `userId`, mas `stripe-webhook.service.ts:53-55` constrói apenas `payload: { stripeSubscriptionId }` — `trialEndsAt` está disponível em `localSubscription` (buscado na linha 46) mas nunca é escrito no objeto de payload. Pela regra de Conjunção, cada campo nomeado após o "e" é seu próprio check; a ausência do campo **no ponto de construção** é UNMET independentemente de o `emit(...)` pai estar presente. (Esta é a armadilha que a cláusula de formato de dados da Regra central 3 existe para pegar.) |
| **Limítrofe → resolvido UNMET** (regra de Disjunção, configurável) | "O comportamento de cancelamento é configurável pelo produto" | **UNMET** | O AC dizia "DEVE aplicar o comportamento **escolhido pelo produto**: pausar (recomendado) ou cancelar". `stripe.client.ts:48-50` faz hard-code de `missing_payment_method: 'pause'`; não há chave de config, flag ou variável de ambiente que troque para `'cancel'`. "Escolhido pelo produto" se lê como configurável em runtime, então o check (2) "alternativa alcançável sem mudança de código" é UNMET. O caminho de `cancel` via `customer.subscription.deleted` + `TERMINAL_STATUSES` é um comportamento *diferente* (cancelamento iniciado pelo usuário), não este ponto de decisão controlado pelo produto, então não satisfaz (2). |
| **`T-outcome` MET** (baseado em resultado, neutro quanto ao ponto de entrada) | "Evento de mudança de status de entrada resulta na mudança do status persistido" | **MET** | Um teste de integração invoca o handler contra o banco real e asserta o resultado — ex.: `await processWebhookEvent(customerSubscriptionUpdated)` e depois `expect((await subscriptionRepo.findBy({ stripeSubscriptionId })).status).toBe('paused')`. A linha real persistida é assertada. A entrada é pelo handler diretamente (não HTTP) — isso é aceitável: `T-outcome` é neutro quanto ao ponto de entrada, então um design assíncrono de ack rápido não precisa ser dirigido por HTTP→fila→worker. |
| **`T-outcome` UNMET (só-mock)** | "Evento de cancelamento de entrada atualiza o status exibido para `cancelado`" | **UNMET** | O teste asserta `expect(subscriptionRepo.save).toHaveBeenCalledWith(objectContaining({ status: 'canceled' }))` / `expect(stateMachine.transition).toHaveBeenCalled()` sobre um repo/state-machine mockado. Isso prova uma *chamada*, não um *resultado persistido* — só-mock não satisfaz um check de "resulta em / status exibido" (conta apenas para `R`). **Nota de escopo:** essa exclusão NÃO dispara num check de "invoca a API externa" (ex.: "cancelamento imediato chama `StripeClient.cancel`"), que um spy/mock verifica corretamente porque a proposição assertada *é* a chamada. |
| **`T-outcome` UNMET (só-ingress)** | "Evento de entrada resulta na mudança do status persistido" | **UNMET** | O teste asserta que a linha do evento de entrada foi persistida e um job de processamento foi enfileirado (`expect(eventRepo.findBy(...)).toBeDefined()` + `expect(queue.add).toHaveBeenCalled()`) mas nunca asserta o *status resultante da subscription*. Captura de ingress + enfileiramento não é o resultado — o estado resultante nunca é assertado, então o check `T-outcome` é UNMET (a asserção de enfileiramento pode contar para `R`). |
| **I-check de wiring MET** (ingress assíncrono) | "O endpoint de webhook recebe, verifica e despacha o evento por tipo para o handler" | **MET** | `stripe-webhook.controller.ts:22-40` constrói/verifica a assinatura do evento (`stripe.webhooks.constructEvent(...)`) e roteia por `event.type` (`customer.subscription.updated` → `handleSubscriptionUpdated`). Este é o check dedicado de wiring — distinto do teste `T-outcome` de que o handler produz o estado certo. Um handler correto atrás de uma rota morta/não registrada falharia *este* check mesmo com `T-outcome` verde; aqui a rota existe e despacha, então MET. |

**Regra de fronteira que as âncoras codificam:** MET exige implementado-e-evidenciado (checks de implementação) ou assertado-e-não-apenas-exercitado (checks de teste). Para artefatos com múltiplos campos, cada campo nomeado é checado contra o **objeto de payload construído**, não contra o call site; para alternativas escolhidas pelo produto, o caminho não padrão deve ser alcançável sem mudança de código. Para checks de **persistência/assíncrono (`T-outcome`)**, MET exige assertar o **estado resultante real** (linha persistida no banco / payload retornado) via **qualquer** ponto de entrada (HTTP ou handler/consumidor/service invocado diretamente contra infra real) — uma asserção só de chamada mockada e uma asserção só de ingress (persistir evento + enfileirar) são ambas UNMET, enquanto um check de "invoca a API externa" continua sendo corretamente atendido por spy/mock; o nível exigido é um **piso**, então um teste mais forte satisfaz uma proposição de nível mais fraco. Um efeito entregue de forma assíncrona precisa adicionalmente de um **I-check de wiring** (o endpoint recebe + verifica + despacha por tipo), para que um handler correto atrás de uma rota morta não ganhe crédito total. Qualquer outra coisa é UNMET. Não existe veredicto intermediário por check — o crédito parcial emerge apenas da fração MET/total.

---

## 3. Exemplo trabalhado — Fakeflix P0: Start Free Trial Without a Card

Avaliação real reconstruída sobre o checklist binário. Use como régua de rigor e evidência.

### Acceptance criteria (do PRD, alinhados com `spec.md` STRIPE-01..05)
- AC1 — Usuário autenticado inicia trial com `planId` (+ `trialDays` opcional); criar customer no Stripe se não existir + sub **trialing** **sem payment method**; persistir; retornar `Trialing` + data de fim.
- AC2 — Sem `trialDays` ⇒ padrão 14.
- AC3 — `Trialing`/`Active` existente para o mesmo plano ⇒ rejeitar duplicata **e informar a subscription existente** (409).
- AC4 — Sucesso ⇒ acesso imediato (a state machine concede `Trialing`).
- AC5 — Falha transitória (Stripe/identity) ⇒ erro claro, sem estado inconsistente, retry idempotente (sem duplicata).

### Checklist de implementação
| AC | I-check | Veredicto | Evidência |
| --- | --- | --- | --- |
| AC1 | I1. Resolver `planId`→`stripePriceId` | MET | `subscription.service.ts:63-106` |
| AC1 | I2. Criar customer no Stripe se não existir | MET | `customer-stripe.service.ts:16-38` |
| AC1 | I3. Criar sub trialing **sem** payment method | MET | `stripe.client.ts:59-80` |
| AC1 | I4. Persistir subscription | MET | `subscription.entity.ts:61-62`; service `:97-104` |
| AC1 | I5. Retornar `Trialing` + data de fim | MET | `subscription.controller.ts:35-58` |
| AC2 | I1. Padrão 14 quando `trialDays` é omitido | MET | `subscription.service.ts:27,71` (`DEFAULT_TRIAL_DAYS=14`) |
| AC3 | I1. Rejeitar duplicata active/trialing para o plano (409) | MET | `subscription.service.ts:79-85`; `subscription.controller.ts:50-52` |
| AC3 | I2. Resposta de conflito **informa** a subscription existente | UNMET | busquei no corpo da exception + mapeamento do controller; sem payload da sub existente |
| AC4 | I1. `Trialing` ∈ status com acesso concedido | MET | `subscription-state-machine.service.ts:28-32,60-62` |
| AC5 | I1. Escrita atômica/transacional (sem estado parcial) | MET | `@Transactional` `subscription.service.ts:63` |
| AC5 | I2. Criação idempotente de customer + sub (sem duplicata) | MET | `stripe.client.ts:40-57`, idempotencyKey `:88-95`; `stripeSubscriptionId` único `subscription.entity.ts:61-62` |
| AC5 | I3. Erro de domínio claro em falha transitória | UNMET | 500 cru exposto; sem código/mensagem de erro estável mapeado |

**I por AC:** AC1 5/5=1.00 · AC2 1/1=1.00 · AC3 1/2=0.50 · AC4 1/1=1.00 · AC5 2/3=0.67

### Elicitation E (framework — extrair)

**Rubrica de categorias (recall):**
| # | Categoria | Veredicto | Evidência / por quê |
| --- | --- | --- | --- |
| 1 | Validação de entrada & limites | Endereçada | limite de `trialDays` 1–30 em `spec.md` (→ impl `subscription.service.ts`, e2e E1) |
| 2 | Taxonomia de erros & mensagens | Perdida | falha transitória expõe um 500 cru; nenhum erro tipado especificado (AC5 I3 UNMET) |
| 3 | AuthN / AuthZ | Endereçada | requisito de usuário autenticado levado à spec/controller |
| 4 | Idempotência & dedup | Endereçada | chave de idempotência + `stripeSubscriptionId` único especificados |
| 5 | Concorrência & corridas | Perdida | nenhuma guarda especificada para dois inícios de trial concorrentes no mesmo plano |
| 6 | Ciclo de vida & consistência de dados | Endereçada | escrita transacional especificada |
| 7 | Observabilidade | Perdida | nenhum requisito de logging/métricas/trace no caminho de início do trial |
| 8 | Limites, paginação & taxa | N/A | criação de recurso único; sem endpoint de listagem |
| 9 | Falha de dependência externa | Endereçada | config de retries/timeout do SDK do Stripe especificada (E6) |
| 10 | Integridade de transição de estado | Endereçada | a máquina de status guarda transições ilegais |

**E_recall = 6 Endereçadas / (6 + 3 Perdidas) = 0.67**

**Ledger de requisitos adicionados (precisão + justificativa):**
| # | Requisito além do PRD | Veredicto | Construído? | Justificado? | Evidência + justificativa |
| --- | --- | --- | --- | --- | --- |
| A1 | `trialDays` deve estar entre 1–30 | Válido-necessário | construído | sim | `spec.md` — o PRD implica uma janela de trial sensata |
| A2 | Plano sem `stripePriceId` ⇒ 404 | Válido-necessário | construído | sim | `spec.md` — pré-condição exigida para o trial |
| A3 | Config de retry/timeout do SDK do Stripe | Válido-defensivo | construído | sim | endurecimento de dependência externa |
| A4 | Cache de snapshot do customer | Válido-defensivo | construído | parcial | performance; racional fraco na spec |

**E_precision = 4 válidos / 4 = 1.00** · **E_justified = 3 / 4 = 0.75**
`E-additions válidas` = [A1, A2, A3, A4] (todas construídas; nenhuma adiada)

### Scope S (framework — respeito)
Todo comportamento construído rastreia até um AC do PRD ou uma `E`-addition válida (A1–A4); nada mapeia para a lista de fora-de-escopo do PRD (nenhuma monetização/cancelamento construído); todos os itens de spec/tasks têm código correspondente (sem drift de plano).
**S = pass.**

### Checklist de testes (sobre o conjunto sancionado = AC1–AC5 ∪ A1–A4)
| AC | Nível | T-check | Veredicto | Evidência |
| --- | --- | --- | --- | --- |
| AC1 | unit | Criação do trial assertada | MET | `subscription.service.spec.ts:114-130`; `customer-stripe.service.spec.ts:51-77` |
| AC1 | unit | Ausência de payment method assertada | MET | `stripe.client.spec.ts:149-193` |
| AC1 | e2e | Criação do trial retorna Trialing+fim | MET | `subscription-trial.e2e-spec.ts:129-164` |
| AC2 | unit | Padrão 14 assertado | MET | `subscription.service.spec.ts:114-130`; `stripe.client.spec.ts:178-190` |
| AC2 | e2e | Padrão 14 propagado ao Stripe assertado | UNMET | `:157-163` exercitado, valor não assertado |
| AC3 | unit | Duplicata rejeitada assertada | MET | `subscription.service.spec.ts:151-160` |
| AC3 | unit | Sub existente retornada no conflito assertada | UNMET | não assertado (impl ausente) |
| AC3 | e2e | 409 em duplicata assertado | MET | `subscription-trial.e2e-spec.ts:166-187` |
| AC4 | unit | Status `Trialing` assertado | MET | `subscription.service.spec.ts:126` |
| AC4 | e2e | Acesso de fato concedido assertado | UNMET | sem asserção de `GET .../active` |
| AC5 | unit | Tratamento de falha transitória assertado | MET | `subscription.service.spec.ts:162-167`; `customer-stripe.service.spec.ts:79-87` |
| AC5 | e2e | Retry idempotente (duas vezes ⇒ sem duplicata) assertado | UNMET | sem e2e de retry |
| A1 (adição válida) | e2e | Limite de `trialDays` 1–30 assertado | MET | `subscription-trial.e2e-spec.ts:189-200` |
| A2 (adição válida) | unit | Plano sem `stripePriceId` ⇒ 404 assertado | MET | `subscription.service.spec.ts:142-149` |
| A3 (adição válida) | unit | Config de retry/timeout do SDK do Stripe assertada | MET | `stripe.client.spec.ts:262-270` |
| A4 (adição válida) | unit | Caminho de cache-hit do customer assertado | MET | `customer-stripe.service.spec.ts:38-49` |

**T por AC (PRD):** AC1 3/3=1.00 · AC2 1/2=0.50 · AC3 2/3=0.67 · AC4 1/2=0.50 · AC5 1/2=0.50
**Completude do harness sobre o conjunto sancionado:** todos os ACs do PRD têm ≥1 T-check MET; as adições válidas A1–A4 estão todas testadas ⇒ nenhum requisito extraído fica não verificado.

> **Nota de reconciliação.** Como A1–A4 são requisitos *válidos e sancionados*, seus testes (E1, E2, E6, E3 abaixo) são cobertura do conjunto sancionado — sob o modelo de dois sujeitos, eles se reclassificam de *Nice-to-have* (Robustness `R`) para *Secundário*. As figuras de `R`/`D` abaixo são mostradas no enquadramento legado só-PRD por continuidade; numa execução nova pontuada sob o conjunto sancionado, `R` encolheria e Secundário subiria proporcionalmente. Apenas testes genuinamente não mapeáveis a ACs (ex.: E5, internals do snapshot-mapper) permanecem em `R`.

### Testes extras (Robustez)
| # | Teste extra | Nível | Evidência | Valor |
| --- | --- | --- | --- | --- |
| E1 | `trialDays` fora de 1–30 ⇒ 400 | e2e | `subscription-trial.e2e-spec.ts:189-200` | Alto |
| E2 | Plano sem `stripePriceId` ⇒ 404 | unit | `subscription.service.spec.ts:142-149` | Alto |
| E3 | Cache hit de customer ⇒ sem chamada a identity/Stripe | unit | `customer-stripe.service.spec.ts:38-49` | Médio |
| E4 | `ensureCustomer` existente-vs-criar | unit | `stripe.client.spec.ts:110-147` | Médio |
| E5 | Snapshot mapper: customer expandido / período nulo | unit | `stripe.client.spec.ts:25-77` | Médio |
| E6 | Config do SDK do Stripe (retries/timeout) | unit | `stripe.client.spec.ts:262-270` | Baixo |

R = 1.0+1.0+0.5+0.5+0.5+0.25 = **3.75**

### Distribuição de testes por tier (D)
| Tier | Contagem | % | Evidência representativa |
| --- | --- | --- | --- |
| Necessário (caminho feliz primário P0) | 4 | 27% | criação do trial unit+e2e `subscription.service.spec.ts:114-130`, `subscription-trial.e2e-spec.ts:129-164`; sem payment method `stripe.client.spec.ts:149-193`; status de acesso imediato `subscription.service.spec.ts:126` |
| Secundário (importante) | 5 | 33% | padrão-14 `:114-130`,`:157-163`; guarda de duplicata `:151-160`,`:166-187`; idempotência/retry `:162-167` |
| Nice-to-have | 6 | 40% | E1–E6 (inventário de robustez acima) |
| **Total de testes de feature** | 15 | 100% | — |

**Formato**: top-light / robustness-heavy — apenas 27% provam o caminho primário P0 enquanto 40% é defensivo; todo caminho P0 tem ≥1 teste Necessário, então é aceitável, mas a camada de caminho feliz é fina. (Testes pré-existentes excluídos: nenhum.)

### Resultado
| AC | I | T | AC_score = 0.6·I + 0.4·T |
| --- | --- | --- | --- |
| AC1 | 1.00 | 1.00 | 1.00 |
| AC2 | 1.00 | 0.50 | 0.80 |
| AC3 | 0.50 | 0.67 | 0.57 |
| AC4 | 1.00 | 0.50 | 0.80 |
| AC5 | 0.67 | 0.50 | 0.60 |

| Dimensão | Sujeito | Valor |
| --- | --- | --- |
| Story_score / Final (fidelidade ao PRD) | framework+harness | (1.00+0.80+0.57+0.80+0.60)/5 = **0.75** |
| Elicitation E (recall / precisão / justificado) | framework | 0.67 / 1.00 / 0.75 |
| Aderência de Escopo S | framework | pass (todo comportamento construído é rastreável; nada fora de escopo; sem drift de plano) |
| Completude do harness (T sobre o conjunto sancionado) | harness | todos os ACs do PRD + A1–A4 verificados (as lacunas são de força de asserção, não buracos de cobertura) |
| Engineering Gates G | harness | rode antes de reportar: `nx build billing && nx lint:check billing`, `nx test:unit billing`, `yarn db:migrate billing && nx test:e2e billing` → registre ✓/✗ de cada (não presuma) |
| Índice de Robustez R | harness | 3.75 (enquadramento legado — veja a nota de reconciliação) |
| Distribuição de Testes D | harness | Necessário 27% / Secundário 33% / Nice-to-have 40% (15 testes) — top-light, robustness-heavy |
| Discordâncias em k=3 | — | nenhuma (todos os checks estáveis entre as passadas) |

**Veredicto**: Forte (0.75).
- **Framework — respeito + extração:** honra o PRD e fica nos limites (`S pass`); extrai de forma limpa (`E_precision 1.00`) mas com cobertura moderada dos requisitos implícitos (`E_recall 0.67` — perdeu taxonomia de erros, concorrência, observabilidade).
- **Harness — garantir implementação:** todo requisito sancionado é testado, mas várias asserções são fracas (exercitado-mas-não-assertado), então `T` fica atrás de `I`.

### Lacunas (ranqueadas) e fixes
**Framework (respeito/extração):**
1. `E_recall` — taxonomia de erros perdida → especifique um erro tipado de falha transitória (também corrige AC5 I3).
2. `E_recall` — concorrência perdida → especifique uma guarda para dois inícios de trial concorrentes no mesmo plano.
3. `E_recall` — observabilidade perdida → especifique logging/métricas no caminho de início do trial.
4. AC3 I2 (UNMET) — o 409 não informa a sub existente → retorne a subscription existente no payload de conflito.

**Harness (garantir implementação):**
5. AC5 I3 / e2e (UNMET) — 500 cru + sem e2e de retry idempotente → mapeie para um erro de domínio claro e adicione um e2e de retry (duas vezes ⇒ sem duplicata).
6. AC4 e2e (UNMET) — sem asserção de acesso → e2e batendo em `GET /subscription/user/:id/active` provando que `Trialing` concede acesso.
7. AC2 e2e (UNMET) — asserte que o padrão 14 é propagado ao Stripe quando `trialDays` é omitido.
