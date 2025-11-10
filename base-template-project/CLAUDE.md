# Contexto del Proyecto - Base Template

## Descripción General
Aplicación web full-stack con sistema de e-commerce multi-tenant que incluye gestión de productos, servicios, carrito de compras, sistema de reservas y procesamiento de pagos con MercadoPago.

## Stack Tecnológico

### Backend
- **Framework**: Spring Boot (Java)
- **Base de datos**: PostgreSQL
- **Build tool**: Maven (migrado desde Gradle)
- **Gestor de pagos**: MercadoPago API
- **Arquitectura**: Multi-tenant con aislamiento de datos

### Frontend
- **Framework**: React
- **Gestión de estado**: Context API (CartContext)
- **Estilos**: CSS modules
- **UI Components**: Toast notifications

## Arquitectura

### Backend
```
backend/src/main/java/com/example/core/
├── controller/
│   ├── OrderController.java
│   ├── BookingController.java
│   ├── SuperAdminController.java (✅ NUEVO)
│   └── ...
├── service/
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── BookingService.java
│   └── ...
├── model/
│   ├── Tenant.java (✅ Multi-tenant)
│   ├── TenantConfig.java
│   ├── User.java (con rol SUPER_ADMIN)
│   └── ...
├── repository/
│   ├── TenantRepository.java
│   └── ...
├── config/
│   ├── TenantInterceptor.java (✅ Gestión de tenants)
│   └── SecurityConfig.java
└── dto/
    └── ...
```

### Frontend
```
frontend/src/
├── components/
│   ├── ProductCard.jsx
│   ├── ServiceCard.jsx
│   ├── BookingModal.jsx
│   ├── EditTenantModal.jsx (✅ NUEVO)
│   └── Toast.jsx
├── pages/
│   ├── SuperAdminPage.jsx (✅ NUEVO)
│   ├── ManageItemsPage.jsx
│   └── ...
└── store/
    └── CartContext.jsx
```

## Funcionalidades Principales

### Sistema Multi-Tenant
1. **Aislamiento de Datos**
   - Cada tenant tiene sus propios productos, servicios, órdenes
   - Configuración personalizada por tenant (colores, features, etc.)
   - Subdomain-based routing

2. **Roles de Usuario**
   - `SUPER_ADMIN`: Gestiona toda la plataforma y todos los tenants
   - `ADMIN`: Administra un tenant específico
   - `VENDEDOR`: Gestiona productos/servicios de su tenant
   - `CLIENTE`: Usuario final que compra

3. **Tenant Interceptor**
   - Detecta el tenant por subdomain o header HTTP
   - **Header para desarrollo**: `X-Tenant-Subdomain` permite testing local
   - Valida que el tenant esté activo
   - Inyecta el tenant en el contexto de la petición

### Gestión de Productos y Servicios
1. **Productos**
   - CRUD completo
   - Gestión de stock
   - Tipos: Físico/Digital
   - Validación de stock en carrito

2. **Servicios con Booking**
   - Configuración de disponibilidad (días, horarios)
   - Sistema de slots/turnos
   - Reserva previa
   - Validación de conflictos

### Sistema de Pagos
1. **MercadoPago Integration**
   - Generación de preferencias de pago
   - Webhook para confirmación
   - Páginas de resultado (success/pending/failure)
   - Restauración de stock en caso de fallo

2. **Flujo de Pago**
   - Creación de orden
   - Reserva de stock
   - Generación de link de pago
   - Confirmación vía webhook
   - Actualización de estado de orden y bookings

### Sistema de Reservas (Booking)
1. **Funcionalidades**
   - Modal de selección de fecha/hora
   - Validación de disponibilidad
   - Gestión de capacidad máxima
   - Cancelación de reservas (con restricción de 24hs)

## Testing en Local (Multi-Tenant)

### Backend
Para testear múltiples tenants en local sin múltiples subdominios:

**Opción 1: Usar Header HTTP (Recomendado)**
```javascript
// En frontend/src/App.jsx o api.js
api.defaults.headers.common['X-Tenant-Subdomain'] = 'beauty-test';
```

**Configuración del Backend** (TenantInterceptor.java):
```java
// ✅ Da prioridad al header X-Tenant-Subdomain en desarrollo
String subdomain = request.getHeader("X-Tenant-Subdomain");
if (subdomain == null || subdomain.isEmpty()) {
    subdomain = extractSubdomain(request); // Extrae del dominio
}
```

**Opción 2: Modificar /etc/hosts** (Linux/Mac)
```
127.0.0.1   beauty-test.localhost
127.0.0.1   gym-test.localhost
```

### Frontend
```javascript
// services/api.js
export function setTenantHeader(subdomain) {
    if (subdomain) {
        api.defaults.headers.common['X-Tenant-Subdomain'] = subdomain;
        console.log('✅ Header X-Tenant-Subdomain seteado:', subdomain);
    } else {
        delete api.defaults.headers.common['X-Tenant-Subdomain'];
    }
}
```

## Convenciones de Código

### Java
- Arquitectura MVC
- Service layer para lógica de negocio
- DTOs para requests/responses
- Repositories para acceso a datos
- Interceptors para cross-cutting concerns (tenants)

### React
- Functional components con hooks
- Context API para estado global
- CSS modules para estilos
- Componentes reutilizables

## Issues Conocidos y Soluciones

### ❌ Problema: Tenant Interceptor no lee el header
**Síntoma**: Aunque se envía `X-Tenant-Subdomain: beauty-test`, el backend usa `default`

**Causa**: El método `extractSubdomain()` retornaba `"default"` para localhost, por lo que nunca se leía el header.

**Solución**:
```java
private String extractSubdomain(HttpServletRequest request) {
    String host = request.getServerName();
    
    if (host.contains("localhost") || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
        return null; // ← Retornar null en lugar de "default"
    }
    // ...
}
```

## Estado Actual (2025-11-09)

### Completado ✅
- Sistema multi-tenant funcional
- CRUD de productos y servicios
- Sistema de reservas con booking
- Integración completa con MercadoPago
- Panel de Super Admin
- Gestión de tenants (activar/suspender)
- Testing multi-tenant en local con headers

### En Progreso 🚧
- Testing de webhooks de MercadoPago
- Validaciones de stock en flujo completo

### Pendientes 📋
- Sistema de notificaciones por email
- Dashboard con estadísticas avanzadas
- Exportación de reportes

## Resúmenes de Sesiones

### Sesión 1 - 2025-10-31
**Objetivos**:
- Recuperar contexto de sesión anterior (perdida)
- Establecer sistema de documentación para continuidad

**Logros**:
- ✅ Creado TODO.md para gestión de tareas
- ✅ Creado TESTING_LOG.md para documentar pruebas
- ✅ Creado CLAUDE.md para contexto del proyecto

### Sesión 2 - 2025-11-09
**Objetivos**:
- Resolver problema de tenant detection en desarrollo local
- Permitir testing de múltiples tenants sin configurar subdominios

**Logros**:
- ✅ Identificado bug en TenantInterceptor
- ✅ Implementada solución con header HTTP `X-Tenant-Subdomain`
- ✅ Prioridad al header sobre extracción de subdomain
- ✅ Documentado flujo de testing multi-tenant

**Cambios Técnicos**:
- Modificado `TenantInterceptor.preHandle()` para leer header primero
- Modificado `extractSubdomain()` para retornar `null` en localhost
- Agregado helper `setTenantHeader()` en frontend

---

**Última actualización**: 2025-11-09