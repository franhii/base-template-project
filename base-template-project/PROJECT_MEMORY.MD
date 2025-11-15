# PROJECT_MEMORY.md
> **PROPÓSITO**: Contexto técnico completo para Claude. Actualizar al final de cada sesión.
> **ÚLTIMA ACTUALIZACIÓN**: 2025-11-15

---

## 🎯 PROYECTO: Core E-Commerce Multi-Tenant

### Stack
**Backend**: Spring Boot 3.2.0 + Java 21 + PostgreSQL + Maven
**Frontend**: React 18 + Vite + Axios + Context API + CSS Vanilla
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

### Address
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

### Order
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
  
  // ========== SHIPPING ==========
  Address deliveryAddress; // @ManyToOne (null si es pickup)
  BigDecimal deliveryCost; // Costo del envío (de MercadoEnvíos)
  boolean isDelivery; // true = delivery, false = pickup
  String deliveryNotes; // Notas del cliente
  Long shippingMethodId; // ID de MercadoEnvíos (100009, 100012, etc.)
  String shipmentId; // ID del shipment en ML (para tracking)
  String shippingStatus; // pending, ready_to_ship, shipped, delivered
  
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
// CLIENTE: Compras, reservas, perfil, direcciones
```

### SecurityConfig
- **CORS**: Habilitado para localhost:5173, incluye X-Tenant-Subdomain
- **CSRF**: Disabled (JWT stateless)
- **Public endpoints**: /api/auth/**, /api/config/current, /api/items/** (GET), /api/payments/webhook/**, /api/georef/**, /api/shipping/**

---

## 🌐 BACKEND: ENDPOINTS PRINCIPALES

### Shipping (/api/shipping) - MercadoEnvíos
- `POST /quote-by-postalcode` → Cotizar con CP (4 dígitos)
   - Body: `{ postalCode: "2600", orderTotal: 15000 }`
   - Retorna: `[{ shippingMethodId, name, cost, estimatedDelivery, carrier, isFree }]`
   - Validaciones: CP del tenant en config, formato 4 dígitos

- `POST /quote` → Cotizar con addressId
   - Body: `{ addressId: "uuid", orderTotal: 15000 }`
   - Usa CP de la dirección seleccionada

### Addresses (/api/addresses)
- `POST /` → Crear dirección (con GeoRef)
- `GET /` → Listar mis direcciones
- `GET /default` → Obtener dirección por defecto
- `GET /{id}` → Obtener dirección específica
- `PUT /{id}` → Actualizar dirección
- `PATCH /{id}/set-default` → Marcar como default
- `DELETE /{id}` → Eliminar dirección

### GeoRef (/api/georef) - Público
- `GET /provinces` → Todas las provincias argentinas
- `GET /municipalities?provinceId=X` → Municipios por provincia
- `GET /municipalities/search?query=X` → Buscar municipios (autocomplete)
- `GET /localities?municipalityId=X` → Localidades por municipio

### Orders (/api/orders)
- `POST /` → Crea orden + reserva stock + crea bookings + calcula envío
   - Body incluye: `isDelivery, deliveryAddressId?, shippingMethodId?, deliveryNotes?`
   - Validación automática de shipping si isDelivery=true
- `GET /my-orders` → Órdenes del user
- `GET /` → Todas las órdenes del tenant (ADMIN/VENDEDOR)
- `POST /{id}/cancel` → Cancela orden + restaura stock

### Payments, Bookings, Auth, Items... (resto igual)

---

## 🎨 FRONTEND: ESTRUCTURA

### Servicios API (services/api.js)
```javascript
// api.js: axios instance con JWT interceptor + tenant header
// BASE_URL: import.meta.env.VITE_API_URL || 'http://localhost:8080'

// Interceptor request: Agrega Authorization Bearer token
// Interceptor response: 401 → localStorage.removeItem('token')

// Servicios exportados:
authService: { register, login, getCurrentUser }
itemService: { getProducts, getServices, createProduct, createService }
tenantService: { getCurrentConfig }

// Helper multi-tenancy:
setTenantHeader(subdomain) // Setea X-Tenant-Subdomain
```

### Context & State
```javascript
// CartContext (store/CartContext.jsx)
cart: Array<{id, name, price, quantity, stock?, bookingDate?, bookingTime?}>

Métodos:
- addToCart(item, qty) → valida stock
- removeFromCart(id)
- updateQuantity(id, qty) → valida stock
- clearCart()
- getTotal() → sum(price * quantity)
- getTotalItems() → sum(quantity)

Persistencia: localStorage auto-save/load
```

### Páginas Principales
```
HomePage → Tabs (Todo/Productos/Servicios) + Grid de cards
CartPage → Items + Summary (subtotal + IVA 21% + total)
CheckoutPage → Método pago + (PENDIENTE: ShippingSelector)
MyAccountPage → Tabs: Overview, Órdenes, Reservas, Perfil (PENDIENTE: Direcciones)
AdminPage → <AdminDashboard /> (KPIs + Recharts + Pending payments)
ManageItemsPage → CRUD productos/servicios (tabs)
SuperAdminPage → Tabla tenants + CRUD
SuperAdminServiceCreationPage → Crear servicio para tenant específico
LoginPage / RegisterPage
SuccessPage / FailurePage / PendingPage (MP callbacks)
NotFoundPage
```

### Componentes Críticos
```
Navbar → Logo dinámico, cart badge, links por rol
ProductCard / ServiceCard → Validación stock, badges
BookingModal → Selector fecha + slots disponibles
Toast → Notificaciones (4 tipos)
ConfirmModal → Confirmaciones
AdminDashboard → Recharts (LineChart, PieChart, BarChart)
EditTenantModal
```

### Componentes CSS (IMPORTANTE - No modificar estructura)
```
Todos los componentes usan CSS Vanilla con clases propias
NO usar Tailwind ni cambiar nombres de clases existentes
Mantener estructura de divs original
Grid layouts con display: grid y auto-fit/auto-fill
Responsive con @media queries
```

---

## 🔄 FLUJOS CRÍTICOS

### Compra con Envío (En Desarrollo)
```
1. HomePage → Agregar productos al carrito
2. CartPage → Revisar items
   └─ NUEVO: ShippingPreview (cotización preliminar por CP)
3. CheckoutPage
   ├─ Selector delivery/pickup
   ├─ Si delivery: Selector de dirección
   ├─ POST /api/shipping/quote-by-postalcode
   ├─ Mostrar opciones de envío con precios
   └─ Calcular totalWithShipping
4. POST /api/orders (incluye shippingMethodId, addressId)
5. POST /api/payments
6. Redirect a MercadoPago o success
```

### Reserva de Servicio con Booking
```
1. HomePage → ServiceCard (requiresBooking)
2. Click "Reservar" → BookingModal
3. Seleccionar fecha → GET /api/bookings/available
4. Seleccionar slot → addToCart con bookingDate/Time
5. CartPage → Checkout normal
6. Pago aprobado → Booking.status = CONFIRMED
```

---

## 🔄 PENDIENTE (Priorizado)

### 🔥 PRIORIDAD 1 - Frontend Shipping (AHORA)

**CONTEXTO**: Backend de shipping YA está implementado. Necesitamos frontend.

**COMPONENTES A CREAR**:

1. **ShippingPreview.jsx** (CartPage) ✅ CREADO PERO REVISAR
   - Input código postal (4 dígitos)
   - Botón "Calcular envío"
   - POST /api/shipping/quote-by-postalcode
   - Mostrar opciones: nombre, costo, tiempo estimado, courier
   - Radio buttons para seleccionar
   - Badge "GRATIS" si isFree
   - Nota: "Costo final se confirma en checkout"
   - **PROBLEMA ACTUAL**: Cambios en estructura CSS/divs

2. **ShippingSelector.jsx** (CheckoutPage) - PENDIENTE
   - Radio: "Retiro en local" / "Envío a domicilio"
   - Si delivery:
      * Selector de dirección (GET /api/addresses)
      * Botón "Agregar dirección" (futuro AddressForm)
      * POST /api/shipping/quote al seleccionar dirección
      * Mostrar opciones MercadoEnvíos
      * Radio para elegir método
   - Actualizar estado: deliveryType, selectedAddress, shippingMethod, deliveryCost

3. **CartPage.jsx** - MODIFICAR
   - Integrar <ShippingPreview /> en sidebar
   - Mostrar costo estimado en summary (si hay)
   - Nota aclaratoria sobre estimación

4. **CheckoutPage.jsx** - MODIFICAR
   - Integrar <ShippingSelector />
   - Estados: isDelivery, deliveryAddressId, shippingMethodId, deliveryCost
   - Calcular totalWithShipping
   - Enviar datos shipping en POST /api/orders

**ARCHIVOS EXISTENTES A REVISAR**:
- `ShippingPreview.jsx` ✅ Existe
- `ShippingPreview.css` ✅ Existe
- `CartPage.jsx` ⚠️ Verificar integración
- `CartPage.css` ⚠️ Verificar que no se rompió estructura

**TESTING**:
- [ ] Flujo: Calcular envío en cart → Ver opciones → Continuar checkout
- [ ] Flujo: Checkout con retiro en local
- [ ] Flujo: Checkout con delivery (cuando AddressManager esté listo)

---

### 🔥 PRIORIDAD 2 - Frontend Direcciones

**AddressManager.jsx** - PENDIENTE
- Listar direcciones (GET /api/addresses)
- Cards con badge "Default"
- Botones: Editar, Eliminar, Set Default
- Empty state
- ConfirmModal para delete

**AddressForm.jsx** - PENDIENTE
- Modal con campos street, streetNumber, apartment, reference
- Autocompletado GeoRef:
   * Provincias (GET /api/georef/provinces)
   * Municipios (GET /api/georef/municipalities?provinceId=X)
   * Localidades (GET /api/georef/localities?municipalityId=X)
- Input CP (4 dígitos)
- Checkbox isDefault
- POST /api/addresses o PUT /api/addresses/{id}

**MyAccountPage.jsx** - MODIFICAR
- Agregar tab "📍 Direcciones"
- Renderizar <AddressManager />

---

### 📋 PRIORIDAD 3 - Mejoras

- Tracking de envíos (shipmentId en Order)
- WhatsApp floating button
- Google OAuth
- Soft-delete entities
- Cache config tenants
- Rate limiting
- Logging estructurado

---

## 📝 DECISIONES TÉCNICAS CLAVE

### ¿Por qué ShippingPreview en CartPage?
- **UX**: Usuario ve costo estimado ANTES de checkout
- **Conversión**: Reduce abandono (transparencia de costos)
- **Limitación**: Solo cotiza por CP (no requiere dirección completa aún)

### ¿Por qué separar ShippingPreview y ShippingSelector?
- **ShippingPreview** (Cart): Estimación rápida, solo CP
- **ShippingSelector** (Checkout): Selección final con dirección completa
- Evita UX confuso (dos lugares haciendo lo mismo)

### ¿Por qué no modificar estructura CSS existente?
- **Estabilidad**: Componentes ya funcionales y testeados
- **Responsive**: Media queries ya ajustadas
- **Consistencia**: Diseño unificado en toda la app
- **Tiempo**: Evita refactorings innecesarios

### ¿Por qué MercadoEnvíos?
- No requiere contrato corporativo
- Mismas credenciales que MercadoPago
- Múltiples couriers (Correo Argentino + Andreani)
- Tracking incluido
- Tarifas competitivas

---

## 🔗 LINKS IMPORTANTES

- **Chat anteriores**:
   - https://claude.ai/chat/f73e801d-c1ea-45ee-8a89-e517c03886ee (Sesión 1)
   - https://claude.ai/chat/f052be26-377a-40ee-a877-08cfae28a7b3 (Sesión 2)
   - https://claude.ai/chat/34892749-e9fd-4463-b8af-a25605b78d5c (Sesión 3)
   - https://claude.ai/chat/a20c1ea9-e0b5-4552-a346-2a7f57a43937 (Sesion 4)
- **Repo Git**: https://github.com/franhii/base-template-project
- **Docs MP**: https://www.mercadopago.com.ar/developers/es/docs
- **GeoRef API**: https://apis.datos.gob.ar/georef/api/
- **MercadoEnvíos API**: https://developers.mercadolibre.com.ar/es_ar/envios

---

## 🎯 PRÓXIMA ACCIÓN INMEDIATA

**OBJETIVO**: Revisar y corregir ShippingPreview.jsx + Integración en CartPage

**PASOS**:
1. Revisar ShippingPreview.jsx (verificar estructura original)
2. Revisar CartPage.jsx (verificar integración correcta)
3. Testear flujo de cotización
4. Si funciona → Continuar con ShippingSelector.jsx
5. Si hay problemas → Corregir manteniendo CSS original

**ARCHIVOS A REVISAR**:
- `/frontend/src/pages/ShippingPreview.jsx`
- `/frontend/src/pages/ShippingPreview.css`
- `/frontend/src/pages/CartPage.jsx`
- `/frontend/src/pages/CartPage.css`

---

**🔄 ACTUALIZAR ESTE ARCHIVO AL FINAL DE CADA SESIÓN**