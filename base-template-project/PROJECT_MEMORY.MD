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
---

## 🔄 PENDIENTE (TODO.md - Priorizado)

### 🔥 Prioridad Alta (Siguiente Sesión - 2025-11-13)

**OBJETIVO**: Completar frontend de Direcciones y Envío

1. **AddressForm.jsx** (NUEVO)
    - [ ] Modal/Form con campos de dirección
    - [ ] Integración con GeoRef API (provinces, municipalities, localities)
    - [ ] Autocompletado en cascada (Provincia → Municipio → Localidad)
    - [ ] Validación CP 4 dígitos
    - [ ] Checkbox "Marcar como default"
    - [ ] Modos: create/edit

2. **AddressManager.jsx** (NUEVO)
    - [ ] Listar direcciones del usuario
    - [ ] Card por dirección con badge "Default"
    - [ ] Botones: Editar, Eliminar, Set Default
    - [ ] Botón "Agregar Dirección" → Abre AddressForm
    - [ ] Empty state si no hay direcciones
    - [ ] ConfirmModal para eliminación

3. **ShippingSelector.jsx** (NUEVO)
    - [ ] Radio: "Retiro en local" / "Envío a domicilio"
    - [ ] Si delivery: Selector de dirección existente
    - [ ] Botón "Agregar nueva dirección"
    - [ ] POST /api/shipping/quote al seleccionar dirección
    - [ ] Mostrar opciones MercadoEnvíos con precios
    - [ ] Badge "GRATIS" si cost = 0
    - [ ] Radio buttons para elegir opción de envío
    - [ ] Actualizar total del checkout

4. **MyAccountPage.jsx** (MODIFICAR)
    - [ ] Agregar tab "📍 Direcciones"
    - [ ] Renderizar <AddressManager /> en el tab

5. **CheckoutPage.jsx** (MODIFICAR)
    - [ ] Agregar estados: deliveryType, selectedAddress, shippingOptions, selectedShipping, deliveryCost
    - [ ] Integrar <ShippingSelector />
    - [ ] Calcular totalWithShipping
    - [ ] Enviar isDelivery, deliveryAddressId, shippingMethodId en orderData

6. **Testing E2E**
    - [ ] Flujo: Crear dirección → Cotizar envío → Comprar con delivery
    - [ ] Flujo: Retiro en local (sin envío)
    - [ ] Validar que Order se cree con shipping data
    - [ ] Verificar total incluye deliveryCost

### 📋 Prioridad Media

7. **Tracking de Envíos** (Futuro)
    - [ ] Crear shipment en MercadoEnvíos post-pago
    - [ ] Guardar shipmentId en Order
    - [ ] Endpoint GET /api/orders/{id}/tracking
    - [ ] Frontend: Mostrar estado de envío en "Mis Órdenes"

8. **WhatsApp Floating Button**
    - [ ] Botón flotante esquina inferior derecha
    - [ ] Número configurable en TenantConfig
    - [ ] Link directo wa.me

9. **Google OAuth Login**
    - [ ] Login con Google
    - [ ] Auto-registro si no existe
    - [ ] Mantener JWT existente

### 🔧 Refactoring & Performance
- [ ] Agregar soft-delete a entidades críticas
- [ ] Cache configuración tenants (Redis)
- [ ] Índices BD para performance (Address por user/tenant)
- [ ] Rate limiting endpoints públicos
- [ ] Logging estructurado (SLF4J)

### 🐛 Bugs Conocidos
- [ ] Validar webhooks MP en producción (ngrok → dominio real)
- [ ] Manejo de timezones en bookings
- [ ] Validar emails únicos por tenant

---

## 📝 DECISIONES TÉCNICAS IMPORTANTES

### ¿Por qué GeoRef API en lugar de campos libres?
- API oficial del gobierno argentino (gratis, datos actualizados)
- IDs oficiales de provincias/municipios
- Mejor UX con autocomplete
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
    - https://claude.ai/chat/f73e801d-c1ea-45ee-8a89-e517c03886ee (Sesión 1)
    - https://claude.ai/chat/f052be26-377a-40ee-a877-08cfae28a7b3 (Sesión 2)
    - [SESIÓN_ACTUAL_2025-11-12] (Sesión 3 - Backend Direcciones + MercadoEnvíos)
- **Repo Git**: https://github.com/franhii/base-template-project
- **Docs MP**: https://www.mercadopago.com.ar/developers/es/docs
- **GeoRef API**: https://apis.datos.gob.ar/georef/api/
- **MercadoEnvíos API**: https://developers.mercadolibre.com.ar/es_ar/envios

---

## 📊 MÉTRICAS DEL PROYECTO

- **Backend**: ~100 archivos Java
- **Frontend**: ~45 archivos JSX/CSS (estimado con nuevos componentes)
- **Total LOC**: ~20,000 (estimado)
- **Endpoints**: ~70
- **Entidades**: 12 principales
- **Tiempo desarrollo**: 3 sesiones intensivas

---

## 🎯 PRÓXIMA SESIÓN (2025-11-13)

**Objetivo**: Frontend completo de Direcciones y Envío
**Archivos a crear**:
- AddressForm.jsx + CSS
- AddressManager.jsx + CSS
- ShippingSelector.jsx + CSS

**Archivos a modificar**:
- MyAccountPage.jsx (agregar tab)
- CheckoutPage.jsx (integrar shipping)

**Resultado esperado**:
- Usuario puede gestionar direcciones en "Mi Cuenta"
- Checkout con selector de envío funcional
- Cotización en tiempo real con MercadoEnvíos
- Compra completa con delivery/pickup
- Testing E2E exitoso del flujo

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