# PROJECT_MEMORY.md
> **PROPÓSITO**: Contexto técnico completo para Claude. Actualizar al final de cada sesión.
> **ÚLTIMA ACTUALIZACIÓN**: 2025-11-10

---

## 🎯 PROYECTO: Core E-Commerce Multi-Tenant

### Stack
**Backend**: Spring Boot 3.2.0 + Java 21 + PostgreSQL + Maven
**Frontend**: React 18 + Vite + Axios + Context API
**Integraciones**: MercadoPago SDK 2.1.26

### Arquitectura Multi-Tenant
- **Estrategia**: Subdomain-based (beauty.app.com, gym.app.com)
- **Aislamiento**: Shared DB + Tenant discriminator en todas las entidades
- **Detección**: TenantInterceptor → TenantContext (ThreadLocal)
- **Desarrollo local**: Header `X-Tenant-Subdomain` para testing

---

## 📊 MODELO DE DATOS (Core Entities)

### User
```java
@Entity class User {
  String id, name, email, phone, password;
  Role role; // SUPER_ADMIN, ADMIN, VENDEDOR, CLIENTE
  Tenant tenant; // ManyToOne
  LocalDateTime createdAt, updatedAt;
}
```

### Tenant
```java
@Entity class Tenant {
  String id, subdomain, businessName;
  BusinessType type; // GYM, RETAIL, BEAUTY_SALON, etc.
  boolean active; // ✅ Suspensión de tenants
  TenantConfig config; // JSONB
  LocalDateTime createdAt;
}
```

### TenantConfig (JSONB)
```java
class TenantConfig {
  String primaryColor, secondaryColor, logo, favicon;
  Features features; // productos, servicios, booking, mercadoPago, etc.
  DeliveryConfig deliveryConfig; // freeDeliveryThreshold, deliveryCost, zones
  BookingConfig bookingConfig;
  SocialMedia socialMedia;
  List<String> categories;
}
```

### Item (Herencia JOINED)
```java
@Entity @Inheritance(JOINED)
abstract class Item {
  String id, name, description, imageUrl, category;
  BigDecimal price;
  Tenant tenant;
  boolean active;
}

@Entity class Product extends Item {
  Integer stock;
  String sku;
  ProductType type; // PHYSICAL, DIGITAL
}

@Entity class ServiceItem extends Item {
  Integer durationMinutes, maxCapacity;
  ScheduleType scheduleType; // ON_DEMAND, SCHEDULED, RECURRING
  boolean requiresBooking;
  Set<DayOfWeek> availableDays;
  LocalTime workStartTime, workEndTime;
  Integer slotIntervalMinutes;
}
```

### Order
```java
@Entity class Order {
  String id;
  User user;
  Tenant tenant;
  List<OrderItem> items; // @OneToMany
  BigDecimal total;
  OrderStatus status; // PENDING, CONFIRMED, PREPARING, COMPLETED, CANCELLED
  PaymentMethod paymentMethod;
  String notes;
  LocalDateTime createdAt, updatedAt;
}

@Entity class OrderItem {
  String id;
  Order order;
  Item item;
  Integer quantity;
  BigDecimal priceAtPurchase;
  String itemName, itemType; // Snapshot para items eliminados
  // Booking fields (solo si es servicio):
  LocalDate bookingDate;
  LocalTime bookingTime;
}
```

### Booking
```java
@Entity class Booking {
  String id;
  ServiceItem service;
  Order order;
  OrderItem orderItem;
  Tenant tenant;
  User user;
  LocalDate bookingDate;
  LocalTime startTime, endTime;
  BookingStatus status; // PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW
  String customerName, customerEmail, customerPhone, notes;
}
```

### Payment
```java
@Entity class Payment {
  String id;
  Order order; // @OneToOne
  Tenant tenant;
  PaymentMethod method; // CASH, MERCADO_PAGO, BANK_TRANSFER, etc.
  PaymentStatus status; // PENDING, APPROVED, REJECTED, CANCELLED
  BigDecimal amount;
  String externalId, externalStatus, paymentLink;
  String receiptUrl, receiptNotes; // Para comprobantes manuales
  LocalDateTime createdAt, confirmedAt;
}
```

---

## 🔐 SEGURIDAD & AUTENTICACIÓN

### JWT
- **Secret**: APP_JWT_SECRET (env)
- **Expiration**: APP_JWT_EXP_MS (env, default 24h)
- **Claims**: email (subject), userId, role
- **Filter**: JwtAuthenticationFilter → SecurityContext

### Roles & Permisos
```java
// SUPER_ADMIN: Gestiona toda la plataforma (/api/super-admin/**)
// ADMIN: Gestiona su tenant (/api/admin/**)
// VENDEDOR: CRUD items de su tenant
// CLIENTE: Compras, reservas, perfil
```

### SecurityConfig
- **CORS**: Habilitado para localhost:5173, incluye X-Tenant-Subdomain
- **CSRF**: Disabled (JWT stateless)
- **Public endpoints**: /api/auth/**, /api/config/current, /api/items/** (GET), /api/payments/** (webhooks)

---

## 🌐 BACKEND: ENDPOINTS PRINCIPALES

### Auth (/api/auth)
- `POST /register` → Crea User + rol CLIENTE + tenant default
- `POST /login` → Retorna JWT
- `GET /me` → User actual (requiere auth)

### Items (/api/items)
- `GET /products` → Lista productos del tenant actual
- `GET /services` → Lista servicios del tenant actual
- `POST /products` → Crea producto (ADMIN/VENDEDOR)
- `POST /services` → Crea servicio (ADMIN/VENDEDOR)

### Orders (/api/orders)
- `POST /` → Crea orden + reserva stock + crea bookings si hay servicios
- `GET /my-orders` → Órdenes del user
- `GET /` → Todas las órdenes del tenant (ADMIN/VENDEDOR)
- `POST /{id}/cancel` → Cancela orden + restaura stock

### Payments (/api/payments)
- `POST /` → Crea pago (MP o manual)
- `POST /upload-receipt` → Sube comprobante (transferencias)
- `PATCH /{id}/approve` → Aprueba pago manual (ADMIN/VENDEDOR)
- `PATCH /{id}/reject` → Rechaza pago + restaura stock
- `POST /webhook/mercadopago` → Webhook de MP (público)
- `GET /success, /failure, /pending` → Redirects de MP

### Bookings (/api/bookings)
- `GET /available?serviceId=X&date=Y` → Slots disponibles
- `POST /` → Crea booking (status PENDING hasta pago)
- `POST /{id}/cancel` → Cancela booking (24hs antes mínimo)
- `GET /my-bookings` → Bookings del user

### Admin (/api/admin)
- `GET /users` → Todos los usuarios
- `POST /users` → Crea usuario con tenant específico
- `PATCH /users/{id}/role` → Cambia rol

### Admin Stats (/api/admin/stats)
- `GET /dashboard` → KPIs (ventas, órdenes, clientes, revenue)
- `GET /daily-sales?days=30` → Ventas diarias (chart)
- `GET /pending-payments` → Pagos pendientes revisión
- `GET /top-products?limit=10` → Productos más vendidos
- `GET /payment-methods` → Distribución métodos de pago
- `GET /order-status` → Distribución estados órdenes

### Customer (/api/customer)
- `GET /profile` → Perfil del user
- `PUT /profile` → Actualiza perfil (nombre, teléfono)
- `GET /orders` → Mis órdenes
- `GET /bookings` → Mis reservas
- `GET /bookings/upcoming` → Próximas reservas
- `POST /bookings/{id}/cancel` → Cancela reserva (24hs antes)
- `GET /stats` → Estadísticas del cliente

### Super Admin (/api/super-admin)
- `GET /tenants` → TODOS los tenants (sin filtro)
- `POST /tenants` → Crea tenant
- `PUT /tenants/{id}` → Actualiza tenant
- `PATCH /tenants/{id}/toggle-status` → Activa/Suspende tenant
- `PATCH /tenants/{id}/features` → Actualiza features del tenant
- `POST /services` → Crea servicio para un tenant específico

### Config (/api/config)
- `GET /current` → Configuración del tenant actual (público)

---

## 🎨 FRONTEND: ESTRUCTURA

### Context & State
```javascript
// CartContext: cart, addToCart, removeFromCart, updateQuantity, clearCart, getTotal, getTotalItems
// Persistencia en localStorage
// Validación de stock en addToCart/updateQuantity
```

### Páginas
```
HomePage → Lista productos/servicios (tabs)
LoginPage / RegisterPage
CartPage → Lista items + summary + validaciones stock
CheckoutPage → Selector método pago + notas → Crea orden + pago → Redirige a MP o success
SuccessPage / FailurePage / PendingPage → Resultados MP
AdminPage → Dashboard (Recharts) + Pending payments table
ManageItemsPage → CRUD productos/servicios (tabs) + booking config
MyAccountPage → Tabs: Overview (stats), Órdenes, Reservas, Perfil
SuperAdminPage → Tabla tenants + Edit/Suspend + Crear servicio
NotFoundPage
```

### Componentes
```
Navbar → Logo, links, carrito badge, user dropdown, roles
ProductCard / ServiceCard → Imagen, precio, stock, agregar carrito
BookingModal → Selector fecha + slots disponibles
Toast → Notificaciones (success, error, warning)
ConfirmModal → Confirmaciones (cancelar booking, vaciar carrito, etc.)
EditTenantModal → Editar tenant (nombre, tipo, features)
AdminDashboard → KPIs + charts (Recharts)
```

### Servicios API
```javascript
// api.js: axios instance con JWT interceptor + tenant header
// authService: register, login, getCurrentUser
// itemService: getProducts, getServices, createProduct, createService
// tenantService: getCurrentConfig
// setTenantHeader(subdomain) → Para testing multi-tenant
```

---

## 🔄 FLUJOS CRÍTICOS

### Flujo de Compra (Producto Físico)
1. Cliente agrega al carrito → Valida stock
2. Checkout → Selecciona método pago + notas
3. Crea orden → Descuenta stock (OrderService)
4. Crea payment → Genera preferencia MP (PaymentService)
5. Redirige a MP → Usuario paga
6. Webhook MP → Actualiza payment + order a CONFIRMED
7. Success page → Limpia carrito

### Flujo de Reserva (Servicio con Booking)
1. Cliente ve servicio → Click "Reservar"
2. BookingModal → Selecciona fecha + slot disponible
3. Agrega al carrito con bookingDate + bookingTime
4. Checkout → Crea orden + OrderItem con booking data
5. OrderService → Crea Booking (status PENDING)
6. Pago confirmado → PaymentService.confirmOrderBookings() → Booking a CONFIRMED

### Flujo de Cancelación
**Orden**:
1. Cliente/Admin cancela orden
2. OrderService.cancelOrder() → Restaura stock de productos físicos
3. Order.status = CANCELLED

**Booking**:
1. Cliente cancela reserva (validación 24hs antes)
2. BookingService.cancelBooking() → Booking.status = CANCELLED
3. Si hay orden asociada, considerar cancelar orden completa

**Pago Rechazado (MP o Manual)**:
1. Webhook MP status=rejected O Admin rechaza manual
2. PaymentService → Payment.status = REJECTED
3. OrderService.restoreStock() → Devuelve stock
4. Order.status = CANCELLED
5. BookingService.cancelOrderBookings() → Cancela reservas

### Multi-Tenant Testing Local
```javascript
// frontend/src/App.jsx (en desarrollo)
api.defaults.headers.common['X-Tenant-Subdomain'] = 'beauty-test';

// O modificar /etc/hosts:
// 127.0.0.1 beauty-test.localhost
```

---

## 🚀 DEPLOYMENT & ENV VARS

### Backend (.env.local)
```properties
SPRING_PROFILES_ACTIVE=local
PGHOST=localhost
PGPORT=5432
PGDATABASE=coredb
PGUSER=postgres
PGPASSWORD=admin
APP_JWT_SECRET=desarrollo-local-32-caracteres-minimo-muy-seguro
APP_JWT_EXP_MS=86400000
PORT=8080
MERCADOPAGO_ACCESS_TOKEN=xxx
MERCADOPAGO_PUBLIC_KEY=xxx
MERCADOPAGO_WEBHOOK_SECRET=xxx
MERCADOPAGO_WEBHOOK_URL=https://xxx.ngrok.io/api/payments/webhook/mercadopago
MERCADOPAGO_SUCCESS_URL=http://localhost:5173/success
MERCADOPAGO_FAILURE_URL=http://localhost:5173/failure
MERCADOPAGO_PENDING_URL=http://localhost:5173/pending
```

### Frontend
```bash
VITE_API_URL=http://localhost:8080
```

### Usuarios de Testing
```
Super Admin: superadmin@platform.com / superadmin123
Admin: admin@admin.com / admin123
```

---

## ✅ FEATURES COMPLETADAS

### Core
- [x] Sistema multi-tenant completo
- [x] Autenticación JWT + roles
- [x] CRUD productos/servicios
- [x] Sistema de carrito (Context + localStorage)
- [x] Checkout + creación órdenes
- [x] Gestión de stock (descuento + restauración)

### Booking
- [x] Servicios con reserva
- [x] Disponibilidad configurable (días, horarios, slots)
- [x] Modal de selección de turnos
- [x] Validación de conflictos
- [x] Cancelación con restricción 24hs

### Payments
- [x] Integración MercadoPago (preferencias + webhooks)
- [x] Pagos manuales (transferencia + comprobante)
- [x] Aprobación/rechazo manual (admin)
- [x] Restauración de stock en fallos
- [x] Páginas de resultado (success/failure/pending)

### Admin
- [x] Dashboard con KPIs (Recharts)
- [x] Ventas diarias (chart)
- [x] Top productos
- [x] Pagos pendientes de revisión
- [x] Distribución métodos de pago
- [x] Gestión de usuarios

### Super Admin
- [x] Panel de gestión de tenants
- [x] Activar/Suspender tenants
- [x] Editar configuración de tenants
- [x] Crear servicios para tenants específicos

### Cliente
- [x] Panel "Mi Cuenta" (Overview, Órdenes, Reservas, Perfil)
- [x] Ver historial de compras
- [x] Ver reservas próximas/pasadas
- [x] Cancelar reservas (24hs)
- [x] Editar perfil (nombre, teléfono)

---

## 🔄 PENDIENTE (TODO.md - Priorizado)

### 🔥 Prioridad Alta (Siguiente Sesión)
1. **Sistema de Delivery** ← PROPUESTO POR FRANCIS
    - [ ] Modelo Address (calle, ciudad, CP, provincia)
    - [ ] CRUD direcciones (User → List<Address>)
    - [ ] DeliveryService: cálculo de envío
    - [ ] TenantConfig.DeliveryConfig: freeDeliveryThreshold, deliveryCost, zones
    - [ ] Endpoint /api/orders/calculate-delivery
    - [ ] Frontend: AddressManager en Mi Cuenta
    - [ ] Frontend: Selector dirección en Checkout
    - [ ] Checkout: Mostrar "Envío gratis" cuando aplique
    - [ ] Validación: Productos físicos requieren dirección

2. **Testing completo flujo MercadoPago**
    - [ ] Testear success/pending/failure en dev
    - [ ] Verificar restauración stock en todos los casos
    - [ ] Validar webhooks con ngrok

### 📋 Prioridad Media
3. **WhatsApp Floating Button**
    - [ ] Botón flotante esquina inferior derecha
    - [ ] Número configurable en TenantConfig
    - [ ] Link directo wa.me

4. **Google OAuth Login**
    - [ ] Login con Google
    - [ ] Auto-registro si no existe
    - [ ] Mantener JWT existente

### 🔧 Refactoring
- [ ] Agregar soft-delete
- [ ] Cache configuración tenants
- [ ] Índices BD para performance
- [ ] Rate limiting endpoints públicos
- [ ] Logging estructurado (SLF4J)

### 🐛 Bugs Conocidos
- [ ] Validar webhooks MP en producción
- [ ] Manejo de timezones en bookings
- [ ] Validar emails únicos por tenant

---

## 📝 DECISIONES TÉCNICAS IMPORTANTES

### ¿Por qué JOINED herencia para Items?
- Permite queries polimórficas eficientes
- Evita nullables en tabla base
- Fácil agregar tipos nuevos (ej: DigitalProduct)

### ¿Por qué ThreadLocal para TenantContext?
- Aislamiento automático por request
- No contamina parámetros de métodos
- Compatible con transacciones Spring

### ¿Por qué snapshot en OrderItem?
- Preserva datos si se elimina Item
- Auditabilidad (precio al momento de compra)
- Evita inconsistencias en reportes históricos

### ¿Por qué JSONB para TenantConfig?
- Flexibilidad para configs custom por tenant
- No requiere migraciones para agregar campos
- PostgreSQL indexa JSONB eficientemente

### ¿Por qué external_reference en MP en lugar de payment_id?
- MP puede reintentar pagos con mismo payment_id
- external_reference es nuestro Order.id (único)
- Evita duplicados y facilita tracking

---

## 🔗 LINKS IMPORTANTES

- **Chat anteriores**: 
- https://claude.ai/chat/f73e801d-c1ea-45ee-8a89-e517c03886ee
- https://claude.ai/chat/f052be26-377a-40ee-a877-08cfae28a7b3
- **Repo Git**: https://github.com/franhii/base-template-project
- **Docs MP**: https://www.mercadopago.com.ar/developers/es/docs

---

## 📊 MÉTRICAS DEL PROYECTO

- **Backend**: ~70 archivos Java
- **Frontend**: ~40 archivos JSX/CSS
- **Total LOC**: ~15,000 (estimado)
- **Endpoints**: ~50
- **Entidades**: 10 principales
- **Tiempo desarrollo**: ~4-5 sesiones

---

## 🎯 PRÓXIMA SESIÓN

**Objetivo**: Sistema de Delivery con envío gratis
**Archivos necesarios**:
- TenantConfig.java
- OrderService.java
- CheckoutPage.jsx
- MyAccountPage.jsx

**Resultado esperado**:
- Address model + CRUD
- Cálculo de envío en checkout
- Mensaje "Envío gratis" dinámico
- Validación productos físicos requieren dirección

---

## 📖 GLOSARIO

- **Tenant**: Cliente/negocio que usa la plataforma
- **Item**: Abstracción de Product + ServiceItem
- **Booking**: Reserva de un servicio en fecha/hora específica
- **OrderItem**: Línea de una orden (puede incluir booking data)
- **Slot**: Horario disponible para reserva
- **External Reference**: ID de nuestra orden usado en MercadoPago
- **Snapshot**: Copia de datos al momento de la transacción (ej: priceAtPurchase)

---

**🔄 ACTUALIZAR ESTE ARCHIVO AL FINAL DE CADA SESIÓN**


## ✅ FEATURES COMPLETADAS
- HACER EL PROJECT_MEMORY.MD

## 🔄 PENDIENTE
- [ ] Objetivo: Sistema de Delivery con envío gratis Archivos necesarios:

TenantConfig.java
OrderService.java
CheckoutPage.jsx
MyAccountPage.jsx
Resultado esperado:

Address model + CRUD
Cálculo de envío en checkout
Mensaje "Envío gratis" dinámico
Validación productos físicos requieren dirección


## 🎯 PRÓXIMA SESIÓN
Objetivo: LO PENDIENTE.

