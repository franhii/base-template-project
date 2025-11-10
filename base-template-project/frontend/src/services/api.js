import axios from 'axios';

// URL base del backend (configurable por variable de entorno)
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Función para setear el header del tenant
export function setTenantHeader(subdomain) {
    if (subdomain) {
        api.defaults.headers.common['X-Tenant-Subdomain'] = subdomain;
        console.log('✅ Header X-Tenant-Subdomain seteado:', subdomain);
    } else {
        delete api.defaults.headers.common['X-Tenant-Subdomain'];
        console.log('❌ Header X-Tenant-Subdomain eliminado');
    }
}
// Interceptor para agregar token JWT automáticamente
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Interceptor para manejar errores de autenticación
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            // No redirigir automáticamente para evitar loop infinito
            console.warn('Unauthorized - token may be expired');
        }
        return Promise.reject(error);
    }
);

export default api;

// ========== Servicios específicos ==========

export const authService = {
    register: (data) => api.post('/api/auth/register', data),
    login: (data) => api.post('/api/auth/login', data),
    getCurrentUser: () => api.get('/api/auth/me'),
};

export const itemService = {
    getProducts: () => api.get('/api/items/products'),
    getServices: () => api.get('/api/items/services'),
    getProduct: (id) => api.get(`/api/items/products/${id}`),
    getService: (id) => api.get(`/api/items/services/${id}`),
    createProduct: (data) => api.post('/api/items/products', data),
    createService: (data) => api.post('/api/items/services', data),
};

export const tenantService = {
    getCurrentConfig: () => api.get('/api/config/current'),
};