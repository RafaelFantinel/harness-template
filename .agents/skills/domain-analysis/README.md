# Skill de Identificação de Subdomains

Uma Agent Skill para identificar subdomains e sugerir bounded contexts em qualquer codebase seguindo os princípios de Strategic Design do Domain-Driven Design (DDD).

## O Que Esta Skill Faz

Esta skill analisa codebases para:

1. **Extrair conceitos de negócio** do código (entities, services, use cases, controllers)
2. **Agrupar conceitos pela Ubiquitous Language** (vocabulário de negócio)
3. **Identificar subdomains** e classificá-los como Core, Supporting ou Generic
4. **Avaliar coesão** dentro e entre domínios
5. **Detectar problemas de baixa coesão** e problemas de acoplamento
6. **Sugerir bounded contexts** com boundaries linguísticos claros
7. **Fornecer recomendações acionáveis** para separação de domínios

## Quando o Agente Usa Esta Skill

O agente aplica esta skill automaticamente quando você:

- Pede para analisar boundaries de domínio
- Solicita identificação de subdomains
- Precisa de ajuda com DDD strategic design
- Quer avaliar coesão de domínio
- Pergunta sobre bounded contexts
- Discute refatoração orientada a domínio
- Questiona sobre capacidades de negócio no código

## Principais Recursos

### Genérica & Portátil

Esta skill foi projetada para funcionar com **qualquer codebase** em qualquer linguagem:

- Sem premissas específicas de framework
- Princípios agnósticos de linguagem
- Foca em conceitos de negócio, não em implementação técnica
- Consegue analisar monólitos, microserviços ou arquiteturas híbridas

### Fundamento em DDD Strategic Design

Baseada em princípios comprovados de Domain-Driven Design:

- **Espaço do Problema**: identifica subdomains (Core, Supporting, Generic)
- **Espaço da Solução**: sugere bounded contexts com boundaries claros
- **Ubiquitous Language**: principal driver para detecção de boundaries
- **Análise de Coesão**: métricas objetivas para relacionamentos de domínio

### Saída Acionável

Fornece análise concreta e acionável:

- Mapas de domínio com pontuações de coesão
- Matrizes de coesão entre domínios
- Relatórios de problemas de baixa coesão com prioridades
- Sugestões de bounded context com patterns de integração
- Recomendações claras de melhoria

## Arquivos Incluídos

### SKILL.md (Skill Principal)

O arquivo principal da skill, contendo:

- Processo completo de análise (6 fases)
- Regras de classificação de subdomain
- Framework de avaliação de coesão
- Regras de detecção de baixa coesão
- Templates de formato de saída
- Boas práticas e anti-patterns

### EXAMPLES.md (Exemplos Práticos)

Exemplos do mundo real em domínios diferentes:

- Plataforma de E-Commerce
- Sistema de Saúde
- Ferramenta SaaS de Gestão de Projetos
- Plataforma de Streaming de Vídeo
- Patterns e soluções comuns
- Template de análise rápida

### QUICK-REFERENCE.md (Consulta Rápida)

Referência rápida para cenários comuns:

- Árvores de decisão para classificação
- Atalhos de pontuação de coesão
- Sinais de alerta
- Guia de patterns de integração
- Erros comuns a evitar
- Perguntas chave para avaliação

## Exemplos de Uso

### Exemplo 1: Analisar a Codebase Inteira

```
User: "Analyze the domains in this codebase and suggest bounded contexts"

Agent: [Uses skill to:]
1. Extract all business concepts
2. Group by Ubiquitous Language
3. Identify subdomains
4. Calculate cohesion scores
5. Detect issues
6. Suggest bounded contexts
```

### Exemplo 2: Checar um Módulo Específico

```
User: "Is the billing module properly separated from other domains?"

Agent: [Uses skill to:]
1. Analyze billing module concepts
2. Check cross-domain dependencies
3. Assess linguistic cohesion
4. Flag coupling issues
5. Recommend improvements
```

### Exemplo 3: Classificar um Subdomain

```
User: "Should our recommendation engine be Core or Supporting?"

Agent: [Uses skill to:]
1. Ask: Is it competitive advantage?
2. Assess business differentiation
3. Check complexity & change frequency
4. Classify using decision tree
5. Explain classification
```

## Conceitos Centrais

### Tipos de Subdomain

**Core Domain**

- Sua vantagem competitiva
- O que torna seu negócio único
- Exige os melhores desenvolvedores e domain experts
- Exemplo: o algoritmo de recomendação da Netflix

**Supporting Subdomain**

- Essencial mas não diferenciador
- Específico do negócio, mas não único
- Apoia o Core Domain
- Exemplo: regras customizadas de gestão de inventário

**Generic Subdomain**

- Funcionalidade comum
- Poderia ser terceirizada ou comprada
- Soluções bem compreendidas
- Exemplo: autenticação de usuário, envio de e-mail

### Pontuação de Coesão

A skill usa uma escala de coesão de 10 pontos:

```
Score = Linguistic (0-3) + Usage (0-3) + Data (0-2) + Change (0-2)

8-10: High Cohesion ✅ (Strong subdomain candidate)
5-7:  Medium Cohesion ⚠️ (Review boundaries)
0-4:  Low Cohesion ❌ (Wrong grouping, needs separation)
```

### Bounded Context

Um boundary linguístico explícito onde todos os termos de domínio têm significados específicos e inequívocos:

- Driver principal: **linguagem de negócio**, não arquitetura técnica
- Objetivo: alinhar 1 Subdomain para 1 Bounded Context
- Integração: use interfaces, eventos ou APIs entre contexts
- Tamanho: tão grande quanto necessário para expressar a Ubiquitous Language completa

## Princípios Chave

1. **Linguagem Acima da Arquitetura**: bounded contexts são boundaries linguísticos, não técnicos
2. **Negócio Acima do Técnico**: foque em capacidades de negócio, não na estrutura de código
3. **Coesão é Mensurável**: use métricas objetivas, não intuição
4. **O Contexto Manda**: o mesmo termo pode significar coisas diferentes em contexts diferentes
5. **Integração é Necessária**: algumas dependências entre domínios são normais e saudáveis

## Anti-Patterns Detectados

A skill identifica erros comuns:

- **Big Ball of Mud**: tudo conectado a tudo
- **All-Inclusive Model**: tentar criar um único modelo global
- **Conceitos Linguísticos Misturados**: vocabulários diferentes no mesmo context
- **Acoplamento Forte Entre Domínios**: referências diretas de entity entre domínios
- **Generic dentro do Core**: preocupações de infraestrutura na lógica de negócio
- **Boundaries Indefinidos**: não dá para determinar qual domínio é dono do conceito

## Patterns de Integração

A skill sugere os patterns de integração apropriados:

- **Domain Events**: para desacoplamento e consistência eventual
- **API/Interface**: para integração síncrona com contrato claro
- **Anti-Corruption Layer**: para proteger de sistemas externos
- **Published Language**: para integração estável e documentada
- **Customer/Supplier**: para relações claras de upstream/downstream

## Instalação

Esta skill é instalada no nível de projeto, no diretório de skills do seu agente:

```
.{agent}/skills/subdomain-identification/
```

Onde `{agent}` é o diretório do seu agente (ex.: `.cursor/`, `.claude/`, `.agent/`, `.github/`, `.opencode/`).

Isso significa que ela é:

- **Compartilhada com o repositório**: quem clonar este repo recebe a skill
- **Versionada**: as mudanças são rastreadas no git
- **Específica do projeto**: pode ser customizada para esta codebase

O agente vai descobrir e usar a skill automaticamente quando apropriado, com base na description do frontmatter.

## Customização

### Para Domínios Específicos do Projeto

Se seu projeto tem patterns de domínio específicos, crie uma referência no nível do projeto:

```
.{agent}/skills/subdomain-identification/
└── project-domains.md  # Document project-specific patterns
```

Referencie esse arquivo nas suas solicitações de análise.

### Para Análise Específica de Framework

Adicione patterns específicos do framework para ajudar a skill:

```markdown
## Framework: NestJS

**Entity Pattern**: `@Entity()` decorator
**Service Pattern**: `@Injectable()` classes ending in `Service`
**Controller Pattern**: `@Controller()` decorator
**Use Case Pattern**: Classes ending in `UseCase`
```

## Validação

Para verificar se a skill funciona corretamente, tente:

```
User: "What subdomains can you identify in this codebase?"
```

O agente deve:

1. Ler o arquivo SKILL.md
2. Seguir o processo de análise de 6 fases
3. Produzir mapas de domínio e matrizes de coesão
4. Fornecer recomendações acionáveis

## Referências

Esta skill é baseada em:

- **Domain-Driven Design**, de Eric Evans
- **Implementing Domain-Driven Design**, de Vaughn Vernon
- Princípios de Strategic Design da comunidade de DDD

## Licença

Esta skill pode ser usada, modificada e compartilhada livremente. Foi projetada para ser portátil entre qualquer codebase ou organização.

## Contribuindo

Para melhorar esta skill:

1. Adicione mais exemplos em `EXAMPLES.md`
2. Expanda a referência rápida com novos patterns
3. Adicione patterns de detecção específicos de linguagem/framework
4. Documente novos anti-patterns ou sinais de alerta
5. Compartilhe estudos de caso do mundo real

## Versão

**Versão**: 1.0.0  
**Criada em**: 2026-02-05  
**Baseada em**: teoria de DDD Strategic Design

---

## Início Rápido

Para usar esta skill imediatamente:

```
User: "Analyze domains in my codebase"
User: "Identify subdomains and suggest bounded contexts"
User: "Check cohesion between [DomainA] and [DomainB]"
User: "Is [concept] Core, Supporting, or Generic?"
```

O agente vai aplicar esta skill automaticamente e fornecer uma análise abrangente.
