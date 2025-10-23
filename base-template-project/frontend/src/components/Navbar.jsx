
import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../store/CartContext';
import api from '../services/api';
import './Navbar.css';

export default function Navbar({ user, setUser }) {
    const navigate = useNavigate();
    const { getTotalItems } = useCart();
    const [menuOpen, setMenuOpen] = useState(false);
    const [config, setConfig] = useState(null);

    useEffect(() => {
        // Cargar config para obtener logo
        api.get('/api/config/current')
            .then(response => setConfig(response.data))
            .catch(err => console.error('Error loading config:', err));
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('token');
        setUser(null);
        navigate('/');
    };

    const toggleMenu = () => {
        setMenuOpen(!menuOpen);
    };

    return (
        <nav className="navbar">
            <div className="navbar-container">
                {/* Logo */}
                <Link to="/" className="navbar-logo">
                    {config?.config?.logo ? (
                        <img
                            src={config.config.logo}
                            alt={config.businessName}
                            className="navbar-logo-img"
                        />
                    ) : (
                        <span>🛒 {config?.businessName || 'Mi Tienda'}</span>
                    )}
                </Link>

                {/* Hamburger Menu (mobile) */}
                <button className="navbar-toggle" onClick={toggleMenu}>
                    <span></span>
                    <span></span>
                    <span></span>
                </button>

                {/* Nav Links */}
                <div className={`navbar-menu ${menuOpen ? 'active' : ''}`}>
                    <Link to="/" className="navbar-link" onClick={() => setMenuOpen(false)}>
                        Inicio
                    </Link>

                    {user ? (
                        <>
                            <Link to="/cart" className="navbar-link navbar-cart" onClick={() => setMenuOpen(false)}>
                                🛒 Carrito
                                {getTotalItems() > 0 && (
                                    <span className="cart-badge">{getTotalItems()}</span>
                                )}
                            </Link>

                            {/* Admin/Vendedor Link */}
                            {(user.role === 'ADMIN' || user.role === 'VENDEDOR') && (
                                <Link to="/admin" className="navbar-link navbar-admin" onClick={() => setMenuOpen(false)}>
                                    📊 Admin
                                </Link>
                            )}

                            {/* User Menu */}
                            <div className="navbar-user">
                                <span className="navbar-username">Hola, {user.name}</span>
                                <button onClick={handleLogout} className="navbar-logout">
                                    Salir
                                </button>
                            </div>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="navbar-link" onClick={() => setMenuOpen(false)}>
                                Iniciar Sesión
                            </Link>
                            <Link to="/register" className="navbar-link navbar-register" onClick={() => setMenuOpen(false)}>
                                Registrarse
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
}