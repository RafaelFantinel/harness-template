# Exemplos de Identificação de Subdomains

Este documento traz exemplos práticos de aplicação da identificação de subdomains em tipos diferentes de codebase.

## Exemplo 1: Plataforma de E-Commerce

### Conceitos Descobertos

**Entities**:
- Product, Category, Inventory, SKU
- Order, OrderItem, Cart, CartItem
- Customer, Address, PaymentMethod
- Shipment, Tracking, Warehouse

**Services**:
- ProductCatalogService, InventoryService
- OrderProcessingService, CartService
- PaymentService, ShippingService
- CustomerService

### Grupos de Linguagem

**Linguagem de Catalog**: product, category, SKU, inventory, stock
**Linguagem de Order**: order, cart, checkout, fulfillment
**Linguagem de Payment**: payment, transaction, refund, charge
**Linguagem de Shipping**: shipment, tracking, delivery, carrier
**Linguagem de Customer**: customer, account, profile, address

### Subdomains Identificados

#### 1. Product Catalog (Core Domain)
- **Tipo**: Core (se a diferenciação for a descoberta de produtos)
- **Ubiquitous Language**: product, category, catalog, search, browse
- **Conceitos**: Product, Category, ProductCatalogService
- **Coesão**: 9/10
- **Bounded Context**: CatalogContext

#### 2. Inventory Management (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: stock, inventory, SKU, warehouse, allocation
- **Conceitos**: Inventory, SKU, InventoryService, Warehouse
- **Coesão**: 8/10
- **Bounded Context**: InventoryContext

#### 3. Order Processing (Core Domain)
- **Tipo**: Core (se a diferenciação for a experiência de checkout)
- **Ubiquitous Language**: order, cart, checkout, fulfillment
- **Conceitos**: Order, Cart, OrderProcessingService
- **Coesão**: 9/10
- **Bounded Context**: OrderContext

#### 4. Payment Processing (Generic)
- **Tipo**: Generic (gateway de pagamento padrão)
- **Ubiquitous Language**: payment, transaction, charge, refund
- **Conceitos**: Payment, PaymentService, PaymentGateway
- **Coesão**: 7/10
- **Bounded Context**: PaymentContext

#### 5. Shipping (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: shipment, tracking, delivery, carrier
- **Conceitos**: Shipment, Tracking, ShippingService
- **Coesão**: 8/10
- **Bounded Context**: ShippingContext

### Análise de Coesão

| Domain A | Domain B | Coesão | Relacionamento |
|----------|----------|--------|----------------|
| Catalog | Inventory | 6/10 | ⚠️ Checagem de disponibilidade de produto |
| Order | Catalog | 5/10 | ⚠️ Referência de produto no pedido |
| Order | Payment | 7/10 | ✅ Pedido dispara pagamento |
| Order | Shipping | 7/10 | ✅ Pedido dispara envio |
| Customer | Order | 3/10 | ❌ Referência direta de entity |

### Problemas de Baixa Coesão

**Problema 1**: entity Customer referenciada diretamente em Order
- **Problema**: contexts diferentes (Identity vs Order)
- **Recomendação**: usar value object CustomerId, não referência de entity
- **Pattern**: Published Language

**Problema 2**: o service de Catalog consulta o Inventory diretamente
- **Problema**: Core Domain depende de um Supporting
- **Recomendação**: usar atualizações de inventário baseadas em eventos
- **Pattern**: Domain Events

---

## Exemplo 2: Sistema de Saúde

### Conceitos Descobertos

**Entities**:
- Patient, MedicalRecord, Diagnosis
- Appointment, Schedule, Availability
- Prescription, Medication, Dosage
- Doctor, Nurse, Staff
- Billing, Claim, Insurance

### Grupos de Linguagem

**Linguagem Clínica**: patient, diagnosis, treatment, medical record
**Linguagem de Scheduling**: appointment, schedule, availability, slot
**Linguagem de Pharmacy**: prescription, medication, dosage, drug
**Linguagem de Staff**: doctor, nurse, practitioner, credential
**Linguagem de Billing**: claim, insurance, copay, reimbursement

### Subdomains Identificados

#### 1. Patient Care (Core Domain)
- **Tipo**: Core
- **Ubiquitous Language**: patient, diagnosis, treatment, care plan
- **Conceitos**: Patient, MedicalRecord, Diagnosis, CareService
- **Coesão**: 9/10
- **Bounded Context**: ClinicalContext

#### 2. Appointment Management (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: appointment, schedule, availability, booking
- **Conceitos**: Appointment, Schedule, SchedulingService
- **Coesão**: 8/10
- **Bounded Context**: SchedulingContext

#### 3. Pharmacy (Supporting)
- **Tipo**: Supporting (ou Core se a farmácia for o diferencial)
- **Ubiquitous Language**: prescription, medication, dosage, drug interaction
- **Conceitos**: Prescription, Medication, PharmacyService
- **Coesão**: 8/10
- **Bounded Context**: PharmacyContext

#### 4. Medical Billing (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: claim, insurance, billing, reimbursement
- **Conceitos**: Claim, Insurance, BillingService
- **Coesão**: 7/10
- **Bounded Context**: BillingContext

### Insight Chave

**O conceito "Patient" tem significados diferentes**:

| Context | Significado de Patient | Propriedades |
|---------|------------------------|--------------|
| Clinical | Sujeito médico | Diagnóstico, sinais vitais, alergias |
| Scheduling | Titular do agendamento | Disponibilidade, preferências |
| Billing | Pagador/beneficiário | Seguro, saldo, claims |

→ Esses são bounded contexts diferentes, apesar de compartilharem o termo "Patient"

---

## Exemplo 3: Ferramenta SaaS de Gestão de Projetos

### Conceitos Descobertos

**Entities**:
- Project, Task, Milestone, Sprint
- User, Team, Role, Permission
- Comment, Attachment, Activity
- Subscription, Plan, Invoice
- Notification, Alert

### Grupos de Linguagem

**Linguagem de Project**: project, task, milestone, sprint, backlog
**Linguagem de Collaboration**: comment, discussion, mention, activity
**Linguagem de Access**: user, team, role, permission, access
**Linguagem de Billing**: subscription, plan, invoice, payment
**Linguagem de Notification**: notification, alert, reminder

### Subdomains Identificados

#### 1. Project Management (Core Domain)
- **Tipo**: Core
- **Ubiquitous Language**: project, task, milestone, workflow
- **Conceitos**: Project, Task, Milestone, ProjectService
- **Coesão**: 9/10
- **Bounded Context**: ProjectContext

#### 2. Collaboration (Core/Supporting)
- **Tipo**: Core se for diferencial, Supporting caso contrário
- **Ubiquitous Language**: comment, discussion, activity, collaboration
- **Conceitos**: Comment, Activity, CollaborationService
- **Coesão**: 8/10
- **Bounded Context**: CollaborationContext

#### 3. Identity & Access (Generic)
- **Tipo**: Generic
- **Ubiquitous Language**: user, authentication, authorization, role
- **Conceitos**: User, Role, Permission, AuthService
- **Coesão**: 9/10
- **Bounded Context**: IdentityContext

#### 4. Billing (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: subscription, plan, invoice, billing
- **Conceitos**: Subscription, Invoice, BillingService
- **Coesão**: 8/10
- **Bounded Context**: BillingContext

#### 5. Notifications (Generic)
- **Tipo**: Generic
- **Ubiquitous Language**: notification, alert, message, reminder
- **Conceitos**: Notification, NotificationService
- **Coesão**: 7/10
- **Bounded Context**: NotificationContext

### Problema de Baixa Coesão

**Problema**: entity User usada em todo lugar
```typescript
// Project domain
class Project {
  owner: User;        // ❌ Direct reference
  members: User[];    // ❌ Direct reference
}

// Collaboration domain
class Comment {
  author: User;       // ❌ Direct reference
}

// Billing domain
class Subscription {
  subscriber: User;   // ❌ Direct reference
}
```

**Problema**: conceito do context de Identity vazou para todos os domínios

**Recomendação**: usar conceitos específicos de cada context
```typescript
// Project domain
class Project {
  ownerId: OwnerId;           // ✅ Value object
  members: MemberId[];        // ✅ Value object
}

// Collaboration domain
class Comment {
  authorId: ParticipantId;    // ✅ Context-specific
}

// Billing domain
class Subscription {
  subscriberId: CustomerId;   // ✅ Context-specific
}
```

---

## Exemplo 4: Plataforma de Streaming de Vídeo

### Conceitos Descobertos

**Entities**:
- Movie, TVShow, Episode, Season
- Video, Stream, Encoding, Quality
- Watchlist, Viewing, Progress
- Recommendation, Preference
- Subscription, Plan, Billing

### Grupos de Linguagem

**Linguagem de Content**: movie, show, episode, season, catalog
**Linguagem de Streaming**: video, stream, encoding, bitrate, quality
**Linguagem de Engagement**: watchlist, viewing, progress, rating
**Linguagem de Recommendation**: recommendation, preference, algorithm
**Linguagem de Billing**: subscription, plan, billing, payment

### Subdomains Identificados

#### 1. Content Catalog (Supporting)
- **Tipo**: Supporting (a menos que conteúdo exclusivo seja o diferencial)
- **Ubiquitous Language**: movie, show, episode, catalog, metadata
- **Conceitos**: Movie, TVShow, Episode, CatalogService
- **Coesão**: 9/10
- **Bounded Context**: CatalogContext

#### 2. Video Streaming (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: video, stream, encoding, playback, quality
- **Conceitos**: Video, Stream, StreamingService
- **Coesão**: 8/10
- **Bounded Context**: StreamingContext

#### 3. User Engagement (Supporting)
- **Tipo**: Supporting
- **Ubiquitous Language**: watchlist, viewing, progress, rating
- **Conceitos**: Watchlist, Viewing, EngagementService
- **Coesão**: 8/10
- **Bounded Context**: EngagementContext

#### 4. Recommendation Engine (Core Domain)
- **Tipo**: Core (se o algoritmo for vantagem competitiva)
- **Ubiquitous Language**: recommendation, preference, algorithm, personalization
- **Conceitos**: Recommendation, RecommendationEngine
- **Coesão**: 9/10
- **Bounded Context**: RecommendationContext

#### 5. Video Processing (Generic)
- **Tipo**: Generic
- **Ubiquitous Language**: transcoding, encoding, compression
- **Conceitos**: VideoProcessor, EncodingService
- **Coesão**: 7/10
- **Bounded Context**: ProcessingContext

### Pattern de Integração

```
Catalog Context → publishes → ContentPublished event
                              ↓
                   Recommendation Context ← consumes
                              ↓
Engagement Context → publishes → UserWatched event
                              ↓
                   Recommendation Context ← consumes
```

**Pattern**: Event-Driven Architecture com Domain Events

---

## Patterns Comuns Entre os Exemplos

### Pattern 1: Vazamento de Identity

**Problema**: entities de User/Identity usadas diretamente em todo lugar

**Solução**: identificadores específicos de cada context
- Context de Project: OwnerId, MemberId
- Context de Billing: CustomerId, SubscriberId
- Context de Content: CreatorId, ViewerId

### Pattern 2: Uso Excessivo de Shared Kernel

**Problema**: modelos compartilhados grandes usados em todo lugar

**Solução**: shared kernel mínimo, majoritariamente value objects
- Compartilhe: UserId (como string/UUID), Email (como value object)
- Não compartilhe: entity User, entity Customer

### Pattern 3: Confusão entre Core e Supporting

**Pergunta chave**: "Isto é nossa vantagem competitiva?"
- Se SIM → Core Domain (melhor time, mais atenção)
- Se NÃO, mas específico do negócio → Supporting
- Se NÃO e padrão de mercado → Generic

### Pattern 4: Tamanho do Bounded Context

**Pequeno demais**:
```
OrderContext
OrderItemContext        ❌ Gaping holes
OrderStatusContext      ❌ Fragmented
```

**Tamanho certo**:
```
OrderContext            ✅ Complete language
├── Order
├── OrderItem
└── OrderStatus
```

**Grande demais**:
```
SalesContext            ❌ Mixed concerns
├── Order
├── Product
├── Customer
└── Invoice
```

### Pattern 5: Tipos de Integração

**Síncrona** (use com parcimônia):
- Quando consistência imediata é necessária
- Exemplo: Order → Payment (precisa de resposta imediata)

**Assíncrona** (prefira):
- Quando consistência eventual é aceitável
- Exemplo: Order → Shipping (pode ser adiada)

**Orientada a Eventos** (melhor para desacoplamento):
- Quando múltiplos contexts precisam reagir
- Exemplo: OrderPlaced → [Billing, Shipping, Analytics]

---

## Template de Análise Rápida

Use este template ao analisar qualquer codebase:

```markdown
## Codebase: {Name}

### Step 1: Concepts Extracted
- Entities: [list]
- Services: [list]
- Use Cases: [list]
- Controllers: [list]

### Step 2: Language Groups
- Group 1: {name} - terms: [list]
- Group 2: {name} - terms: [list]

### Step 3: Subdomains Identified
1. {Subdomain} (Core/Supporting/Generic)
   - Language: [terms]
   - Concepts: [list]
   - Cohesion: X/10
   - Bounded Context: {Name}Context

### Step 4: Cohesion Matrix
| Domain A | Domain B | Cohesion | Issue |
|----------|----------|----------|-------|
| ... | ... | X/10 | ... |

### Step 5: Issues Found
- Priority High: [list]
- Priority Medium: [list]
- Priority Low: [list]

### Step 6: Recommendations
1. [recommendation]
2. [recommendation]
```
