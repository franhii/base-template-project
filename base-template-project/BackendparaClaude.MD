# BACKEND ARCHITECTURE - COMPREHENSIVE TECHNICAL ANALYSIS

## 🏗️ SYSTEM ARCHITECTURE OVERVIEW

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Java Version**: 21
- **Build Tool**: Gradle 8.13 / Maven (dual support)
- **Database**: PostgreSQL (with JSONB support)
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **Payments**: MercadoPago SDK
- **Documentation**: OpenAPI/Swagger
- **Object Mapping**: MapStruct
- **Code Generation**: Lombok

---

## 📊 DATABASE SCHEMA & ENTITY MODEL

### Core Entities Hierarchy

#### 1. **TENANT** (Multi-tenancy Root)
```java
@Entity tenants
- id: UUID
- subdomain: String (unique) // "tienda1", "gimnasio2"
- businessName: String
- type: Enum(GYM, RETAIL, RESTAURANT, etc.)
- config: JSONB(TenantConfig) // Dynamic configuration
- active: boolean // Suspension control
- createdAt: timestamp
```

**TenantConfig Structure** (stored as JSONB):
```json
{
  "branding": {
    "logo": "url",
    "primaryColor": "#667eea",
    "secondaryColor": "#764ba2"
  },
  "businessInfo": {
    "postalCode": "2600", // Origin for shipping
    "contactEmail": "...",
    "contactPhone": "..."
  },
  "features": {
    "products": true,
    "services": true,
    "cart": true,
    "checkout": true,
    "delivery": false,
    "booking": false,
    "mercadoPago": true,
    "cashPayment": true
  },
  "categories": ["Electrónica", "Ropa", ...]
}
```

#### 2. **USER** (Multi-role Authentication)
```java
@Entity users
- id: UUID
- tenant_id: FK -> tenants
- name: String
- email: String (unique)
- password: String (bcrypt)
- phone: String
- role: Enum(SUPER_ADMIN, ADMIN, VENDEDOR, CLIENTE)
- createdAt/updatedAt: timestamps
```

**Role Hierarchy**:
- `SUPER_ADMIN`: Platform-wide access, manages ALL tenants
- `ADMIN`: Full control within own tenant
- `VENDEDOR`: Manage products/services/orders within tenant
- `CLIENTE`: Browse, purchase, book services

#### 3. **ITEM** (Polymorphic Base Class)
```java
@Entity items (JOINED inheritance)
- id: UUID
- tenant_id: FK
- name: String
- description: String
- price: BigDecimal
- category: String
- imageUrl: String
- active: boolean
- createdAt/updatedAt
```

**Subclasses**:

**3.1 PRODUCT** (extends Item)
```java
@Entity products
- stock: Integer
- sku: String
- weight: Double
- type: Enum(PHYSICAL, DIGITAL)
```

**3.2 SERVICE** (extends Item)
```java
@Entity services
- durationMinutes: Integer
- scheduleType: Enum(ON_DEMAND, SCHEDULED, RECURRING)
- maxCapacity: Integer
- requiresBooking: boolean
- availableDays: Set<DayOfWeek> (ElementCollection)
- workStartTime: LocalTime
- workEndTime: LocalTime
- slotIntervalMinutes: Integer
```

#### 4. **ORDER** (Transaction Core)
```java
@Entity orders
- id: UUID
- user_id: FK
- tenant_id: FK
- items: List<OrderItem> (CASCADE ALL)
- total: BigDecimal
- status: Enum(PENDING, CONFIRMED, PREPARING, READY, COMPLETED, CANCELLED)
- paymentMethod: Enum(CASH, MERCADO_PAGO, BANK_TRANSFER, etc.)
- notes: String

// Delivery fields
- isDelivery: boolean
- deliveryAddress_id: FK -> addresses
- deliveryCost: BigDecimal
- deliveryNotes: String
- shippingMethodId: Long // MercadoEnvíos method ID
- shipmentId: String // Tracking ID

- createdAt/updatedAt
```

#### 5. **ORDER_ITEM** (Order Line Items)
```java
@Entity order_items
- id: UUID
- order_id: FK
- item_id: FK -> items
- quantity: Integer
- priceAtPurchase: BigDecimal // Historical price snapshot
- itemName: String // Preserved name (in case item deleted)
- itemType: String // "PRODUCT" or "SERVICE"

// For services with booking
- bookingDate: LocalDate
- bookingTime: LocalTime
```

#### 6. **PAYMENT**
```java
@Entity payments
- id: UUID
- order_id: FK (OneToOne)
- tenant_id: FK
- method: Enum(CASH, MERCADO_PAGO, BANK_TRANSFER, etc.)
- status: Enum(PENDING, PROCESSING, APPROVED, REJECTED, CANCELLED, REFUNDED)
- amount: BigDecimal

// MercadoPago integration
- externalId: String // MP payment ID
- externalStatus: String // MP status
- paymentLink: String // MP checkout URL

// Manual payment (bank transfer)
- receiptUrl: String // Image URL
- receiptNotes: String

- createdAt/updatedAt
- confirmedAt: timestamp
```

#### 7. **BOOKING** (Service Reservations)
```java
@Entity bookings
- id: UUID
- service_id: FK -> services
- order_id: FK (nullable until confirmed)
- orderItem_id: FK
- user_id: FK
- tenant_id: FK
- bookingDate: LocalDate
- startTime: LocalTime
- endTime: LocalTime
- status: Enum(PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW)
- customerName/Email/Phone: String
- notes: String
- createdAt/updatedAt
```

#### 8. **ADDRESS** (Normalized with GeoRef API)
```java
@Entity addresses
- id: UUID
- user_id: FK
- tenant_id: FK
- street: String
- streetNumber: String

// Normalized data from Argentina's GeoRef API
- provinceId: String // Official ID (e.g., "82")
- provinceName: String // "Santa Fe"
- municipalityId: String
- municipalityName: String // "Venado Tuerto"
- localityId: String
- localityName: String
- postalCode: String (4 digits)

- apartment: String
- reference: String
- latitude/longitude: Double
- isDefault: boolean
- createdAt/updatedAt
```

---

## 🔐 SECURITY ARCHITECTURE

### JWT Authentication Flow

1. **Login** (`POST /api/auth/login`)
   ```java
   AuthenticationManager.authenticate(email, password)
   → JwtUtil.generateToken(email, claims)
   → claims: {userId, role}
   → Returns: {token: "eyJhbGc..."}
   ```

2. **Request Authentication**
   ```java
   JwtAuthenticationFilter (OncePerRequestFilter)
   → Extract token from "Authorization: Bearer <token>"
   → Validate token signature & expiration
   → Load UserDetails from database
   → Set SecurityContext.authentication
   ```

3. **Authorization**
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
   ```

### Security Configuration
```java
// CORS: Allows localhost:5173 (Vite), 3000 (CRA), 4200 (Angular)
// CSRF: Disabled (stateless JWT)
// Session: STATELESS
// Public endpoints: /api/auth/**, /api/config/current, /swagger-ui/**
// Protected: Everything else requires authentication
```

---

## 🏢 MULTI-TENANCY IMPLEMENTATION

### Tenant Isolation Strategy: **Discriminator Column**

Every entity has `tenant_id` FK → Enforced at application level

### Tenant Resolution Flow

1. **Request arrives** → `TenantInterceptor.preHandle()`
2. **Extract subdomain**:
   ```java
   // Priority 1: HTTP Header (for development/API)
   String subdomain = request.getHeader("X-Tenant-Subdomain");
   
   // Priority 2: Domain parsing
   if (host == "tienda1.miplatforma.com") 
     subdomain = "tienda1"
   
   // Priority 3: Default fallback
   if (null) subdomain = "default"
   ```

3. **Load Tenant** from DB
4. **Check active status** → Return 403 if suspended
5. **Store in ThreadLocal**:
   ```java
   TenantContext.setCurrentTenant(tenantId)
   ```

6. **Business Logic** → All queries filtered by `tenant_id`
7. **Cleanup** → `TenantInterceptor.afterCompletion()`

### Tenant-Aware Repositories
```java
List<Product> findByActiveTrueAndTenant(Tenant tenant)
List<Order> findByTenantOrderByCreatedAtDesc(Tenant tenant)
```

---

## 💳 PAYMENT PROCESSING SYSTEM

### MercadoPago Integration

#### Payment Creation Flow
```java
1. Order created → status = PENDING
2. POST /api/payments
   {orderId, method: "MERCADO_PAGO"}

3. PaymentService.createPayment()
   → Create Payment entity (status=PENDING)
   → Call createMercadoPagoPreference()
     → Build PreferenceRequest with order items
     → Set backUrls (success, failure, pending)
     → Set notificationUrl (webhook)
     → Set externalReference = orderId
     → MercadoPagoClient.create()
   → Store: externalId, paymentLink
   → Return payment with checkout URL

4. Frontend redirects to payment.paymentLink
5. Customer completes payment on MP
6. MP redirects to success/failure URL
7. MP sends webhook to notificationUrl
```

#### Webhook Processing (Critical)
```java
POST /api/payments/webhook/mercadopago

1. Receive webhook: {type: "payment", data: {id: "123456"}}
2. Call MP API to get full payment details
   GET https://api.mercadopago.com/v1/payments/123456
3. Extract: status, external_reference (orderId)
4. Find local Payment by externalReference
5. Update payment status based on MP status:
   - "approved" → APPROVED, order → CONFIRMED
   - "rejected"/"cancelled" → REJECTED, restore stock
   - "in_process" → PROCESSING
6. If approved: Confirm associated bookings
7. Save all changes in transaction
```

### Manual Payment Flow (Bank Transfer)
```java
1. POST /api/payments {method: "BANK_TRANSFER"}
   → Payment created, status=PENDING

2. Customer uploads receipt
   POST /api/payments/upload-receipt
   {paymentId, receiptUrl, notes}
   → Payment updated with receipt info

3. Admin reviews receipt
   POST /api/payments/{id}/approve (or /reject)
   → If approved: status=APPROVED, order=CONFIRMED
   → If rejected: status=REJECTED, stock restored
```

---

## 🗓️ BOOKING SYSTEM (Services with Appointments)

### Slot Generation Algorithm
```java
getAvailableSlots(serviceId, date):
  1. Validate service.requiresBooking = true
  2. Check date.dayOfWeek in service.availableDays
  3. Generate all possible slots:
     currentTime = workStartTime
     while (currentTime + duration <= workEndTime):
       slots.add({
         startTime: currentTime,
         endTime: currentTime + duration
       })
       currentTime += slotIntervalMinutes
  4. Fetch existing bookings for date
  5. For each slot:
     count = bookings overlapping this slot
     if (count < maxCapacity):
       availableSlots.add(slot)
  6. Return availableSlots
```

### Booking Lifecycle
```
PENDING → order created, payment pending
  ↓ payment approved
CONFIRMED → booking confirmed, customer notified
  ↓ service completed
COMPLETED → past service, marked as done
  OR
CANCELLED → customer/admin cancelled
  OR
NO_SHOW → customer didn't attend
```

### Booking + Payment Integration
```java
OrderService.createOrder():
  1. Create Order with OrderItems
  2. For each service item with booking:
     Create Booking (status=PENDING)
     Link: booking.order = order, booking.orderItem = item
  3. Payment confirmed → BookingService.confirmBooking()
  4. Payment rejected → BookingService.cancelBooking()
```

---

## 🚚 DELIVERY SYSTEM (MercadoEnvíos)

### Shipping Quote Flow
```java
POST /api/shipping/quote-by-postalcode
{postalCode: "2600", orderTotal: 15000}

1. Get tenant from context
2. Extract origin postal code from tenant.config
3. Call MercadoEnvíos API:
   POST /shipments/calculate
   {
     zipCodeFrom: "2600",
     zipCodeTo: "1234",
     dimensions: "30x20x10,1000", // cm, grams
     listCost: 15000,
     freeShipping: false
   }
4. Parse response → List<ShippingOption>:
   [{
     shippingMethodId: 100009,
     name: "Estándar",
     cost: 1500,
     estimatedDeliveryDays: "5-7 días"
   }, ...]
5. Return options to frontend
```

### Checkout with Delivery
```java
CreateOrderRequest:
  - items: [...]
  - isDelivery: true
  - deliveryAddressId: "address-uuid"
  - shippingMethodId: 100009 // Customer selected

OrderService.createOrder():
  1. Validate address belongs to user
  2. Get tenant origin postal code
  3. Quote shipping with selected method
  4. Add delivery cost to order total
  5. Save: order.deliveryCost, order.shippingMethodId
  6. Order status = PREPARING (until payment)
```

---

## 📍 ADDRESS NORMALIZATION (GeoRef API)

### Argentina's Official Geographic API

```java
GET https://apis.datos.gob.ar/georef/api/provincias
→ [{id: "82", nombre: "Santa Fe"}, ...]

GET /municipios?provincia=82
→ [{id: "822077", nombre: "Venado Tuerto", ...}, ...]

GET /localidades?municipio=822077
→ [{id: "82077010000", nombre: "Venado Tuerto", ...}, ...]
```

### Address Creation with Validation
```java
AddressService.createAddress():
  1. Validate postalCode format (4 digits)
  2. Call GeoRefService.isValidProvinceId()
  3. Store normalized IDs + human-readable names
  4. If first address → mark as default
  5. Else if isDefault=true → remove default from others
```

### Why GeoRef?
- ✅ Official government data
- ✅ Standardized province/city IDs
- ✅ Supports shipping calculations
- ✅ Autocomplete for municipalities
- ✅ Postal code validation

---

## 🔄 STOCK MANAGEMENT

### Stock Lifecycle

#### Order Creation
```java
OrderService.createOrder():
  1. VALIDATE stock BEFORE creating order
     for each product:
       if (product.stock < requestedQty):
         throw "Stock insuficiente"
  2. CREATE order
  3. DECREMENT stock atomically:
     product.stock -= quantity
     productRepository.save()
  4. Log: "Stock actualizado: {name} - Restante: {stock}"
```

#### Payment Rejection/Cancellation
```java
OrderService.restoreStock():
  1. For each item in order:
     if (itemType == "PRODUCT"):
       product.stock += quantity
       save()
  2. Log: "Stock restaurado: {name} - Nuevo: {stock}"

Called by:
- PaymentService.rejectPayment()
- PaymentService.cancelPayment()
- OrderService.cancelOrder()
- Webhook: MP status = rejected/cancelled
```

### Race Condition Prevention
```java
@Transactional // ACID guarantees
- Validation + creation + stock update = atomic
- Optimistic/Pessimistic locking at DB level
```

---

## 🧪 DATA SEEDING (Development)

```java
DataSeeder.initDatabase():
  1. Create "default" tenant if not exists
     - subdomain: "default"
     - businessName: "Default Business"
     - type: RETAIL
     - features: products, services, cart, checkout, MP
  
  2. Create admin user
     - email: admin@admin.com
     - password: admin123 (bcrypt)
     - role: ADMIN
     - tenant: default
  
  3. Create super admin
     - email: superadmin@platform.com
     - password: superadmin123
     - role: SUPER_ADMIN
     - tenant: default
```

---

## 📡 API ENDPOINTS MATRIX

### Public (No Auth)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/login` | JWT authentication |
| GET | `/api/items/products` | List products (public catalog) |
| GET | `/api/items/services` | List services |
| GET | `/api/config/current` | Get tenant config |
| POST | `/api/payments/webhook/mercadopago` | MP webhook |
| GET | `/api/georef/**` | Address autocomplete |

### Authenticated (All Roles)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/me` | Current user info |
| POST | `/api/orders` | Create order (checkout) |
| GET | `/api/orders/my-orders` | My order history |
| POST | `/api/payments` | Create payment |
| GET | `/api/addresses` | My addresses |
| POST | `/api/addresses` | Add address |
| GET | `/api/bookings/my-bookings` | My reservations |
| GET | `/api/customer/**` | Customer profile/stats |

### ADMIN + VENDEDOR
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/items/products` | Create/edit products |
| POST | `/api/items/services` | Create/edit services |
| GET | `/api/orders` | All tenant orders |
| PATCH | `/api/orders/{id}/status` | Update order status |
| GET | `/api/payments/pending-list` | Payments to review |
| PATCH | `/api/payments/{id}/approve` | Approve payment |
| GET | `/api/admin/stats/**` | Dashboard stats |
| GET | `/api/booking-management/**` | Manage bookings |

### SUPER_ADMIN (Platform)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/super-admin/tenants` | List ALL tenants |
| POST | `/api/super-admin/tenants` | Create new tenant |
| PATCH | `/api/super-admin/tenants/{id}/toggle-status` | Suspend tenant |
| POST | `/api/super-admin/services` | Create service for any tenant |
| DELETE | `/api/super-admin/tenants/{id}` | Delete tenant |

---

## 🛡️ ERROR HANDLING

### Global Exception Handler
```java
@RestControllerAdvice
- ResourceNotFoundException → 404
- UnauthorizedException → 401
- BadRequestException → 400
- MethodArgumentNotValidException → 400 (validation)
- Exception → 500

Response format:
{
  "timestamp": "2024-11-15T...",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found",
  "path": "/api/products/123"
}
```

---

## 🔍 KEY ARCHITECTURAL PATTERNS

### 1. **Repository Pattern**
- JpaRepository for data access
- Custom queries with `@Query`
- Method naming conventions

### 2. **Service Layer**
- Business logic encapsulation
- Transaction management with `@Transactional`
- Cross-cutting concerns

### 3. **DTO Pattern**
- Request/Response separation
- MapStruct for entity ↔ DTO conversion
- Validation with `@Valid`

### 4. **Inheritance (JPA)**
- `Item` → `Product` / `ServiceItem`
- Strategy: JOINED tables
- Polymorphic queries

### 5. **Composition over Inheritance**
- `TenantConfig` as JSONB
- Flexible feature flags
- No rigid class hierarchies

### 6. **Observer Pattern**
- Payment webhooks
- Event-driven status updates

---

## 🚀 DEPLOYMENT CONSIDERATIONS

### Environment Variables (Railway/Production)
```bash
# Database (Railway auto-provides)
PGHOST=containers-us-west-x.railway.app
PGPORT=5432
PGDATABASE=railway
PGUSER=postgres
PGPASSWORD=xxx

# JWT
APP_JWT_SECRET=your-256-bit-secret
APP_JWT_EXP_MS=86400000

# MercadoPago
MERCADOPAGO_ACCESS_TOKEN=APP_USR-xxx
MERCADOPAGO_PUBLIC_KEY=APP_USR-xxx
MERCADOPAGO_WEBHOOK_SECRET=xxx
MERCADOPAGO_WEBHOOK_URL=https://your-domain.com/api/payments/webhook/mercadopago

# Server
PORT=8080
```

### Critical Runtime Requirements
1. ✅ PostgreSQL 12+ with JSONB support
2. ✅ Java 21 JVM
3. ✅ HTTPS for production (MP webhooks require it)
4. ✅ Ngrok/tunnel for local MP testing
5. ✅ CORS configured for your frontend domain

---

## 🎯 BUSINESS LOGIC HIGHLIGHTS

### Order + Payment Atomicity
```java
@Transactional ensures:
1. Order created
2. Stock decremented
3. Bookings created (if services)
→ ALL OR NOTHING (rollback on error)
```

### Multi-Tenant Data Isolation
```java
Every query filtered by tenant_id
No cross-tenant data leakage possible
Tenant suspension → immediate 403
```

### Payment State Machine
```
PENDING → PROCESSING → APPROVED ✅
         ↘ REJECTED ❌
         ↘ CANCELLED ❌
APPROVED → REFUNDED (future)
```

### Booking Conflict Prevention
```java
findConflictingBookings(serviceId, date, startTime, endTime)
→ Check overlaps with PENDING + CONFIRMED bookings
→ Enforce maxCapacity per slot
```

---

## 📊 PERFORMANCE OPTIMIZATIONS

1. **Lazy Loading** on relationships (FetchType.LAZY)
2. **Indexed columns**: email, subdomain, tenant_id
3. **JSONB** for flexible config (no joins)
4. **ThreadLocal** for tenant context (no repeated queries)
5. **Projection queries** where full entities not needed

---

## 🔐 SECURITY BEST PRACTICES IMPLEMENTED

✅ Passwords: BCrypt hashing  
✅ JWT: HS256 signed tokens with expiration  
✅ CORS: Restricted origins  
✅ CSRF: Disabled (stateless)  
✅ SQL Injection: Protected (JPA/Hibernate)  
✅ Authorization: Role-based with `@PreAuthorize`  
✅ Tenant Isolation: Enforced at interceptor level  
✅ Input Validation: `@Valid` + custom validators  

---

## 📝 CRITICAL NOTES

### Stock Race Conditions
**MUST TEST**: High-concurrency order creation  
**Solution**: Database-level locking or optimistic concurrency

### MP Webhook Idempotency
**IMPLEMENTED**: Duplicate webhook detection via `externalStatus` check  
**LOG**: "⚠️ Webhook duplicado ignorado"

### Tenant Suspension
**IMMEDIATE**: Active check on EVERY request  
**USER IMPACT**: 403 error with message

### Address Validation
**DEPENDS ON**: GeoRef API availability  
**FALLBACK**: Manual entry if API fails

---

END OF DEEP TECHNICAL ANALYSIS