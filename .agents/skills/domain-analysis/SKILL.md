---
name: domain-analysis
description: Mapeia domínios de negócio e sugere boundaries de serviço em qualquer codebase usando DDD Strategic Design. Use ao perguntar "quais são os domínios nesta codebase?", "onde devo traçar os boundaries de serviço?", "identificar bounded contexts", "classificar subdomains", "análise de DDD", ou ao analisar coesão de domínio. NÃO use para agrupar componentes existentes em domínios (use domain-identification-grouping) nem para análise de dependências (use coupling-analysis).
---

# Identificação de Subdomains & Análise de Bounded Context

Esta skill analisa codebases para identificar subdomains (Core, Supporting, Generic) e sugerir bounded contexts seguindo os princípios de Strategic Design do Domain-Driven Design.

## Quando Usar

Aplique esta skill quando:

- Analisar boundaries de domínio em qualquer codebase
- Identificar subdomains Core, Supporting e Generic
- Mapear bounded contexts do espaço do problema para o espaço da solução
- Avaliar coesão de domínio e detectar problemas de acoplamento
- Planejar refatoração orientada a domínio
- Entender capacidades de negócio no código

## Princípios Centrais

### Classificação de Subdomain

**Core Domain**: vantagem competitiva, maior valor de negócio, exige os melhores desenvolvedores

- Indicadores: lógica de negócio complexa, mudanças frequentes, necessidade de domain experts

**Supporting Subdomain**: essencial mas não diferenciador, específico do negócio

- Indicadores: apoia o Core Domain, complexidade moderada, regras específicas do negócio

**Generic Subdomain**: funcionalidade comum, poderia ser terceirizada

- Indicadores: problema bem compreendido, baixa diferenciação, funcionalidade padrão

### Bounded Context

Um boundary linguístico explícito onde os termos do domínio têm significados específicos e inequívocos.

- Natureza primária: boundary linguístico, não técnico
- Regra chave: dentro do boundary, todos os termos da Ubiquitous Language são inequívocos
- Objetivo: alinhar 1 subdomain para 1 bounded context (ideal)

## Processo de Análise

### Fase 1: Extrair Conceitos

Varra a codebase em busca de conceitos de negócio (não de infraestrutura):

1. **Entities** (modelos de domínio com identidade)
   - Patterns: `@Entity`, `class`, modelos de domínio
   - Foco: conceitos de negócio, não classes técnicas

2. **Services** (operações de negócio)
   - Patterns: `*Service`, `*Manager`, `*Handler`
   - Foco: lógica de negócio, não utilitários técnicos

3. **Use Cases** (workflows de negócio)
   - Patterns: `*UseCase`, `*Command`, `*Handler`
   - Foco: processos de negócio, não CRUD

4. **Controllers/Resolvers** (pontos de entrada)
   - Patterns: `*Controller`, `*Resolver`, endpoints de API
   - Foco: capacidades de negócio, não rotas técnicas

### Fase 2: Agrupar por Ubiquitous Language

Para cada conceito, determine:

**Contexto Linguístico Primário**

- A qual vocabulário de negócio isso pertence?
- Exemplos:
  - `Subscription`, `Invoice`, `Payment` → linguagem de Billing
  - `Movie`, `Video`, `Episode` → linguagem de Content
  - `User`, `Authentication` → linguagem de Identity

**Boundaries Linguísticos**

- Onde o significado dos termos muda?
- Mesmo termo, significado diferente = bounded context diferente
- Exemplo: "Customer" em Sales vs "Customer" em Support

**Relacionamentos entre Conceitos**

- Quais conceitos pertencem naturalmente juntos?
- Quais compartilham vocabulário de negócio?
- Quais referenciam uns aos outros?

### Fase 3: Identificar Subdomains

Um subdomain tem:

- Capacidade de negócio distinta
- Valor de negócio independente
- Vocabulário único
- Múltiplas entities relacionadas trabalhando juntas
- Conjunto coeso de operações de negócio

**Patterns Comuns de Domínio**:

- Billing/Subscription: pagamentos, invoices, planos
- Content/Catalog: mídia, produtos, inventário
- Identity/Access: usuários, autenticação, autorização
- Analytics: métricas, dashboards, insights
- Notifications: mensagens, alertas, comunicações

**Classifique Cada Subdomain**:

Use esta árvore de decisão:

```
Is it a competitive advantage?
  YES → Core Domain
  NO → Does it require business-specific knowledge?
        YES → Supporting Subdomain
        NO → Generic Subdomain
```

### Fase 4: Avaliar Coesão

**Indicadores de Alta Coesão** ✅

- Conceitos compartilham a Ubiquitous Language
- Conceitos usados juntos com frequência
- Relacionamentos de negócio diretos
- Mudanças em um afetam os outros do grupo
- Resolvem o mesmo problema de negócio

**Indicadores de Baixa Coesão** ❌

- Vocabulários de negócio diferentes misturados
- Conceitos raramente usados juntos
- Sem relacionamento de negócio direto
- Mudanças não afetam os outros
- Resolvem problemas de negócio diferentes

**Fórmula da Pontuação de Coesão**:

```
Score = (
  Linguistic Cohesion (0-3) +    // Shared vocabulary
  Usage Cohesion (0-3) +         // Used together
  Data Cohesion (0-2) +          // Entity relationships
  Change Cohesion (0-2)          // Change together
) / 10

8-10: High Cohesion ✅
5-7:  Medium Cohesion ⚠️
0-4:  Low Cohesion ❌
```

### Fase 5: Detectar Problemas de Baixa Coesão

**Regra 1: Descompasso Linguístico**

- Problema: vocabulários de negócio diferentes misturados
- Exemplo: `User` (identity) + `Subscription` (billing) no mesmo service
- Ação: sugerir separação em bounded contexts diferentes

**Regra 2: Dependências Entre Domínios**

- Problema: acoplamento forte entre domínios
- Exemplo: o Service A instancia diretamente entities do Domínio B
- Ação: sugerir integração baseada em interface

**Regra 3: Responsabilidades Misturadas**

- Problema: uma única classe trata múltiplas preocupações de negócio
- Exemplo: service tratando billing e content ao mesmo tempo
- Ação: sugerir divisão por subdomain

**Regra 4: Generic dentro do Core**

- Problema: funcionalidade genérica dentro da lógica de negócio core
- Exemplo: envio de e-mail no service de billing
- Ação: extrair para um Generic Subdomain

**Regra 5: Boundaries Indefinidos**

- Problema: não dá para determinar a qual domínio o conceito pertence
- Exemplo: entity com relacionamentos para múltiplos domínios
- Ação: esclarecer boundaries, possivelmente dividir o conceito

### Fase 6: Mapear Bounded Contexts

Para cada subdomain identificado, sugira um bounded context:

**Características do Bounded Context**:

- O nome reflete a Ubiquitous Language
- Contém o modelo de domínio completo
- Tem pontos de integração explícitos
- Boundary linguístico claro

**Patterns de Integração**:

- Shared Kernel: modelo compartilhado entre contexts (use com parcimônia)
- Customer/Supplier: o downstream depende do upstream
- Conformist: o downstream se conforma ao upstream
- Anti-corruption Layer: camada de tradução entre contexts
- Open Host Service: interface publicada para integração
- Published Language: protocolo de integração bem documentado

## Formato de Saída

### Mapa de Domínio

Para cada domínio/subdomain:

```markdown
## Domain: {Name}

**Type**: Core Domain | Supporting Subdomain | Generic Subdomain

**Ubiquitous Language**: {key business terms}

**Business Capability**: {what business problem it solves}

**Key Concepts**:

- {Concept} (Entity|Service|UseCase) - {brief description}

**Subdomains** (if applicable):

1. {Subdomain} (Core|Supporting|Generic)
   - Concepts: {list}
   - Cohesion: {score}/10
   - Dependencies: → {other domains}

**Suggested Bounded Context**: {Name}Context

- Linguistic boundary: {where terms have specific meaning}
- Integration: {how it should integrate with other contexts}

**Dependencies**:

- → {OtherDomain} via {interface/API}
- ← {OtherDomain} via {interface/API}

**Cohesion Score**: {score}/10
```

### Matriz de Coesão

```markdown
## Cross-Domain Cohesion

| Domain A | Domain B | Cohesion | Issue              | Recommendation          |
| -------- | -------- | -------- | ------------------ | ----------------------- |
| Billing  | Identity | 2/10     | ❌ Direct coupling | Use interface           |
| Content  | Billing  | 6/10     | ⚠️ Usage tracking  | Event-based integration |
```

### Relatório de Baixa Coesão

```markdown
## Issues Detected

### Priority: High

**Issue**: {description}

- **Location**: {file/class/method}
- **Problem**: {what's wrong}
- **Concepts**: {involved concepts}
- **Cohesion**: {score}/10
- **Recommendation**: {suggested fix}

### Priority: Medium

{similar format}
```

### Mapa de Bounded Contexts

```markdown
## Suggested Bounded Contexts

### {ContextName}Context

**Contains Subdomains**:

- {Subdomain1} (Core)
- {Subdomain2} (Supporting)

**Ubiquitous Language**:

- Term: Definition in this context

**Integration Requirements**:

- Consumes from: {OtherContext} via {pattern}
- Publishes to: {OtherContext} via {pattern}

**Implementation Notes**:

- Separate persistence
- Independent deployment
- Explicit API boundaries
```

## Boas Práticas

### Faça ✅

- Foque na linguagem de negócio, não na estrutura de código
- Deixe a Ubiquitous Language guiar os boundaries
- Meça a coesão de forma objetiva
- Identifique pontos de integração claros
- Classifique todo subdomain (Core/Supporting/Generic)
- Procure primeiro pelos boundaries linguísticos

### Não Faça ❌

- Não agrupe por camadas técnicas
- Não force um único modelo global
- Não ignore diferenças linguísticas
- Não acople domínios diretamente
- Não crie contexts a partir da arquitetura
- Não elimine todas as dependências (algumas são necessárias)

## Checklist de Análise

**Para Cada Conceito**:

- [ ] A qual linguagem de negócio ele pertence?
- [ ] De qual domínio/subdomain ele faz parte?
- [ ] É Core, Supporting ou Generic?
- [ ] A quais outros conceitos ele se relaciona?
- [ ] As dependências estão dentro do mesmo domínio?
- [ ] Há algum descompasso linguístico?

**Para Cada Domínio**:

- [ ] Qual é a Ubiquitous Language?
- [ ] Quais são os conceitos chave?
- [ ] Quais são os subdomains?
- [ ] Qual é o Core Domain?
- [ ] Quais são as dependências entre domínios?
- [ ] A coesão interna é alta?
- [ ] Os boundaries estão claros?

**Para a Análise de Coesão**:

- [ ] Calcular as pontuações de coesão
- [ ] Identificar áreas de baixa coesão
- [ ] Mapear dependências entre domínios
- [ ] Sinalizar descompassos linguísticos
- [ ] Anotar acoplamentos fortes
- [ ] Sugerir esclarecimentos de boundary

## Referência Rápida

### Árvore de Decisão de Subdomain

```
Analyze business capability
└─ Is it competitive advantage?
   ├─ YES → Core Domain
   └─ NO → Is it business-specific?
      ├─ YES → Supporting Subdomain
      └─ NO → Generic Subdomain
```

### Checagem Rápida de Coesão

```
Same vocabulary? → High linguistic cohesion
Used together? → High usage cohesion
Direct relationships? → High data cohesion
Change together? → High change cohesion

All high → Strong subdomain candidate
Mix of high/low → Review boundaries
All low → Likely wrong grouping
```

### Sinais de Bounded Context

```
Clear boundary signs:
✅ Distinct Ubiquitous Language
✅ Concepts have unambiguous meaning
✅ Different meanings across contexts
✅ Clear integration points

Unclear boundary signs:
❌ Same terms with same meanings everywhere
❌ Concepts used identically across system
❌ No clear linguistic differences
❌ Tight coupling everywhere
```

## Anti-Patterns a Evitar

**Big Ball of Mud**

- Tudo conectado a tudo
- Sem boundaries claros
- Vocabulários misturados
- Prevenção: bounded contexts explícitos

**All-Inclusive Model**

- Modelo único para o negócio inteiro
- Definições globais impossíveis
- Cria conflitos
- Prevenção: abrace múltiplos contexts

**Conceitos Linguísticos Misturados**

- Vocabulários diferentes no mesmo context
- Exemplo: User/Permission junto com Forum/Post
- Prevenção: mantenha as associações linguísticas

## Notas

- Esta é uma análise estratégica, não implementação tática
- Foque em QUAIS domínios existem, não em COMO implementar
- Algumas dependências entre domínios são normais
- Baixa coesão nem sempre significa "ruim", significa "precisa de atenção"
- Generic Subdomains naturalmente têm coesão menor
- Sempre valide com domain experts quando possível

## Critérios de Validação

Uma boa identificação de domínio tem:

- ✅ Boundaries claros com Ubiquitous Language distinta
- ✅ Alta coesão interna dentro dos domínios
- ✅ Dependências entre domínios explícitas
- ✅ Alinhamento de negócio com as capacidades
- ✅ Recomendações acionáveis para os problemas
