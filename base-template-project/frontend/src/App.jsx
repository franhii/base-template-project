import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import api from './services/api';
import { CartProvider } from './store/CartContext';

// Components
import Navbar from './components/Navbar';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import AdminPage from './pages/AdminPage';
import {ManageItemsPage} from './pages/ManageItemsPage';
import NotFoundPage from './pages/NotFoundPage';
import SuccessPage from './pages/SuccessPage';
import FailurePage from './pages/FailurePage';
import PendingPage from './pages/PendingPage';

import './App.css';

export default function App() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [config, setConfig] = useState(null);

    useEffect(() => {
        // Cargar configuración del tenant
        api.get('/api/config/current')
            .then(response => {
                setConfig(response.data);
                const tenantConfig = response.data.config;

                // ========== Aplicar branding dinámico ==========

                // Colores
                if (tenantConfig?.primaryColor) {
                    document.documentElement.style.setProperty(
                        '--primary-color',
                        tenantConfig.primaryColor
                    );
                }
                if (tenantConfig?.secondaryColor) {
                    document.documentElement.style.setProperty(
                        '--secondary-color',
                        tenantConfig.secondaryColor
                    );
                }
                if (tenantConfig?.accentColor) {
                    document.documentElement.style.setProperty(
                        '--accent-color',
                        tenantConfig.accentColor
                    );
                }

                // Fuente
                if (tenantConfig?.fontFamily) {
                    document.documentElement.style.setProperty(
                        '--font-family',
                        tenantConfig.fontFamily
                    );
                }

                // Título y favicon
                if (response.data.businessName) {
                    document.title = response.data.businessName;
                }
                if (tenantConfig?.favicon) {
                    const link = document.querySelector("link[rel*='icon']") || document.createElement('link');
                    link.type = 'image/x-icon';
                    link.rel = 'shortcut icon';
                    link.href = tenantConfig.favicon;
                    document.getElementsByTagName('head')[0].appendChild(link);
                }

                // CSS Custom (opcional)
                if (tenantConfig?.customCssUrl) {
                    const customCss = document.createElement('link');
                    customCss.rel = 'stylesheet';
                    customCss.href = tenantConfig.customCssUrl;
                    document.head.appendChild(customCss);
                }

                // Meta description para SEO
                if (tenantConfig?.businessDescription) {
                    let metaDescription = document.querySelector('meta[name="description"]');
                    if (!metaDescription) {
                        metaDescription = document.createElement('meta');
                        metaDescription.name = 'description';
                        document.head.appendChild(metaDescription);
                    }
                    metaDescription.content = tenantConfig.businessDescription;
                }
            })
            .catch(err => console.error('Error loading tenant config:', err));

        // Verificar si hay usuario autenticado
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

    // ProtectedRoute - Solo usuarios autenticados
    const ProtectedRoute = ({ element }) => {
        if (loading) return <div className="app-loading"><div className="spinner"></div><p>Cargando...</p></div>;
        return user ? element : <Navigate to="/login" />;
    };

    // AdminRoute - Solo ADMIN y VENDEDOR
    const AdminRoute = ({ element }) => {
        if (loading) return <div className="app-loading"><div className="spinner"></div><p>Cargando...</p></div>;
        if (!user) return <Navigate to="/login" />;
        if (user.role !== 'ADMIN' && user.role !== 'VENDEDOR') {
            return <Navigate to="/" />;
        }
        return element;
    };

    if (loading) {
        return (
            <div className="app-loading">
                <div className="spinner"></div>
                <p>Cargando aplicación...</p>
            </div>
        );
    }

    return (
        <CartProvider>
            <Router>
                <div className="app">
                    <Navbar user={user} setUser={setUser} />

                    <main className="main-content">
                        <Routes>
                            {/* Rutas Públicas */}
                            <Route path="/" element={<HomePage config={config} />} />
                            <Route
                                path="/login"
                                element={
                                    user ? <Navigate to="/" /> :
                                        <LoginPage setUser={setUser} />
                                }
                            />
                            <Route
                                path="/register"
                                element={
                                    user ? <Navigate to="/" /> :
                                        <RegisterPage setUser={setUser} />
                                }
                            />

                            {/* Rutas Protegidas - Cliente */}
                            <Route
                                path="/cart"
                                element={<ProtectedRoute element={<CartPage />} />}
                            />
                            <Route
                                path="/checkout"
                                element={<ProtectedRoute element={<CheckoutPage />} />}
                            />

                            {/* Rutas Protegidas - Admin/Vendedor */}
                            <Route
                                path="/admin"
                                element={<AdminRoute element={<AdminPage />} />}
                            />
                            <Route
                                path="/admin/manage-items"
                                element={<AdminRoute element={<ManageItemsPage />} />}
                            />

                            {/* 404 */}
                            <Route path="*" element={<NotFoundPage />} />
                        </Routes>
                        <Routes>
                            {/* Rutas Públicas */}
                            <Route path="/" element={<HomePage config={config} />} />
                            <Route path="/login" element={user ? <Navigate to="/" /> : <LoginPage setUser={setUser} />} />
                            <Route path="/register" element={user ? <Navigate to="/" /> : <RegisterPage setUser={setUser} />} />

                            {/* Rutas de Mercado Pago - AGREGAR ESTAS */}
                            <Route path="/success" element={<SuccessPage />} />
                            <Route path="/failure" element={<FailurePage />} />
                            <Route path="/pending" element={<PendingPage />} />

                            {/* Rutas Protegidas - Cliente */}
                            <Route path="/cart" element={<ProtectedRoute element={<CartPage />} />} />
                            <Route path="/checkout" element={<ProtectedRoute element={<CheckoutPage />} />} />

                            {/* Rutas Protegidas - Admin/Vendedor */}
                            <Route path="/admin" element={<AdminRoute element={<AdminPage />} />} />
                            <Route path="/admin/manage-items" element={<AdminRoute element={<ManageItemsPage />} />} />

                            {/* 404 */}
                            <Route path="*" element={<NotFoundPage />} />
                        </Routes>
                    </main>
                </div>
            </Router>
        </CartProvider>
    );
}