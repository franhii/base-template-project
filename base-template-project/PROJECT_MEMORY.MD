# PROJECT_MEMORY.md
> **PROPÓSITO**: Contexto técnico completo para Claude. Actualizar al final de cada sesión.
> **ÚLTIMA ACTUALIZACIÓN**: 2025-11-12

---

## 🎯 PROYECTO: Core E-Commerce Multi-Tenant

### Stack
**Backend**: Spring Boot 3.2.0 + Java 21 + PostgreSQL + Maven
**Frontend**: React 18 + Vite + Axios + Context API
**Integraciones**: MercadoPago SDK 2.1.26 + GeoRef API + MercadoEnvíos

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
  String postalCode; // CP del negocio (para MercadoEnvíos)
  Features features; // productos, servicios, booking, mercadoPago, mercadoEnvios
  BookingConfig bookingConfig;
  SocialMedia socialMedia;
  List<String> categories;
}
```

### Address (NUEVO - Sistema de Direcciones)
```java
@Entity class Address {
  String id;
  User user; // ManyToOne
  Tenant tenant; // ManyToOne
  
  // Datos normalizados con GeoRef API
  String street, streetNumber;
  String provinceId, provinceName;       // GeoRef oficial
  String municipalityId, municipalityName; // GeoRef oficial
  String localityId, localityName;       // GeoRef oficial (opcional)
  String postalCode;                     // 4 dígitos Argentina
  
  String apartment, reference;           // Opcionales
  Double latitude, longitude;            // Para cálculo de distancia
  boolean isDefault;                     // Dirección por defecto
  LocalDateTime createdAt, updatedAt;
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

### Order (ACTUALIZADO - Con Shipping)
```java
@Entity class Order {
  String id;
  User user;
  Tenant tenant;
  List<OrderItem> items; // @OneToMany
  BigDecimal total; // Incluye deliveryCost
  OrderStatus status; // PENDING, CONFIRMED, PREPARING, COMPLETED, CANCELLED
  PaymentMethod paymentMethod;
  String notes;
  
  // ========== SHIPPING (NUEVO) ==========
  Address deliveryAddress; // @ManyToOne (null si es pickup)
  BigDecimal deliveryCost; // Costo del envío (de MercadoEnvíos)
  boolean isDelivery; // true = delivery, false = pickup
  String deliveryNotes; // Notas del cliente
  Long shippingMethodId; // ID de MercadoEnvíos (100009, 100012, etc.)
  String shipmentId; // ID del shipment en ML (para tracking)
  String shippingStatus; // pending, ready_to_ship, shipped, delivered
  
  LocalDateTime createdAt, updatedAt;
  
  // Método helper
  BigDecimal getTotalWithDelivery(); // total + deliveryCost
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
// CLIENTE: Compras, reservas, perfil, direcciones
```

### SecurityConfig
- **CORS**: Habilitado para localhost:5173, incluye X-Tenant-Subdomain
- **CSRF**: Disabled (JWT stateless)
- **Public endpoints**: /api/auth/**, /api/config/current, /api/items/** (GET), /api/payments/** (webhooks), /api/georef/**

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

### Addresses (/api/addresses) - NUEVO
- `POST /` → Crear dirección (con GeoRef)
- `GET /` → Listar mis direcciones
- `GET /default` → Obtener dirección por defecto
- `GET /{id}` → Obtener dirección específica
- `PUT /{id}` → Actualizar dirección
- `PATCH /{id}/set-default` → Marcar como default
- `DELETE /{id}` → Eliminar dirección

### GeoRef (/api/georef) - NUEVO (Público)
- `GET /provinces` → Todas las provincias argentinas
- `GET /municipalities?provinceId=X` → Municipios por provincia
- `GET /municipalities/search?query=X` → Buscar municipios (autocomplete)
- `GET /localities?municipalityId=X` → Localidades por municipio

### Shipping (/api/shipping) - NUEVO (MercadoEnvíos)
- `POST /quote` → Cotizar opciones de envío
   - Body: `{ addressId, orderTotal, dimensions? }`
   - Retorna opciones de MercadoEnvíos con precios
- `GET /available/{addressId}` → Validar disponibilidad de envío

### Orders (/api/orders)
- `POST /` → Crea orden + reserva stock + crea bookings + calcula envío
   - Validación automática de shipping si isDelivery=true
   - Integración con MercadoEnvíos
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
CheckoutPage → Selector método pago + dirección + envío → Crea orden + pago → Redirige a MP o success
SuccessPage / FailurePage / PendingPage → Resultados MP
AdminPage → Dashboard (Recharts) + Pending payments table
ManageItemsPage → CRUD productos/servicios (tabs) + booking config
MyAccountPage → Tabs: Overview (stats), Órdenes, Reservas, Perfil, Direcciones (NUEVO)
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
AddressManager → CRUD direcciones (pendiente frontend)
AddressForm → Formulario con GeoRef autocomplete (pendiente frontend)
ShippingSelector → Selector de opciones de envío (pendiente frontend)
```

### Servicios API
```javascript
// api.js: axios instance con JWT interceptor + tenant header
// authService: register, login, getCurrentUser
// itemService: getProducts, getServices, createProduct, createService
// tenantService: getCurrentConfig
// addressService: CRUD direcciones (pendiente)
// shippingService: quoteShipping (pendiente)
// setTenantHeader(subdomain) → Para testing multi-tenant
```

---

## 🔄 FLUJOS CRÍTICOS

### Flujo de Compra con Envío (Producto Físico)
1. Cliente agrega al carrito → Valida stock
2. Checkout → Selecciona/crea dirección
3. Frontend llama `/api/shipping/quote` → Muestra opciones de MercadoEnvíos
4. Cliente elige método de envío (estándar, express, etc.)
5. Checkout → Selecciona método pago + notas
6. Crea orden → Valida dirección + método envío + Descuenta stock
7. OrderService calcula costo con MercadoEnvíos
8. Crea payment → Genera preferencia MP
9. Redirige a MP → Usuario paga
10. Webhook MP → Actualiza payment + order a CONFIRMED
11. Actualiza shippingStatus a "ready_to_ship"
12. Success page → Limpia carrito

### Flujo de Reserva (Servicio con Booking)
1. Cliente ve servicio → Click "Reservar"
2. BookingModal → Selecciona fecha + slot disponible
3. Agrega al carrito con bookingDate + bookingTime
4. Checkout → Elige pickup o delivery
5. Si delivery → Selecciona dirección + método envío
6. Crea orden + OrderItem con booking data
7. OrderService → Crea Booking (status PENDING)
8. Pago confirmado → PaymentService.confirmOrderBookings() → Booking a CONFIRMED

### Flujo de Cancelación
**Orden**:
1. Cliente/Admin cancela orden
2. OrderService.cancelOrder() → Restaura stock de productos físicos
3. Order.status = CANCELLED
4. Si tiene envío, shippingStatus = "cancelled"

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

### Direcciones & GeoRef (NUEVO - Sesión 2025-11-12)
- [x] Modelo Address con campos normalizados
- [x] CRUD completo de direcciones
- [x] Integración con GeoRef API (Argentina)
- [x] Autocompletar provincias, municipios, localidades
- [x] Validación de código postal (4 dígitos)
- [x] Gestión de dirección por defecto automática
- [x] Coordenadas GPS opcionales

### MercadoEnvíos (NUEVO - Sesión 2025-11-12)
- [x] Integración completa con API de MercadoEnvíos
- [x] Cotización de envío en tiempo real
- [x] Múltiples opciones (estándar, express, sucursal)
- [x] Cálculo automático de costos
- [x] Tiempo estimado de entrega
- [x] Validación de disponibilidad por CP
- [x] Integración en OrderService
- [x] Campos de shipping en Order
- [x] Estados de envío (pending, ready_to_ship, etc.)

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

1. **Frontend - Sistema de Direcciones**
   - [ ] AddressManager component en Mi Cuenta
   - [ ] AddressForm con dropdowns de GeoRef
   - [ ] Autocompletar provincias → municipios → localidades
   - [ ] Selector de dirección en Checkout
   - [ ] Validación de campos obligatorios

2. **Frontend - Sistema de Envío**
   - [ ] ShippingSelector component
   - [ ] Llamada a `/api/shipping/quote` en Checkout
   - [ ] Mostrar opciones de MercadoEnvíos
   - [ ] Destacar envío gratis (si aplica)
   - [ ] Mostrar tiempo estimado de entrega
   - [ ] Toggle Delivery / Pickup en local

3. **Testing completo flujo MercadoEnvíos**
   - [ ] Testear cotización con diferentes CPs
   - [ ] Verificar opciones de envío
   - [ ] Validar creación de orden con shipping
   - [ ] Probar con envío gratis

### 📋 Prioridad Media

4. **Tracking de Envíos**
   - [ ] Crear shipment en MercadoEnvíos después del pago
   - [ ] Guardar shipmentId en Order
   - [ ] Endpoint GET /api/orders/{id}/tracking
   - [ ] Frontend: Mostrar estado de envío

5. **WhatsApp Floating Button**
   - [ ] Botón flotante esquina inferior derecha
   - [ ] Número configurable en TenantConfig
   - [ ] Link directo wa.me

6. **Google OAuth Login**
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

### ¿Por qué GeoRef API en lugar de campos libres?
- API oficial del gobierno argentino (gratis)
- Datos normalizados y actualizados
- IDs oficiales de provincias/municipios
- Mejor experiencia de usuario (autocomplete)
- Preparado para validaciones de CP

### ¿Por qué MercadoEnvíos en lugar de Andreani/OCA directo?
- **No requiere contrato corporativo** (solo API key)
- Mismas credenciales que MercadoPago
- Mejores tarifas (economías de escala)
- Tracking incluido
- Múltiples couriers en uno (Correo Argentino + Andreani)
- Puntos de retiro en todo el país
- Confianza del usuario

### ¿Por qué separar street y streetNumber?
- Requerimiento de couriers (necesitan separados)
- Mejor búsqueda y filtrado
- Validaciones más precisas
- Compatibilidad con APIs de mapas

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
   - https://claude.ai/chat/[SESIÓN_ACTUAL_2025-11-12]
- **Repo Git**: https://github.com/franhii/base-template-project
- **Docs MP**: https://www.mercadopago.com.ar/developers/es/docs
- **GeoRef API**: https://apis.datos.gob.ar/georef/api/
- **MercadoEnvíos API**: https://developers.mercadolibre.com.ar/es_ar/envios

---

## 📊 MÉTRICAS DEL PROYECTO

- **Backend**: ~85 archivos Java (+15 esta sesión)
- **Frontend**: ~40 archivos JSX/CSS
- **Total LOC**: ~18,000 (estimado)
- **Endpoints**: ~60 (+10 esta sesión)
- **Entidades**: 11 principales (+1 Address)
- **Tiempo desarrollo**: ~5-6 sesiones

---

## 🎯 PRÓXIMA SESIÓN

**Objetivo**: Frontend de Direcciones y Envíos
**Archivos necesarios**:
- MyAccountPage.jsx (agregar tab Direcciones)
- AddressManager.jsx (nuevo)
- AddressForm.jsx (nuevo)
- CheckoutPage.jsx (agregar selector envío)
- ShippingSelector.jsx (nuevo)

**Resultado esperado**:
- CRUD de direcciones funcional en frontend
- Autocompletar con GeoRef en formulario
- Cotización de envío en tiempo real
- Selector de opciones de MercadoEnvíos
- Integración completa checkout → envío → pago

---

## 📖 GLOSARIO

- **Tenant**: Cliente/negocio que usa la plataforma
- **Item**: Abstracción de Product + ServiceItem
- **Booking**: Reserva de un servicio en fecha/hora específica
- **OrderItem**: Línea de una orden (puede incluir booking data)
- **Slot**: Horario disponible para reserva
- **External Reference**: ID de nuestra orden usado en MercadoPago
- **Snapshot**: Copia de datos al momento de la transacción (ej: priceAtPurchase)
- **GeoRef**: API del gobierno argentino para normalizar direcciones
- **MercadoEnvíos**: Sistema de logística de MercadoLibre
- **Shipping Method ID**: ID único de un método de envío (ej: 100009 = Estándar)
- **Shipment ID**: ID del envío en MercadoLibre para tracking

---

**🔄 ACTUALIZAR ESTE ARCHIVO AL FINAL DE CADA SESIÓN**