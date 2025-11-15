📦 FRONTEND CORE TEMPLATE - DOCUMENTACIÓN COMPLETA
🏗️ ARQUITECTURA GENERAL
frontend/
├── public/              # Assets estáticos
├── src/
│   ├── components/      # Componentes reutilizables
│   ├── pages/          # Páginas/Vistas
│   ├── services/       # API y servicios
│   ├── store/          # Estado global (Context API)
│   ├── App.jsx         # Componente raíz + routing
│   ├── App.css         # Estilos globales app
│   ├── index.css       # Reset + estilos base
│   └── main.jsx        # Entry point
├── index.html          # HTML base
├── package.json        # Dependencias
└── vite.config.js      # Configuración Vite
📊 DEPENDENCIAS
json{
  "dependencies": {
    "axios": "^1.12.2",           // HTTP client
    "react": "^18.2.0",           // Core
    "react-dom": "^18.2.0",       // DOM rendering
    "react-router-dom": "^7.9.4", // Routing
    "recharts": "^3.3.0"          // Gráficos dashboard
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.0.0",
    "vite": "^5.0.0"
  }
}
🎨 SISTEMA DE DISEÑO
Variables CSS (App.css)
css:root {
  --primary-color: #667eea;      /* Violeta principal */
  --secondary-color: #764ba2;    /* Violeta secundario */
  --accent-color: /* dinámico */
  --font-family: /* dinámico */
}
Colores Base

Primary Gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
Success: #10b981 (verde)
Error: #ef4444 (rojo)
Warning: #f59e0b (naranja)
Info: #3b82f6 (azul)
Gray Scale: #f5f7fa → #1e293b

Sombras
css.card-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
.hover-shadow: 0 12px 25px rgba(0, 0, 0, 0.15);
.intense-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
🚦 ROUTING (App.jsx)
javascript// Estructura de rutas
Routes
├── / (HomePage) - Público
├── /login (LoginPage) - Solo no autenticados
├── /register (RegisterPage) - Solo no autenticados
├── /cart (CartPage) - ProtectedRoute
├── /checkout (CheckoutPage) - ProtectedRoute
├── /my-account (MyAccountPage) - ProtectedRoute (CLIENTE)
├── /admin (AdminPage) - AdminRoute (ADMIN/VENDEDOR)
├── /admin/manage-items (ManageItemsPage) - AdminRoute
├── /super-admin (SuperAdminPage) - SuperAdminRoute (SUPER_ADMIN)
├── /super-admin/create-service - SuperAdminRoute
├── /success (SuccessPage) - Público (MercadoPago callback)
├── /failure (FailurePage) - Público (MercadoPago callback)
├── /pending (PendingPage) - Público (MercadoPago callback)
└── * (NotFoundPage) - Catch-all 404
Guards Implementados
javascript// ProtectedRoute: Solo usuarios autenticados
const ProtectedRoute = ({ element }) => {
  if (loading) return <Spinner/>;
  return user ? element : <Navigate to="/login" />;
};

// AdminRoute: Solo ADMIN y VENDEDOR
const AdminRoute = ({ element }) => {
  if (loading) return <Spinner/>;
  if (!user) return <Navigate to="/login" />;
  if (user.role !== 'ADMIN' && user.role !== 'VENDEDOR') {
    return <Navigate to="/" />;
  }
  return element;
};

// SuperAdminRoute: Solo SUPER_ADMIN
const SuperAdminRoute = ({ element }) => {
  if (loading) return <Spinner/>;
  if (!user) return <Navigate to="/login" />;
  if (user.role !== 'SUPER_ADMIN') return <Navigate to="/" />;
  return element;
};
📡 API SERVICE (services/api.js)
javascript// Configuración
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' }
});

// Multi-tenancy header
export function setTenantHeader(subdomain) {
  if (subdomain) {
    api.defaults.headers.common['X-Tenant-Subdomain'] = subdomain;
  }
}

// Interceptores
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
    }
    return Promise.reject(error);
  }
);
Servicios Exportados
javascriptauthService: {
  register: (data) => POST /api/auth/register
  login: (data) => POST /api/auth/login
  getCurrentUser: () => GET /api/auth/me
}

itemService: {
  getProducts: () => GET /api/items/products
  getServices: () => GET /api/items/services
  getProduct: (id) => GET /api/items/products/:id
  getService: (id) => GET /api/items/services/:id
  createProduct: (data) => POST /api/items/products
  createService: (data) => POST /api/items/services
}

tenantService: {
  getCurrentConfig: () => GET /api/config/current
}
🛒 CART CONTEXT (store/CartContext.jsx)
Estado Global del Carrito
javascript// Estructura del cart item
{
  id: string,
  name: string,
  price: number,
  quantity: number,
  stock: number | undefined,  // Si es producto físico
  imageUrl: string,
  category: string,
  // Si es servicio con booking:
  bookingDate?: string,
  bookingTime?: string
}
Métodos Disponibles
javascriptconst {
  cart,                    // Array de items
  addToCart,              // (item, quantity) => void
  removeFromCart,         // (itemId) => void
  updateQuantity,         // (itemId, quantity) => void
  clearCart,              // () => void
  getTotal,               // () => number
  getTotalItems           // () => number
} = useCart();
Validaciones de Stock
javascript// Al agregar item
if (item.stock !== undefined && quantity > item.stock) {
  console.warn(`Stock insuficiente`);
  quantity = item.stock; // Limitar al stock máximo
}

// Al actualizar cantidad
if (i.stock !== undefined && quantity > i.stock) {
  return { ...i, quantity: i.stock };
}
Persistencia

LocalStorage: Auto-save en cada cambio
Carga inicial: useEffect lee localStorage al montar

🧩 COMPONENTES PRINCIPALES
1. Navbar (components/Navbar.jsx)
Estructura:
javascript<nav className="navbar">
  <Logo />
  <HamburgerToggle /> {/* Solo mobile */}
  <NavMenu className={menuOpen ? 'active' : ''}>
    <Link to="/">Inicio</Link>
    
    {user ? (
      <>
        <CartLink badge={getTotalItems()} />
        
        {user.role === 'CLIENTE' && (
          <Link to="/my-account">👤 Mi Cuenta</Link>
        )}
        
        {(user.role === 'ADMIN' || user.role === 'VENDEDOR') && (
          <Link to="/admin">📊 Admin</Link>
        )}
        
        {user.role === 'SUPER_ADMIN' && (
          <Link to="/super-admin">🔐 Super Admin</Link>
        )}
        
        <UserInfo name={user.name} />
        <LogoutButton />
      </>
    ) : (
      <>
        <Link to="/login">Iniciar Sesión</Link>
        <Link to="/register">Registrarse</Link>
      </>
    )}
  </NavMenu>
</nav>
Features:

Logo dinámico desde config del tenant
Badge de cantidad en carrito
Menú hamburguesa responsive
Links condicionales por rol


2. ProductCard (components/ProductCard.jsx)
Props: { product: Product }
Estructura:
jsx<div className="product-card">
  <div className="product-image">
    {imageUrl ? <img /> : <NoImageIcon />}
    {stock <= 5 && stock > 0 && <Badge warning />}
    {stock === 0 && <Badge danger />}
  </div>
  
  <div className="product-info">
    <h3>{name}</h3>
    <span className="category">{category}</span>
    <p className="description">{description}</p>
    
    <div className="footer">
      <span className="price">${price}</span>
      <button 
        onClick={handleAddToCart} 
        disabled={stock === 0}
      >
        {stock === 0 ? '❌ Sin stock' : '🛒 Agregar'}
      </button>
    </div>
    
    {sku && <small>SKU: {sku}</small>}
  </div>
</div>
```

**Validaciones al agregar:**
1. Verificar `stock === 0`
2. Calcular cantidad actual en carrito
3. Validar `currentQuantity + 1 <= stock`
4. Mostrar toast de éxito/error

---

### 3. ServiceCard (components/ServiceCard.jsx)

**Props:** `{ service: Service }`

**Features:**
- Badge de duración
- Botón "Reservar" si `requiresBooking === true`
- Modal de booking para servicios con turnos
- Muestra: scheduleType, maxCapacity, requiresBooking

**Flujo de reserva:**
```
1. Click "Reservar"
2. Abrir BookingModal
3. Usuario selecciona fecha y horario
4. Confirmar → addToCart con bookingDate y bookingTime
5. Toast de confirmación

4. BookingModal (components/BookingModal.jsx)
Props:
javascript{
  service: Service,
  onClose: () => void,
  onConfirm: (bookingData) => void
}
```

**Flujo:**
```
1. Generar fechas disponibles (próximos 30 días)
   - Filtrar por availableDays del servicio
   
2. Al seleccionar fecha:
   GET /api/bookings/available?serviceId&date
   → Retorna slots con availableSpots
   
3. Renderizar grid de slots:
   - Mostrar startTime, endTime
   - Disponibilidad: availableSpots/totalCapacity
   
4. Confirmar:
   onConfirm({ date, time: selectedSlot.startTime })
Estructura de slot:
javascript{
  startTime: "09:00",
  endTime: "10:00",
  availableSpots: 8,
  totalCapacity: 10
}

5. AdminDashboard (components/AdminDashboard.jsx)
Endpoints consumidos:
javascript// KPIs principales
GET /api/admin/stats/dashboard
→ { totalSales, monthlyRevenue, averageTicket, 
    activeCustomers, pendingOrders, pendingPayments }

// Gráficos
GET /api/admin/stats/daily-sales?days=30
GET /api/admin/stats/top-products?limit=5
GET /api/admin/stats/payment-methods
GET /api/admin/stats/order-status

// Pagos pendientes
GET /api/admin/stats/pending-payments
Componentes de Recharts:

LineChart: Ventas diarias (últimos 30 días)
PieChart: Métodos de pago
PieChart: Estados de órdenes
BarChart: Productos más vendidos

Tabla de pagos pendientes:

Botón "Aprobar" → PATCH /api/payments/:id/approve
Botón "Rechazar" → PATCH /api/payments/:id/reject + reason


6. Toast (components/Toast.jsx)
Props:
javascript{
  message: string,
  type: 'success' | 'error' | 'warning' | 'info',
  onClose: () => void,
  duration: number = 3000
}
Uso:
javascriptconst [toast, setToast] = useState(null);

const showToast = (message, type) => {
  setToast({ message, type });
  setTimeout(() => setToast(null), 3000);
};

{toast && <Toast {...toast} onClose={() => setToast(null)} />}
Estilos:

Position: fixed top-20 right-20
Auto-close después de duration
Iconos por tipo: ✓ ✕ ⚠ ℹ


7. ConfirmModal (components/ConfirmModal.jsx)
Props:
javascript{
  isOpen: boolean,
  title: string,
  message: string,
  onConfirm: () => void,
  onCancel: () => void,
  confirmText: string = 'Confirmar',
  cancelText: string = 'Cancelar',
  type: 'warning' | 'danger' | 'info' = 'warning'
}
Estilos por tipo:

warning: bg-orange-500
danger: bg-red-500
info: bg-blue-500

Uso típico:
javascriptconst [confirmAction, setConfirmAction] = useState(null);

const handleDelete = () => {
  setConfirmAction({
    title: '¿Eliminar item?',
    message: 'Esta acción no se puede deshacer',
    type: 'danger',
    onConfirm: async () => {
      await deleteItem();
      setConfirmAction(null);
    }
  });
};

{confirmAction && (
  <ConfirmModal
    isOpen={true}
    {...confirmAction}
    onCancel={() => setConfirmAction(null)}
  />
)}
📄 PÁGINAS PRINCIPALES
1. HomePage (pages/HomePage.jsx)
Features:

Hero section con nombre del negocio (desde config)
Tabs: Todo / Productos / Servicios
Grid responsive de ProductCard y ServiceCard
Empty state si no hay items

Carga de datos:
javascriptuseEffect(() => {
  const loadItems = async () => {
    const promises = [];
    
    if (config?.features?.products !== false) {
      promises.push(itemService.getProducts());
    }
    
    if (config?.features?.services !== false) {
      promises.push(itemService.getServices());
    }
    
    const results = await Promise.all(promises);
    setProducts(results[0]?.data || []);
    setServices(results[1]?.data || []);
  };
  
  loadItems();
}, []);

2. CartPage (pages/CartPage.jsx)
Layout: Grid 2 columnas (items + summary)
Items list:
jsx{cart.map(item => (
  <div className="cart-item">
    <ItemImage />
    <ItemInfo />
    
    <QuantityControls>
      <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>-</button>
      <span>{item.quantity}</span>
      <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
    </QuantityControls>
    
    <Subtotal>${(item.price * item.quantity).toFixed(2)}</Subtotal>
    <RemoveButton onClick={() => removeFromCart(item.id)} />
  </div>
))}
Summary sidebar:
jsx<div className="cart-summary">
  <h2>Resumen</h2>
  
  <SummaryRow label="Subtotal" value={subtotal} />
  <SummaryRow label="IVA (21%)" value={tax} />
  <Divider />
  <SummaryRow label="Total" value={total} bold />
  
  <button onClick={() => navigate('/checkout')}>
    Proceder al Pago
  </button>
  
  <button onClick={() => navigate('/')}>
    Continuar Comprando
  </button>
</div>
Cálculo de totales:
javascriptconst getTotals = () => {
  const subtotal = getTotal(); // Suma de price * quantity
  const tax = subtotal * 0.21;  // IVA 21%
  const total = subtotal + tax;
  return { subtotal, tax, total };
};
```

---

### 3. CheckoutPage (pages/CheckoutPage.jsx)

**Flujo:**
```
1. Verificar cart no vacío
2. Mostrar resumen de items
3. Seleccionar método de pago
4. Agregar notas opcionales
5. Confirmar → Crear orden + pago
6. Redirigir según método:
   - MERCADO_PAGO: window.location.href = paymentLink
   - Otros: clearCart() + navigate('/')
Estructura del request:
javascript// 1. Crear orden
POST /api/orders
{
  items: [{ itemId, quantity }],
  paymentMethod: 'MERCADO_PAGO' | 'BANK_TRANSFER' | 'CASH',
  notes: string
}
→ { id, status, total, ... }

// 2. Crear pago
POST /api/payments
{
  orderId: string,
  method: string
}
→ { id, status, paymentLink?, ... }
Métodos de pago disponibles:

MercadoPago (redirige a checkout externo)
Transferencia Bancaria
Efectivo


4. MyAccountPage (pages/MyAccountPage.jsx)
Tabs:

Overview: Stats + Quick Actions
Mis Órdenes: Historial de compras
Mis Reservas: Servicios con booking
Mi Perfil: Editar datos personales

Endpoints:
javascript// Stats
GET /api/customer/stats
→ { totalOrders, completedOrders, totalSpent, upcomingBookings }

// Órdenes
GET /api/customer/orders
→ [{ id, status, total, paymentMethod, createdAt }]

// Reservas
GET /api/customer/bookings
→ [{ 
  id, serviceName, serviceImageUrl, 
  bookingDate, startTime, endTime, 
  serviceDuration, status 
}]

// Perfil
GET /api/customer/profile
→ { name, email, phone, role }

PUT /api/customer/profile
{ name, phone }

// Cancelar reserva
POST /api/customer/bookings/:id/cancel
{ reason: string }
Badges de estado:
javascriptconst getStatusBadgeClass = (status) => ({
  'PENDING': 'badge-warning',
  'CONFIRMED': 'badge-success',
  'PREPARING': 'badge-info',
  'READY': 'badge-info',
  'COMPLETED': 'badge-success',
  'CANCELLED': 'badge-danger'
}[status] || 'badge-default');

5. AdminPage (pages/AdminPage.jsx)
Header:

Nombre del tenant (badge)
Botón "Gestionar Items" → /admin/manage-items
User info + logout

Body:

<AdminDashboard /> (KPIs + gráficos + tabla)

Verificación de acceso:
javascriptuseEffect(() => {
  const verifyAccess = async () => {
    const response = await api.get('/api/auth/me');
    const userData = response.data;
    
    if (userData.role !== 'ADMIN' && userData.role !== 'VENDEDOR') {
      navigate('/');
      return;
    }
    
    setUser(userData);
  };
  
  verifyAccess();
}, []);

6. ManageItemsPage (pages/ManageItemsPage.jsx)
Tabs:

Productos
Servicios

Formulario de Producto:
javascript{
  name: string,          // *
  description: string,
  price: number,         // *
  category: string,
  imageUrl: string,
  stock: number,         // *
  sku: string,
  type: 'PHYSICAL' | 'DIGITAL'
}
Formulario de Servicio:
javascript{
  name: string,                    // *
  description: string,
  price: number,                   // *
  category: string,
  imageUrl: string,
  durationMinutes: number,         // *
  scheduleType: 'ON_DEMAND' | 'SCHEDULED' | 'RECURRING',
  maxCapacity: number,
  requiresBooking: boolean,
  
  // Si requiresBooking === true:
  availableDays: string[],         // * ['MONDAY', 'TUESDAY', ...]
  workStartTime: string,           // * '09:00'
  workEndTime: string,             // * '18:00'
  slotIntervalMinutes: number      // * 30
}
Validaciones críticas:
javascript// Productos
if (productData.type === 'PHYSICAL' && stock === 0) {
  showToast('Advertencia: Producto físico sin stock', 'warning');
}

// Servicios con booking
if (serviceData.requiresBooking) {
  if (availableDays.length === 0) {
    showToast('Selecciona al menos un día disponible', 'error');
    return;
  }
  
  if (!workStartTime || !workEndTime || !slotIntervalMinutes) {
    showToast('Define el horario de atención', 'error');
    return;
  }
}
Selector de días:
jsx<div className="days-selector">
  {['MONDAY', 'TUESDAY', 'WEDNESDAY', ...].map(day => (
    <button
      className={`day-btn ${availableDays.includes(day) ? 'active' : ''}`}
      onClick={() => handleDayToggle(day)}
    >
      {dayLabels[day]}
    </button>
  ))}
</div>

7. SuperAdminPage (pages/SuperAdminPage.jsx)
Header:

Badge "Platform Management"
Botón "Crear Servicio" → /super-admin/create-service
Botón "Actualizar"
User info + logout

Tabla de Tenants:
jsx<table>
  <thead>
    <tr>
      <th>Negocio</th>
      <th>Subdomain</th>
      <th>Tipo</th>
      <th>Estado</th>
      <th>Fecha Creación</th>
      <th>Acciones</th>
    </tr>
  </thead>
  <tbody>
    {tenants.map(tenant => (
      <tr className={!tenant.active ? 'tenant-suspended' : ''}>
        <td>
          {tenant.businessName}
          {!tenant.active && <Badge>Suspendido</Badge>}
        </td>
        <td><code>{tenant.subdomain}</code></td>
        <td><Badge>{tenant.type}</Badge></td>
        <td>
          <Badge className={tenant.active ? 'active' : 'inactive'}>
            {tenant.active ? '✅ Activo' : '⛔ Suspendido'}
          </Badge>
        </td>
        <td>{formatDate(tenant.createdAt)}</td>
        <td>
          <button onClick={() => handleEdit(tenant)}>✏️</button>
          <button onClick={() => handleToggleStatus(tenant)}>
            {tenant.active ? '⛔' : '✅'}
          </button>
        </td>
      </tr>
    ))}
  </tbody>
</table>
Endpoints:
javascriptGET /api/super-admin/tenants
PATCH /api/super-admin/tenants/:id/toggle-status
PUT /api/super-admin/tenants/:id

8. SuperAdminServiceCreationPage
Features exclusivas:

Selector de tenant (campo adicional)
Mismo formulario que ManageItemsPage pero con tenantId

Validación inicial:
javascriptuseEffect(() => {
  const loadTenants = async () => {
    const response = await api.get('/api/super-admin/tenants');
    
    // Filtrar solo tenants con servicios habilitados
    const tenantsWithServices = response.data.filter(t =>
      t.active && t.config?.features?.services === true
    );
    
    setTenants(tenantsWithServices);
  };
  
  loadTenants();
}, []);
Request:
javascriptPOST /api/super-admin/services
{
  tenantId: string,  // ← Campo adicional
  name: string,
  description: string,
  price: number,
  // ... resto igual a servicio normal
}
```

---

### 9. LoginPage (pages/LoginPage.jsx)

**Flujo:**
```
1. Submit form
2. POST /api/auth/login { email, password }
   → { token: string }
3. localStorage.setItem('token', token)
4. GET /api/auth/me
   → { id, name, email, role, ... }
5. setUser(userData)
6. Redirigir según rol:
   - ADMIN/VENDEDOR → /admin
   - Otros → /
```

**Demo credentials (mostradas en UI):**
```
Email: admin@admin.com
Contraseña: admin123

10. RegisterPage (pages/RegisterPage.jsx)
Validaciones:
javascript// Client-side
- name, email, password: required
- password !== confirmPassword → error
- password.length < 6 → error

// Server-side
- Email único (validado en backend)
```

**Flujo:**
```
1. POST /api/auth/register { name, email, phone, password }
2. Auto-login: POST /api/auth/login { email, password }
3. localStorage.setItem('token', token)
4. GET /api/auth/me
5. setUser(userData)
6. navigate('/')

11. MercadoPago Callback Pages
SuccessPage

Query params: ?payment_id=X&status=approved&merchant_order_id=Y
Limpia carrito: localStorage.removeItem('cart')
Muestra detalles de la orden
Botones: "Seguir Comprando" / "Ver Productos"

FailurePage

Query params: ?payment_id=X&status=rejected&status_detail=cc_rejected_*
Mapea status_detail a mensajes user-friendly
Sugerencias de acción según error
Botones: "Intentar Nuevamente" / "Volver al Catálogo"

PendingPage

Query params: ?payment_id=X&status=pending
Info: "Pago en proceso de verificación"
Botones: "Volver al Catálogo" / "Actualizar Estado"

Ejemplos de status_detail:
javascript{
  'cc_rejected_insufficient_amount': 'Fondos insuficientes',
  'cc_rejected_bad_filled_security_code': 'Código de seguridad incorrecto',
  'cc_rejected_bad_filled_date': 'Fecha de vencimiento incorrecta',
  'cc_rejected_call_for_authorize': 'Debes autorizar el pago con tu banco',
  'cc_rejected_duplicated_payment': 'Ya existe un pago con esos datos',
  'cc_rejected_high_risk': 'Pago rechazado por seguridad'
}
🎭 MULTI-TENANCY
Configuración Dinámica (App.jsx)

javascript        metaDescription.content = tenantConfig.businessDescription;
      }
    })
    .catch(err => console.error('Error loading tenant config:', err));
}, []);
Estructura de Config Response
javascript{
  id: "tenant-uuid",
  businessName: "Mi Gimnasio",
  subdomain: "gimnasio",
  type: "GYM",
  active: true,
  config: {
    primaryColor: "#667eea",
    secondaryColor: "#764ba2",
    accentColor: "#10b981",
    fontFamily: "'Poppins', sans-serif",
    logo: "https://example.com/logo.png",
    favicon: "https://example.com/favicon.ico",
    customCssUrl: "https://example.com/custom.css",
    businessDescription: "El mejor gimnasio de la ciudad",
    features: {
      products: true,
      services: true,
      bookings: true,
      mercadopago: true
    }
  }
}
Header X-Tenant-Subdomain
El backend identifica el tenant mediante este header:
javascript// Setear en api.js
setTenantHeader('gimnasio');

// El interceptor lo incluye en cada request
api.defaults.headers.common['X-Tenant-Subdomain'] = 'gimnasio';
🔐 AUTENTICACIÓN Y AUTORIZACIÓN
Estados de Usuario
javascript// No autenticado
user = null

// Cliente
user = {
  id: "user-uuid",
  name: "Juan Pérez",
  email: "juan@email.com",
  phone: "+54 9 11 1234-5678",
  role: "CLIENTE",
  tenantId: "tenant-uuid",
  tenantName: "Mi Gimnasio"
}

// Admin/Vendedor
user = {
  ...
  role: "ADMIN" | "VENDEDOR"
}

// Super Admin
user = {
  ...
  role: "SUPER_ADMIN",
  tenantId: null  // No pertenece a un tenant específico
}
Flujo de Login Completo
javascript// 1. Usuario envía credenciales
const handleLogin = async (email, password) => {
  try {
    // 2. Request al backend
    const loginResponse = await authService.login({ email, password });
    const token = loginResponse.data.token;
    
    // 3. Guardar token
    localStorage.setItem('token', token);
    
    // 4. El interceptor automáticamente lo incluye en requests
    // Authorization: Bearer <token>
    
    // 5. Obtener datos completos del usuario
    const userResponse = await authService.getCurrentUser();
    setUser(userResponse.data);
    
    // 6. Redirigir según rol
    if (userResponse.data.role === 'ADMIN' || 
        userResponse.data.role === 'VENDEDOR') {
      navigate('/admin');
    } else if (userResponse.data.role === 'SUPER_ADMIN') {
      navigate('/super-admin');
    } else {
      navigate('/');
    }
  } catch (error) {
    console.error('Login error:', error);
    setError('Credenciales inválidas');
  }
};
Verificación de Usuario al Cargar App
javascript// App.jsx - useEffect inicial
useEffect(() => {
  const token = localStorage.getItem('token');
  
  if (token) {
    api.get('/api/auth/me')
      .then(response => {
        setUser(response.data);
      })
      .catch(err => {
        console.error('Error loading user:', err);
        localStorage.removeItem('token');
      })
      .finally(() => setLoading(false));
  } else {
    setLoading(false);
  }
}, []);
Logout
javascriptconst handleLogout = () => {
  localStorage.removeItem('token');
  setUser(null);
  navigate('/login');
};
📱 RESPONSIVE DESIGN
Breakpoints
css/* Mobile First Approach */

/* Base: Mobile (< 768px) */
.container {
  padding: 15px;
}

/* Tablet (768px - 1024px) */
@media (max-width: 1024px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Desktop (> 1024px) */
@media (min-width: 1025px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
Navbar Mobile
css/* Desktop */
.navbar-menu {
  display: flex;
  gap: 25px;
}

.navbar-toggle {
  display: none;
}

/* Mobile */
@media (max-width: 768px) {
  .navbar-toggle {
    display: flex; /* Mostrar hamburger */
  }
  
  .navbar-menu {
    position: fixed;
    top: 60px;
    right: -100%; /* Oculto por defecto */
    width: 280px;
    height: calc(100vh - 60px);
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    flex-direction: column;
    padding: 30px 20px;
    transition: right 0.3s ease;
  }
  
  .navbar-menu.active {
    right: 0; /* Mostrar al hacer click */
  }
}
Grids Responsivos
css/* Auto-fit con min-width */
.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 30px;
}

/* Ajuste para mobile */
@media (max-width: 768px) {
  .items-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
Cart Item Mobile
css/* Desktop: Grid horizontal */
.cart-item {
  display: grid;
  grid-template-columns: 100px 1fr auto auto auto;
  gap: 20px;
}

/* Mobile: Stack vertical */
@media (max-width: 768px) {
  .cart-item {
    grid-template-columns: 80px 1fr;
    grid-template-rows: auto auto auto;
  }
  
  .item-image {
    grid-row: 1 / 3;
  }
  
  .item-info {
    grid-column: 2;
  }
  
  .item-quantity {
    grid-column: 1 / 2;
    grid-row: 3;
  }
  
  .item-subtotal {
    grid-column: 2;
    grid-row: 3;
  }
  
  .btn-remove {
    position: absolute;
    top: 15px;
    right: 15px;
  }
}
🎨 ANIMACIONES Y TRANSICIONES
Transiciones Comunes
css/* Hover suave */
.button {
  transition: all 0.3s ease;
}

.button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

/* Focus states */
input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  transition: all 0.3s ease;
}
Animaciones Keyframes
css/* Loading spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner {
  animation: spin 1s linear infinite;
}

/* Slide up (modales) */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-content {
  animation: slideUp 0.5s ease-out;
}

/* Bounce (success icons) */
@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20px);
  }
  60% {
    transform: translateY(-10px);
  }
}

.success-icon {
  animation: bounce 1s ease-in-out;
}

/* Float (404 title) */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.notfound-title {
  animation: float 3s ease-in-out infinite;
}

/* Pulse (pending icons) */
@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

.pending-icon {
  animation: pulse 2s ease-in-out infinite;
}

/* Shake (error icons) */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  75% { transform: translateX(10px); }
}

.failure-icon {
  animation: shake 0.5s ease-in-out;
}

/* Fade in */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-overlay {
  animation: fadeIn 0.2s ease-out;
}

/* Scale in */
@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.modal-content {
  animation: scaleIn 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}
🛠️ UTILIDADES Y HELPERS
Format Functions
javascript// Formatear precio
const formatPrice = (price) => {
  return `$${parseFloat(price).toFixed(2)}`;
};

// Formatear fecha
const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('es-AR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

// Formatear fecha corta
const formatDateShort = (dateString) => {
  return new Date(dateString).toLocaleDateString('es-AR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
};

// Formatear día de la semana
const formatDayName = (date) => {
  const dayNames = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 
                    'Jueves', 'Viernes', 'Sábado'];
  return dayNames[date.getDay()];
};

// Formatear schedule type
const formatScheduleType = (type) => {
  const types = {
    'ON_DEMAND': 'Bajo demanda',
    'SCHEDULED': 'Con turno',
    'RECURRING': 'Recurrente'
  };
  return types[type] || type;
};
Validation Functions
javascript// Validar email
const isValidEmail = (email) => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

// Validar teléfono argentino
const isValidPhone = (phone) => {
  const regex = /^\+54\s?9?\s?\d{2,4}\s?\d{4}-?\d{4}$/;
  return regex.test(phone);
};

// Validar código postal (4 dígitos)
const isValidPostalCode = (code) => {
  return /^\d{4}$/.test(code);
};

// Validar stock
const hasStock = (product, requestedQuantity = 1) => {
  if (product.stock === undefined) return true; // Servicios
  return product.stock >= requestedQuantity;
};
Status Badge Helpers
javascript// Obtener clase de badge según estado de orden
const getOrderStatusClass = (status) => {
  const classes = {
    'PENDING': 'badge-warning',
    'CONFIRMED': 'badge-success',
    'PREPARING': 'badge-info',
    'READY': 'badge-info',
    'COMPLETED': 'badge-success',
    'CANCELLED': 'badge-danger'
  };
  return `badge ${classes[status] || 'badge-default'}`;
};

// Obtener clase de badge según estado de pago
const getPaymentStatusClass = (status) => {
  const classes = {
    'PENDING': 'badge-warning',
    'APPROVED': 'badge-success',
    'REJECTED': 'badge-danger',
    'CANCELLED': 'badge-danger'
  };
  return `badge ${classes[status] || 'badge-default'}`;
};

// Obtener texto user-friendly de estado
const getOrderStatusText = (status) => {
  const texts = {
    'PENDING': 'Pendiente',
    'CONFIRMED': 'Confirmado',
    'PREPARING': 'En Preparación',
    'READY': 'Listo',
    'COMPLETED': 'Completado',
    'CANCELLED': 'Cancelado'
  };
  return texts[status] || status;
};
🐛 MANEJO DE ERRORES
Try-Catch Pattern
javascriptconst handleAction = async () => {
  try {
    setLoading(true);
    setError('');
    
    const response = await api.post('/endpoint', data);
    
    // Éxito
    showToast('Operación exitosa', 'success');
    onSuccess(response.data);
    
  } catch (err) {
    console.error('Error:', err);
    
    // Extraer mensaje de error
    const errorMsg = err.response?.data?.message ||
                     err.response?.data?.error ||
                     'Error al realizar la operación';
    
    setError(errorMsg);
    showToast(errorMsg, 'error');
    
  } finally {
    setLoading(false);
  }
};
Error Boundaries (Opcional)
javascriptclass ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <h1>Algo salió mal</h1>
          <p>{this.state.error?.message}</p>
          <button onClick={() => window.location.reload()}>
            Recargar página
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

// Uso en main.jsx
<ErrorBoundary>
  <App />
</ErrorBoundary>
```

## 🔄 FLUJOS COMPLETOS

### 1. Flujo de Compra de Producto
```
1. HomePage → Usuario ve ProductCard
2. Click "Agregar al carrito"
   ├─ Validar stock disponible
   ├─ Verificar cantidad en carrito
   └─ addToCart(product, 1)
3. Badge del carrito se actualiza
4. Navigate a /cart
5. CartPage → Revisar items
   ├─ Ajustar cantidades
   └─ Ver resumen con IVA
6. Click "Proceder al Pago"
7. Navigate a /checkout
8. CheckoutPage → Seleccionar método de pago
   ├─ MERCADO_PAGO
   ├─ BANK_TRANSFER
   └─ CASH
9. Click "Confirmar Pago"
   ├─ POST /api/orders → orderId
   └─ POST /api/payments → paymentLink
10. Redirección
    ├─ MERCADO_PAGO → window.location.href = paymentLink
    │   ├─ Usuario paga en MercadoPago
    │   └─ Callback a /success, /failure o /pending
    └─ Otros métodos → clearCart() + navigate('/')
```

### 2. Flujo de Reserva de Servicio
```
1. HomePage → Usuario ve ServiceCard con requiresBooking
2. Click "📅 Reservar"
3. Abrir BookingModal
   ├─ Generar fechas disponibles (próximos 30 días)
   └─ Filtrar por availableDays del servicio
4. Usuario selecciona fecha
5. GET /api/bookings/available?serviceId&date
   └─ Retorna slots con disponibilidad
6. Renderizar grid de slots
   ├─ Mostrar horarios (startTime - endTime)
   └─ Mostrar capacidad (availableSpots/totalCapacity)
7. Usuario selecciona slot
8. Click "Confirmar Reserva"
9. addToCart({ ...service, bookingDate, bookingTime })
10. Toast de confirmación
11. Navigate a /cart
12. CartPage → Ver servicio reservado con fecha/hora
13. Proceed to checkout (igual que productos)
```

### 3. Flujo de Admin - Aprobar Pago
```
1. AdminPage → AdminDashboard carga
2. GET /api/admin/stats/pending-payments
3. Tabla muestra pagos PENDING con comprobante
4. Admin revisa comprobante (click "Ver →")
5. Click "Aprobar"
6. PATCH /api/payments/:id/approve
7. Backend:
   ├─ Actualiza payment.status = 'APPROVED'
   ├─ Actualiza order.status = 'CONFIRMED'
   └─ Envía notificación al cliente (opcional)
8. Toast de éxito
9. Reload dashboard (pago desaparece de pending)
```

### 4. Flujo de Super Admin - Crear Servicio
```
1. SuperAdminPage
2. Click "➕ Crear Servicio"
3. Navigate a /super-admin/create-service
4. GET /api/super-admin/tenants
   └─ Filtrar: active && config.features.services === true
5. Seleccionar tenant del dropdown
6. Llenar formulario de servicio
7. Si requiresBooking === true:
   ├─ Seleccionar días disponibles
   ├─ Definir horario (workStartTime, workEndTime)
   └─ Definir intervalo de turnos
8. Click "Crear Servicio"
9. Validaciones client-side
10. POST /api/super-admin/services
    └─ Incluye tenantId
11. Backend crea servicio para ese tenant específico
12. Toast de éxito
13. Form se limpia para crear otro
```

### 5. Flujo de Cliente - Mi Cuenta
```
1. Navbar → Click "👤 Mi Cuenta"
2. Navigate a /my-account
3. Tab "Overview" (default)
   ├─ GET /api/customer/stats
   └─ Mostrar KPIs: totalOrders, totalSpent, etc.
4. Tab "Mis Órdenes"
   ├─ GET /api/customer/orders
   └─ Listar historial con badges de estado
5. Tab "Mis Reservas"
   ├─ GET /api/customer/bookings
   └─ Mostrar servicios reservados con fecha/hora
   └─ Botón "Cancelar" si status === 'CONFIRMED' && fecha futura
6. Tab "Mi Perfil"
   ├─ Mostrar datos: name, email, phone, role
   ├─ Click "Editar Perfil"
   ├─ Modificar name y phone (email no editable)
   └─ PUT /api/customer/profile
📊 INTEGRACIÓN CON RECHARTS
Configuración de Gráficos
javascriptimport {
  BarChart, Bar,
  LineChart, Line,
  PieChart, Pie,
  Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  ResponsiveContainer
} from 'recharts';

// Colores para gráficos
const COLORS = ['#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981'];
LineChart - Ventas Diarias
javascript<ResponsiveContainer width="100%" height={300}>
  <LineChart data={dailySales}>
    <CartesianGrid strokeDasharray="3 3" />
    <XAxis 
      dataKey="date" 
      tick={{ fontSize: 12 }}
    />
    <YAxis />
    <Tooltip 
      formatter={(value) => `$${value.toFixed(2)}`}
    />
    <Line 
      type="monotone" 
      dataKey="total" 
      stroke="#3B82F6" 
      strokeWidth={2}
      name="Total Ventas"
    />
  </LineChart>
</ResponsiveContainer>

// Formato de datos esperado
dailySales = [
  { date: '2025-01-01', total: 1250.50 },
  { date: '2025-01-02', total: 980.00 },
  ...
]
PieChart - Métodos de Pago
javascript<ResponsiveContainer width="100%" height={300}>
  <PieChart>
    <Pie
      data={paymentMethods}
      cx="50%"
      cy="50%"
      labelLine={false}
      label={({ name, value }) => `${name}: ${value}`}
      outerRadius={80}
      fill="#8884d8"
      dataKey="value"
    >
      {paymentMethods.map((entry, index) => (
        <Cell 
          key={`cell-${index}`} 
          fill={COLORS[index % COLORS.length]} 
        />
      ))}
    </Pie>
    <Tooltip />
  </PieChart>
</ResponsiveContainer>

// Formato de datos esperado
paymentMethods = [
  { name: 'MERCADO_PAGO', value: 45 },
  { name: 'BANK_TRANSFER', value: 30 },
  { name: 'CASH', value: 25 }
]
BarChart - Top Productos
javascript<ResponsiveContainer width="100%" height={300}>
  <BarChart data={topProducts}>
    <CartesianGrid strokeDasharray="3 3" />
    <XAxis 
      dataKey="name" 
      angle={-45} 
      textAnchor="end" 
      height={100}
      tick={{ fontSize: 12 }}
    />
    <YAxis />
    <Tooltip />
    <Bar 
      dataKey="quantity" 
      fill="#8B5CF6" 
      name="Cantidad Vendida" 
    />
  </BarChart>
</ResponsiveContainer>

// Formato de datos esperado
topProducts = [
  { name: 'Remera Nike', quantity: 150 },
  { name: 'Zapatillas Adidas', quantity: 120 },
  ...
]
🔐 SEGURIDAD
Protección de Rutas
javascript// Siempre verificar rol en el componente
useEffect(() => {
  const verifyAccess = async () => {
    try {
      const response = await api.get('/api/auth/me');
      
      if (response.data.role !== 'ADMIN' && 
          response.data.role !== 'VENDEDOR') {
        navigate('/');
        return;
      }
      
      setUser(response.data);
    } catch (error) {
      navigate('/login');
    }
  };
  
  verifyAccess();
}, []);
Sanitización de Inputs
javascript// React por defecto escapa HTML
// Pero nunca usar dangerouslySetInnerHTML con input de usuario

// Validar antes de enviar al backend
const sanitizeInput = (input) => {
  return input.trim().replace(/<script[^>]*>.*?<\/script>/gi, '');
};
CORS y Headers
javascript// Vite proxy (vite.config.js)
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
});

// En producción, el backend debe permitir el origin del frontend
// Access-Control-Allow-Origin: https://mi-dominio.com
🚀 BUILD Y DEPLOYMENT
Variables de Entorno
bash# .env (no commitear)
VITE_API_URL=https://api.midominio.com

# .env.development
VITE_API_URL=http://localhost:8080

# .env.production
VITE_API_URL=https://api.produccion.com
Build para Producción
bash# Instalar dependencias
npm install

# Build
npm run build

# Output en /dist
dist/
├── index.html
├── assets/
│   ├── index-[hash].js
│   ├── index-[hash].css
│   └── ...
Deploy en Vercel (ejemplo)
json// vercel.json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
Deploy en Netlify (ejemplo)
toml# netlify.toml
[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
📝 CHECKLIST DE FEATURES
Funcionalidades Implementadas ✅

 Multi-tenancy con branding dinámico
 Autenticación JWT con roles
 Carrito de compras persistente
 Sistema de productos con stock
 Sistema de servicios con booking
 Reserva de turnos con disponibilidad
 Integración MercadoPago
 Dashboard administrativo con gráficos
 Gestión de items (productos y servicios)
 Panel de Super Admin
 Perfil de cliente con historial
 Notificaciones toast
 Modales de confirmación
 Responsive design completo
 Loading states
 Error handling
 Rutas protegidas por rol

Mejoras Futuras 🔮

 Paginación en listas largas
 Búsqueda y filtros avanzados
 Sistema de favoritos
 Valoraciones y reseñas
 Chat de soporte
 Notificaciones push
 PWA (Progressive Web App)
 Dark mode
 Multiidioma (i18n)
 Analytics dashboard
 Export de reportes (PDF/Excel)
 Sistema de cupones/descuentos
 Programas de fidelidad
 Integración con más pasarelas de pago

🎯 PATRONES Y BEST PRACTICES
Componente Pattern
javascript// 1. Imports
import React, { useState, useEffect } from 'react';
import api from '../services/api';
import Toast from '../components/Toast';
import './MyComponent.css';

// 2. Component definition
export default function MyComponent({ prop1, prop2 }) {
  // 3. State
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);
  
  // 4. Effects
  useEffect(() => {
    loadData();
  }, []);
  
  // 5. Handlers
  const loadData = async () => {
    try {
      setLoading(true);
      const response = await api.get('/endpoint');
      setData(response.data);
    } catch (err) {
      setError('Error loading data');
    } finally {
      setLoading(false);
    }
  };
  
  const handleAction = () => {
    // Logic
  };
  
  // 6. Helpers
  const formatData = (value) => {
    return value.toFixed(2);
  };
  
  // 7. Conditional rendering
  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;
  if (!data) return <div>No data</div>;
  
  // 8. Main render
  return (
    <div className="my-component">
      {/* JSX */}
      {toast && <Toast {...toast} onClose={() => setToast(null)} />}
    </div>
  );
}
API Call Pattern
javascriptconst performAction = async (payload) => {
  try {
    setLoading(true);
    setError('');
    
    // Validaciones client-side
    if (!payload.field) {
      throw new Error('Campo requerido');
    }
    
    // Request
    const response = await api.post('/endpoint', payload);
    
    // Success
    showToast('Operación exitosa', 'success');
    onSuccess(response.data);
    
    // Optional: Refresh data
    await loadData();
    
  } catch (err) {
    console.error('Error:', err);
    
    const errorMsg = err.response?.data?.message ||
                     err.message ||
                     'Error desconocido';
    
    setError(errorMsg);
    showToast(errorMsg, 'error');
    
  } finally {
    setLoading(false);
  }
};
Form Handling Pattern
javascriptconst [formData, setFormData] = useState({
  field1: '',
  field2: '',
  field3: false
});

const handleChange = (e) => {
  const { name, value, type, checked } = e.target;
  setFormData(prev => ({
    ...prev,
    [name]: type === 'checkbox' ? checked : value
  }));
};

const handleSubmit = async (e) => {
  e.preventDefault();
  
  // Validaciones
  if (!formData.field1) {
    setError('Campo requerido');
    return;
  }
  
  // Submit
  await performAction(formData);
  
  // Reset form
  setFormData({ field1: '', field2: '', field3: false });
};

return (
  <form onSubmit={handleSubmit}>
    <input
      name="field1"
      value={formData.field1}
      onChange={handleChange}
      required
    />
    
    <select
      name="field2"
      value={formData.field2}


///HERE OTHER MORE EFFICIENT TO KNOW

# FRONTEND CORE TEMPLATE - COMPRESSED DOC

## 🏗️ STACK
```
React 18.2.0 + Vite 5.0.0 + React Router 7.9.4
axios 1.12.2 | recharts 3.3.0
CSS vanilla (no Tailwind) | Context API (state)
```

## 📁 ESTRUCTURA
```
src/
├── components/     # Reutilizables (Navbar, ProductCard, Toast, Modal)
├── pages/          # Vistas (Home, Cart, Checkout, Admin, SuperAdmin, MyAccount)
├── services/       # api.js (axios + interceptors)
├── store/          # CartContext.jsx (Context API)
├── App.jsx         # Router + Guards + Multi-tenancy config
└── main.jsx        # Entry point
```

## 🎨 DISEÑO
```css
/* Variables dinámicas (App.jsx setea desde backend config) */
--primary-color: #667eea
--secondary-color: #764ba2
--accent-color: dynamic
--font-family: dynamic

/* Gradiente principal */
linear-gradient(135deg, #667eea 0%, #764ba2 100%)

/* Colores semánticos */
Success: #10b981 | Error: #ef4444 | Warning: #f59e0b | Info: #3b82f6
Grays: #f5f7fa → #1e293b

/* Sombras */
Card: 0 4px 15px rgba(0,0,0,0.08)
Hover: 0 12px 25px rgba(0,0,0,0.15)
Modal: 0 20px 60px rgba(0,0,0,0.3)
```

## 🚦 ROUTES
```javascript
PUBLIC: /, /login, /register, /success, /failure, /pending, /404
PROTECTED: /cart, /checkout, /my-account
ADMIN: /admin, /admin/manage-items (ADMIN|VENDEDOR)
SUPER: /super-admin, /super-admin/create-service (SUPER_ADMIN)

// Guards en App.jsx
ProtectedRoute → user ? element : <Navigate to="/login" />
AdminRoute → (ADMIN|VENDEDOR) ? element : <Navigate to="/" />
SuperAdminRoute → SUPER_ADMIN ? element : <Navigate to="/" />
```

## 📡 API SERVICE (services/api.js)
```javascript
// Base config
BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } })

// Multi-tenancy
setTenantHeader(subdomain) → headers['X-Tenant-Subdomain'] = subdomain

// Interceptores
request → Authorization: Bearer ${localStorage.getItem('token')}
response → 401 → localStorage.removeItem('token')

// Servicios exportados
authService: { register, login, getCurrentUser }
itemService: { getProducts, getServices, createProduct, createService }
tenantService: { getCurrentConfig }
```

## 🛒 CART CONTEXT
```javascript
// Estado global
cart: Array<{ id, name, price, quantity, stock?, bookingDate?, bookingTime? }>

// Métodos
addToCart(item, qty) → valida stock, agrega/incrementa
removeFromCart(id)
updateQuantity(id, qty) → valida stock
clearCart()
getTotal() → sum(price * quantity)
getTotalItems() → sum(quantity)

// Persistencia
localStorage auto-save/load en cada cambio
```

## 🧩 COMPONENTES KEY

### Navbar
```javascript
// Condicional por user/role
Logo dinámico desde config
CartBadge con getTotalItems()
CLIENTE → /my-account
ADMIN|VENDEDOR → /admin
SUPER_ADMIN → /super-admin
Hamburger mobile (.navbar-menu.active)
```

### ProductCard
```javascript
Props: { product }
Validaciones:
  - stock === 0 → disabled
  - currentQtyInCart >= stock → warning toast
Badges: stock <= 5 (warning) | stock === 0 (danger)
```

### ServiceCard
```javascript
Props: { service }
requiresBooking ? <BookingModal /> : addToCart()
Muestra: durationMinutes, scheduleType, maxCapacity
```

### BookingModal
```javascript
Props: { service, onClose, onConfirm }
Flujo:
  1. Generar fechas (30 días) filtradas por availableDays
  2. User selecciona fecha
  3. GET /api/bookings/available?serviceId&date
  4. Renderizar slots: { startTime, endTime, availableSpots/totalCapacity }
  5. onConfirm({ date, time })
```

### AdminDashboard
```javascript
// Endpoints
GET /api/admin/stats/dashboard → KPIs
GET /api/admin/stats/daily-sales?days=30 → LineChart
GET /api/admin/stats/top-products?limit=5 → BarChart
GET /api/admin/stats/payment-methods → PieChart
GET /api/admin/stats/order-status → PieChart
GET /api/admin/stats/pending-payments → Tabla

// Recharts
LineChart: ventas diarias (últimos 30 días)
PieChart: métodos de pago + estados de órdenes
BarChart: productos más vendidos
Tabla: Aprobar/Rechazar pagos pendientes
```

### Toast
```javascript
Props: { message, type: 'success'|'error'|'warning'|'info', onClose, duration: 3000 }
Position: fixed top-20 right-20
Auto-close con setTimeout
Iconos: ✓ ✕ ⚠ ℹ
```

### ConfirmModal
```javascript
Props: { isOpen, title, message, onConfirm, onCancel, type: 'warning'|'danger'|'info' }
Overlay + centered content
Buttons: Cancel (gray) | Confirm (colored by type)
```

## 📄 PAGES CRÍTICAS

### HomePage
```javascript
// Carga items según config.features
if (config?.features?.products !== false) → getProducts()
if (config?.features?.services !== false) → getServices()

Hero + Tabs (Todo/Productos/Servicios) + Grid
Empty state si no hay items
```

### CartPage
```javascript
Layout: Grid 2 cols (items + summary)
Items: imagen, info, quantity controls, subtotal, remove
Summary: subtotal + IVA(21%) + total
Validación: updateQuantity valida stock en tiempo real
Botones: clearCart (confirm modal) | checkout | continueShopping
```

### CheckoutPage
```javascript
Flujo:
  1. POST /api/orders { items: [{itemId, quantity}], paymentMethod, notes }
  2. POST /api/payments { orderId, method }
  3. if (MERCADO_PAGO) → window.location.href = paymentLink
     else → clearCart() + navigate('/')

Métodos: MERCADO_PAGO | BANK_TRANSFER | CASH
```

### MyAccountPage (CLIENTE only)
```javascript
Tabs: Overview | Mis Órdenes | Mis Reservas | Mi Perfil

Overview: GET /api/customer/stats → totalOrders, totalSpent, upcomingBookings
Órdenes: GET /api/customer/orders → historial con badges
Reservas: GET /api/customer/bookings → servicios con fecha/hora
  - Cancelar: POST /api/customer/bookings/:id/cancel (si CONFIRMED + fecha futura)
Perfil: PUT /api/customer/profile { name, phone } (email no editable)
```

### AdminPage
```javascript
Header: tenant badge + "Gestionar Items" btn + user info + logout
Body: <AdminDashboard />
Verificación: user.role === 'ADMIN' || 'VENDEDOR'
```

### ManageItemsPage
```javascript
Tabs: Productos | Servicios

Producto:
  { name*, description, price*, category, imageUrl, stock*, sku, type }
  Validación: stock < 0 → error | type=PHYSICAL && stock=0 → warning

Servicio:
  { name*, description, price*, category, imageUrl, durationMinutes*, 
    scheduleType, maxCapacity, requiresBooking }
  
  Si requiresBooking:
    { availableDays*, workStartTime*, workEndTime*, slotIntervalMinutes* }
    Validación: días.length === 0 → error

Endpoint: POST /api/items/products | POST /api/items/services
```

### SuperAdminPage
```javascript
Header: "Platform Management" badge + "Crear Servicio" btn + refresh
Tabla: tenants con subdomain, type, status, createdAt
Acciones:
  - Editar (modal con businessName, type)
  - Toggle status (activo/suspendido)
Endpoint: GET /api/super-admin/tenants
```

### SuperAdminServiceCreationPage
```javascript
Igual a ManageItemsPage PERO:
  1. GET /api/super-admin/tenants → filtrar active && features.services
  2. Dropdown selector de tenant (campo adicional)
  3. POST /api/super-admin/services { tenantId, ...serviceData }

Validación: if (tenants.length === 0) → mensaje "No hay tenants con servicios"
```

### Login/Register
```javascript
// Login
POST /api/auth/login { email, password } → { token }
localStorage.setItem('token', token)
GET /api/auth/me → setUser(userData)
Redirigir según role: ADMIN/VENDEDOR → /admin | otros → /

// Register
POST /api/auth/register { name, email, phone, password }
Auto-login después de registro
Validaciones: email único, password >= 6 chars, password === confirmPassword
```

### MercadoPago Callbacks
```javascript
// SuccessPage
Query: ?payment_id&status=approved&merchant_order_id
clearCart() + mostrar detalles + botones (seguir comprando / ver productos)

// FailurePage
Query: ?payment_id&status=rejected&status_detail
Mapear status_detail a mensajes user-friendly:
  cc_rejected_insufficient_amount → "Fondos insuficientes"
  cc_rejected_bad_filled_security_code → "Código incorrecto"
  cc_rejected_high_risk → "Rechazado por seguridad"
Botones: Intentar nuevamente / Volver al catálogo

// PendingPage
Query: ?payment_id&status=pending
Info: "Pago en proceso" + no cerrar sesión + esperar confirmación
Botones: Volver al catálogo / Actualizar estado
```

## 🔐 AUTH FLOW
```javascript
// App.jsx - Initial load
useEffect(() => {
  const token = localStorage.getItem('token');
  if (token) {
    api.get('/api/auth/me')
      .then(res => setUser(res.data))
      .catch(() => localStorage.removeItem('token'))
      .finally(() => setLoading(false));
  } else setLoading(false);
}, []);

// Login
authService.login({ email, password }) → { token }
localStorage.setItem('token', token)
authService.getCurrentUser() → setUser(userData)
navigate según role

// Logout
localStorage.removeItem('token')
setUser(null)
navigate('/login')

// Interceptor auto-incluye token
request → headers.Authorization = `Bearer ${token}`
```

## 🎭 MULTI-TENANCY
```javascript
// App.jsx - Load config
useEffect(() => {
  api.get('/api/config/current').then(res => {
    const { businessName, config } = res.data;
    
    // CSS vars
    if (config?.primaryColor) 
      document.documentElement.style.setProperty('--primary-color', config.primaryColor);
    if (config?.secondaryColor)
      document.documentElement.style.setProperty('--secondary-color', config.secondaryColor);
    if (config?.fontFamily)
      document.documentElement.style.setProperty('--font-family', config.fontFamily);
    
    // Title & favicon
    document.title = businessName;
    if (config?.favicon) {
      const link = document.querySelector("link[rel*='icon']") || document.createElement('link');
      link.href = config.favicon;
      document.head.appendChild(link);
    }
    
    // Custom CSS
    if (config?.customCssUrl) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = config.customCssUrl;
      document.head.appendChild(link);
    }
    
    // Meta description
    if (config?.businessDescription) {
      let meta = document.querySelector('meta[name="description"]');
      if (!meta) {
        meta = document.createElement('meta');
        meta.name = 'description';
        document.head.appendChild(meta);
      }
      meta.content = config.businessDescription;
    }
  });
}, []);

// Response shape
{
  id, businessName, subdomain, type, active,
  config: {
    primaryColor, secondaryColor, accentColor, fontFamily,
    logo, favicon, customCssUrl, businessDescription,
    features: { products, services, bookings, mercadopago }
  }
}
```

## 📱 RESPONSIVE
```css
/* Breakpoints */
Mobile: < 768px (base)
Tablet: 768px - 1024px
Desktop: > 1024px

/* Navbar mobile */
@media (max-width: 768px) {
  .navbar-toggle { display: flex; }
  .navbar-menu {
    position: fixed;
    right: -100%;
    transition: right 0.3s;
  }
  .navbar-menu.active { right: 0; }
}

/* Grids */
.items-grid {
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}
@media (max-width: 768px) {
  .items-grid { grid-template-columns: 1fr; }
}
```

## 🎨 ANIMACIONES
```css
/* Keyframes principales */
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } }
@keyframes bounce { 40% { transform: translateY(-20px); } }
@keyframes float { 50% { transform: translateY(-20px); } }
@keyframes pulse { 50% { transform: scale(1.05); opacity: 0.8; } }
@keyframes shake { 25% { transform: translateX(-10px); } }
@keyframes fadeIn { from { opacity: 0; } }
@keyframes scaleIn { from { transform: scale(0.9); opacity: 0; } }

/* Transiciones estándar */
.button { transition: all 0.3s ease; }
.button:hover { transform: translateY(-2px); }
input:focus { border-color: #667eea; transition: all 0.3s ease; }
```

## 🛠️ HELPERS
```javascript
// Format
formatPrice = (n) => `$${parseFloat(n).toFixed(2)}`
formatDate = (d) => new Date(d).toLocaleDateString('es-AR', { year: 'numeric', month: 'long', day: 'numeric' })
formatScheduleType = (t) => ({ ON_DEMAND: 'Bajo demanda', SCHEDULED: 'Con turno', RECURRING: 'Recurrente' }[t])

// Validation
isValidEmail = (e) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e)
isValidPhone = (p) => /^\+54\s?9?\s?\d{2,4}\s?\d{4}-?\d{4}$/.test(p)
isValidPostalCode = (c) => /^\d{4}$/.test(c)
hasStock = (product, qty) => product.stock === undefined || product.stock >= qty

// Status badges
getOrderStatusClass = (s) => `badge badge-${({ PENDING: 'warning', CONFIRMED: 'success', COMPLETED: 'success', CANCELLED: 'danger' }[s] || 'default')}`
getOrderStatusText = (s) => ({ PENDING: 'Pendiente', CONFIRMED: 'Confirmado', COMPLETED: 'Completado', CANCELLED: 'Cancelado' }[s])
```

## 🔄 FLUJOS CRÍTICOS

### Compra Producto
```
HomePage → ProductCard.addToCart (valida stock) 
→ CartPage (ajustar qty) 
→ CheckoutPage (método pago) 
→ POST /api/orders + POST /api/payments 
→ MERCADO_PAGO ? redirect : clearCart + home
```

### Reserva Servicio
```
HomePage → ServiceCard (requiresBooking) 
→ BookingModal (selecciona fecha) 
→ GET /api/bookings/available 
→ selecciona slot 
→ addToCart({ ...service, bookingDate, bookingTime }) 
→ checkout normal
```

### Admin Aprobar Pago
```
AdminPage → AdminDashboard 
→ GET /api/admin/stats/pending-payments 
→ Click "Aprobar" 
→ PATCH /api/payments/:id/approve 
→ Reload dashboard
```

### Super Admin Crear Servicio
```
SuperAdminPage → "Crear Servicio" 
→ GET /api/super-admin/tenants (filtrar active + features.services) 
→ Selecciona tenant 
→ Form servicio (incluye tenantId) 
→ POST /api/super-admin/services 
→ Toast + clear form
```

## 🐛 ERROR HANDLING
```javascript
// Try-catch pattern
try {
  setLoading(true);
  setError('');
  const response = await api.post('/endpoint', data);
  showToast('Éxito', 'success');
  onSuccess(response.data);
} catch (err) {
  const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Error desconocido';
  setError(errorMsg);
  showToast(errorMsg, 'error');
} finally {
  setLoading(false);
}
```

## 🚀 BUILD
```bash
# Dev
npm install
npm run dev  # http://localhost:5173

# Build
npm run build  # output: /dist

# Env vars
VITE_API_URL=http://localhost:8080 (dev)
VITE_API_URL=https://api.produccion.com (prod)

# Deploy
Vercel: vercel.json → rewrites: "/*" → "/index.html"
Netlify: netlify.toml → redirects: "/*" → "/index.html" (200)
```

## 📊 RECHARTS CONFIG
```javascript
import { BarChart, LineChart, PieChart, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

// LineChart (ventas diarias)
<LineChart data={[{ date: '2025-01-01', total: 1250.50 }]}>
  <Line dataKey="total" stroke="#3B82F6" />
</LineChart>

// PieChart (métodos de pago)
<PieChart>
  <Pie data={[{ name: 'MERCADO_PAGO', value: 45 }]} dataKey="value">
    {data.map((_, i) => <Cell fill={COLORS[i % 5]} />)}
  </Pie>
</PieChart>

// BarChart (top productos)
<BarChart data={[{ name: 'Remera', quantity: 150 }]}>
  <Bar dataKey="quantity" fill="#8B5CF6" />
</BarChart>

const COLORS = ['#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981'];
```

## ✅ FEATURES CHECKLIST
```
[x] Multi-tenancy dinámico
[x] Auth JWT con roles (CLIENTE, ADMIN, VENDEDOR, SUPER_ADMIN)
[x] Carrito persistente con validación de stock
[x] Productos físicos/digitales
[x] Servicios con sistema de booking
[x] Reserva de turnos con disponibilidad en tiempo real
[x] MercadoPago integration (success/failure/pending)
[x] Dashboard admin con Recharts
[x] CRUD items (productos/servicios)
[x] Panel Super Admin (gestión tenants)
[x] Mi Cuenta (stats, órdenes, reservas, perfil)
[x] Toast notifications
[x] Confirm modals
[x] Responsive mobile-first
[x] Loading states
[x] Error handling
[x] Route guards por rol
```

## 🎯 PATRONES CLAVE
```javascript
// Component pattern
export default function MyComponent({ props }) {
  const [state, setState] = useState(initial);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);
  
  useEffect(() => { loadData(); }, []);
  
  const handleAction = async () => { /* try-catch */ };
  const formatHelper = (v) => { /* format */ };
  
  if (loading) return <Loading />;
  if (error) return <Error />;
  if (!data) return <Empty />;
  
  return <div>{/* JSX */}</div>;
}

// API call pattern
const action = async (payload) => {
  try {
    setLoading(true);
    setError('');
    if (!validate(payload)) throw new Error('Validación falló');
    const res = await api.post('/endpoint', payload);
    showToast('Éxito', 'success');
    onSuccess(res.data);
  } catch (err) {
    const msg = err.response?.data?.message || err.message;
    setError(msg);
    showToast(msg, 'error');
  } finally {
    setLoading(false);
  }
};

// Form handling
const [form, setForm] = useState({});
const handleChange = (e) => {
  const { name, value, type, checked } = e.target;
  setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
};
const handleSubmit = async (e) => {
  e.preventDefault();
  await action(form);
  setForm({});
};
```

---

## 💾 COPIAR Y PEGAR EN NUEVAS CONVERSACIONES

Para usar este doc en futuras conversaciones con Claude:

1. Copiar TODO este archivo
2. En nueva conversación: "Este es el frontend de mi proyecto: [PEGAR CONTENIDO]"
3. Claude interpretará la estructura completa
4. Luego preguntar lo que necesites: "Quiero agregar X feature" / "Cómo implemento Y"

**IMPORTANTE:** Este doc asume que ya tienes el backend corriendo. Si necesitas ambos, pasa también el `BackendParaClaude.md`.