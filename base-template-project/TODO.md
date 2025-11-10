# TODO - Tareas Pendientes

## ✅ Completado Recientemente

- [x] Implementar sistema multi-tenant
- [x] Crear panel de Super Admin
- [x] Resolver bug de tenant detection en desarrollo
- [x] Implementar header HTTP para testing multi-tenant
- [x] Sistema de reservas (Booking) completo
- [x] Integración con MercadoPago (webhooks)

## 🎯 Plan de Implementación Actual

### Fase 1: Super Admin Panel ✅ COMPLETADO
- [x] Ruta `/super-admin` solo para rol `SUPER_ADMIN`
- [x] Tabla con todos los tenants
- [x] Columnas: Nombre, Subdomain, Tipo Negocio, Estado, Fecha Creación
- [x] Acciones: Editar, Activar/Desactivar
- [x] Modal para editar: nombre, tipo de negocio, features habilitadas
- [x] Suspensión automática bloquea acceso a ese tenant

### Fase 2: Panel de Cliente 🚧 EN PROGRESO
- [x] Ruta `/my-account` creada
- [x] Estructura de tabs (Overview, Órdenes, Reservas, Perfil)
- [x] Vista de estadísticas (Overview)
- [ ] **Vista de mis órdenes con estados** ⬅️ FALTA IMPLEMENTAR
- [ ] **Vista de mis reservas (próximas y pasadas)** ⬅️ FALTA IMPLEMENTAR
- [ ] **Cancelar reserva (con X días de antelación)** ⬅️ FALTA IMPLEMENTAR
- [ ] **Editar perfil (nombre, teléfono)** ⬅️ FALTA IMPLEMENTAR

### Fase 3: WhatsApp Floating Button 📋 PENDIENTE
- [ ] Botón flotante en esquina inferior derecha
- [ ] Número de WhatsApp configurable por tenant (en TenantConfig)
- [ ] Link directo: `https://wa.me/...?text=...`
- [ ] Animación de entrada
- [ ] Responsive

### Fase 4: Google OAuth Login 📋 PENDIENTE
- [ ] Login con Google (OAuth 2.0)
- [ ] Auto-registro si no existe el usuario
- [ ] Asociar email de Google al tenant actual
- [ ] Mantener JWT funcionando igual

## 🔥 Prioridad Alta

### Testing y Validación
- [ ] Testear flujo completo de MercadoPago (success/pending/failure)
- [ ] Testear restauración de stock en diferentes escenarios:
    - [ ] Pago rechazado
    - [ ] Pago cancelado por el usuario
    - [ ] Timeout de MercadoPago
- [ ] Validar que el sistema de booking funciona con múltiples tenants
- [ ] Testear concurrencia en reservas (múltiples usuarios reservando el mismo slot)

### Mejoras de Código
- [ ] Implementar lógica de restauración de stock cuando falla un pago
- [ ] Agregar validaciones de negocio más robustas:
    - [ ] Validar que el usuario pertenece al tenant correcto
    - [ ] Validar que los items pertenecen al tenant correcto
    - [ ] Validar stock disponible antes de confirmar orden

## 🚀 Features Nuevas (Post-Fases)

### Sistema de Notificaciones
- [ ] Email de confirmación de compra
- [ ] Email de confirmación de reserva
- [ ] Email cuando se cancela una reserva
- [ ] Email cuando un pago es rechazado
- [ ] Notificaciones al admin cuando hay pagos pendientes de revisión

### Dashboard y Reportes
- [ ] Agregar más estadísticas al dashboard:
    - [ ] Productos más vendidos por categoría
    - [ ] Servicios más reservados
    - [ ] Horarios más populares para reservas
- [ ] Exportación de reportes a Excel/PDF
- [ ] Gráficos de tendencias (ventas por mes, etc.)

### Mejoras de UX
- [ ] Sistema de favoritos/wishlist
- [ ] Filtros avanzados de productos/servicios
- [ ] Búsqueda por nombre/categoría
- [ ] Paginación de listados

## 🔧 Refactoring y Optimización

### Backend
- [ ] Agregar cache para configuración de tenants
- [ ] Implementar soft-delete en lugar de hard-delete
- [ ] Agregar índices en BD para mejorar performance
- [ ] Implementar rate limiting en endpoints públicos
- [ ] Agregar logging estructurado (por ejemplo, con SLF4J)

### Frontend
- [ ] Extraer lógica de API a custom hooks
- [ ] Implementar lazy loading de componentes
- [ ] Agregar skeleton loaders para mejor UX
- [ ] Optimizar imágenes con lazy loading
- [ ] Implementar service worker para PWA

## 📚 Documentación

- [ ] Documentar endpoints de API (Swagger/OpenAPI)
- [ ] Crear guía de deployment
- [ ] Documentar variables de entorno necesarias
- [ ] Crear guía de testing multi-tenant
- [ ] Documentar flujo de webhooks de MercadoPago

## 🐛 Bugs Conocidos

### Resueltos ✅
- [x] ~~Tenant interceptor no lee header X-Tenant-Subdomain~~ (Resuelto 2025-11-09)

### Por Resolver
- [ ] Validar que el webhook de MercadoPago funciona en producción
- [ ] Verificar manejo de timezones en bookings
- [ ] Validar que los emails de usuario sean únicos por tenant (actualmente son únicos globalmente)

## 🔐 Seguridad

- [ ] Implementar rate limiting para prevenir ataques
- [ ] Agregar CSRF protection
- [ ] Validar y sanitizar todos los inputs del usuario
- [ ] Implementar 2FA opcional para admins
- [ ] Auditoría de logs de acceso
- [ ] Implementar política de contraseñas fuertes

## 🌐 Internacionalización

- [ ] Implementar i18n en frontend
- [ ] Soporte para múltiples monedas
- [ ] Adaptar formato de fechas según locale
- [ ] Traducir emails de notificación

## 📱 Mobile

- [ ] Hacer la app completamente responsive
- [ ] Optimizar experiencia en mobile
- [ ] Considerar app nativa (React Native/Flutter)

---

## Notas de Desarrollo

### Testing Multi-Tenant en Local
Para testear diferentes tenants en desarrollo:

**Opción 1: Usar Header HTTP** (Recomendado)
```javascript
// En App.jsx antes de cargar la app
api.defaults.headers.common['X-Tenant-Subdomain'] = 'beauty-test';
```

**Opción 2: Configurar hosts**
Agregar en `/etc/hosts` (Linux/Mac) o `C:\Windows\System32\drivers\etc\hosts` (Windows):
```
127.0.0.1   beauty-test.localhost
127.0.0.1   gym-test.localhost
```

### Crear Nuevo Tenant para Testing
```bash
# Usando Super Admin panel o directamente en BD:
INSERT INTO tenants (id, subdomain, business_name, type, active, config, created_at)
VALUES (
  gen_random_uuid(),
  'beauty-test',
  'Beauty Salon Test',
  'BEAUTY_SALON',
  true,
  '{"primaryColor": "#ec4899", "features": {"products": true, "services": true}}',
  NOW()
);
```

---

**Última actualización**: 2025-11-09