# Contexto del Proyecto - Base Template

## Descripción General
Aplicación web full-stack con sistema de e-commerce que incluye gestión de productos, servicios, carrito de compras, sistema de reservas y procesamiento de pagos con MercadoPago.

## Stack Tecnológico

### Backend
- **Framework**: Spring Boot (Java)
- **Base de datos**: [A determinar - revisar application.properties]
- **Build tool**: Gradle → Maven (migrado recientemente)
- **Gestor de pagos**: MercadoPago API

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
│   └── BookingController.java (nuevo, sin commit)
├── service/
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── BookingService.java (nuevo, sin commit)
├── model/
│   ├── OrderItem.java
│   ├── ServiceItem.java
│   └── Booking.java (nuevo, sin commit)
├── repository/
│   └── BookingRepository.java (nuevo, sin commit)
└── dto/
    └── CreateOrderRequest.java
```

### Frontend
```
frontend/src/
├── components/
│   ├── ProductCard.jsx
│   ├── ServiceCard.jsx
│   ├── Toast.jsx
│   └── BookingModal.jsx (nuevo, sin commit)
├── pages/
│   ├── ManageItemsPage.jsx
│   ├── SuccessPage (para MercadoPago)
│   ├── PendingPage (para MercadoPago)
│   └── FailurePage (para MercadoPago)
└── store/
    └── CartContext.jsx
```

## Funcionalidades Principales

### Implementadas
1. **Gestión de Productos y Servicios**
   - CRUD de items
   - Visualización con cards

2. **Carrito de Compras**
   - Agregar productos/servicios
   - Gestión de cantidades
   - Context API para estado global

3. **Sistema de Órdenes**
   - Creación de órdenes
   - OrderItems con productos y servicios
   - Integración con pagos

4. **MercadoPago Integration**
   - Generación de preferencias de pago
   - Verificación de comprobante
   - Páginas de resultado (success/pending/failure)
   - Redirección desde local al servidor desplegado

5. **Notificaciones**
   - Toast component para feedback visual

### En Desarrollo
1. **Sistema de Reservas (Booking)**
   - Modelo Booking
   - BookingController con endpoints
   - BookingService para lógica de negocio
   - BookingModal en frontend
   - Estado: Código escrito, pendiente testing y commit

### Pendientes
1. **Lógica de Restauración de Stock**
   - Cuando un pago falla o se cancela
   - Liberar items reservados en el carrito

## Convenciones de Código

### Java
- Arquitectura MVC
- Service layer para lógica de negocio
- DTOs para requests/responses
- Repositories para acceso a datos

### React
- Functional components con hooks
- Context API para estado global
- CSS modules para estilos
- Componentes reutilizables

## Estado Actual (2025-10-31)

### Archivos Modificados (staged)
- OrderController.java
- CreateOrderRequest.java
- OrderItem.java
- ServiceItem.java
- OrderService.java
- PaymentService.java
- ProductCard.jsx
- ServiceCard.jsx
- ManageItemsPage.jsx/.css
- CartContext.jsx

### Archivos Nuevos (sin commit)
- Sistema de Booking completo (backend + frontend)

### TODO Crítico
- ⚠️ Implementar lógica de restauración de stock
- ⚠️ Testing completo del sistema de reservas
- ⚠️ Testing de flujos de MercadoPago

## Resúmenes de Sesiones

### Sesión 1 - 2025-10-31
**Objetivos**:
- Recuperar contexto de sesión anterior (perdida)
- Establecer sistema de documentación para continuidad

**Logros**:
- ✅ Creado TODO.md para gestión de tareas
- ✅ Creado TESTING_LOG.md para documentar pruebas
- ✅ Creado CLAUDE.md para contexto del proyecto
- ✅ Identificado estado actual del proyecto

**Próximos Pasos**:
- Revisar y hacer commit de cambios actuales
- Iniciar testing del sistema de reservas
- Testear integración completa de MercadoPago

**Notas**:
- Usuario acordó solicitar resumen al final de cada sesión para actualizar este archivo
- Sistema de documentación establecido para evitar pérdida de contexto

---

**Última actualización**: 2025-10-31
