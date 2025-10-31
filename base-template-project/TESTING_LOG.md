# Testing Log

Registro de pruebas realizadas en el proyecto.

## Formato de entrada
```
### [Fecha] - [Funcionalidad]
**Tester**: [Nombre]
**Estado**: ✅ EXITOSO | ⚠️ PARCIAL | ❌ FALLIDO

**Descripción**:
- Qué se probó

**Resultados**:
- Resultado 1
- Resultado 2

**Bugs encontrados**:
- Bug 1
- Bug 2

**Notas adicionales**:
- Observaciones relevantes
```

---

## Pruebas Pendientes

### Sistema de Reservas (Booking)
- [ ] Crear reserva desde frontend
- [ ] Validar fechas y horarios
- [ ] Verificar persistencia en BD
- [ ] Probar endpoints de BookingController
- [ ] Integración con sistema de órdenes

### Carrito de Compras
- [ ] Agregar productos al carrito
- [ ] Agregar servicios al carrito
- [ ] Modificar cantidades
- [ ] Eliminar items
- [ ] Calcular totales correctamente
- [ ] Persistencia del carrito

### Sistema de Órdenes
- [ ] Crear orden desde carrito
- [ ] Validar datos de orden
- [ ] Verificar creación de OrderItems
- [ ] Integrar productos y servicios

### Integración MercadoPago
- [ ] Flujo de pago exitoso (approved)
- [ ] Flujo de pago pendiente (pending)
- [ ] Flujo de pago fallido (rejected)
- [ ] Redirecciones correctas
- [ ] Verificación de comprobante de pago
- [ ] Actualización de estado de orden
- [ ] Restauración de stock en caso de fallo

### UI/UX
- [ ] Toast notifications funcionando
- [ ] Manejo de errores visible
- [ ] Responsive design
- [ ] Validaciones de formularios

---

## Registro de Pruebas Realizadas

### [Pendiente] - Primera sesión de testing
**Tester**: Francis
**Estado**: ⏳ PENDIENTE

**Descripción**:
- Sesión inicial de testing después de implementar sistema de reservas y MercadoPago

---

**Última actualización**: 2025-10-31
