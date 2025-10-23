import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import AdminDashboard from '../components/AdminDashboard';
import './AdminPage.css';

export default function AdminPage() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [isAuthorized, setIsAuthorized] = useState(false);

    useEffect(() => {
        verifyAdminAccess();
    }, []);

    const verifyAdminAccess = async () => {
        try {
            const response = await api.get('/api/auth/me');
            const userData = response.data;

            // Verificar que el usuario sea ADMIN o VENDEDOR
            if (userData.role === 'ADMIN' || userData.role === 'VENDEDOR') {
                setUser(userData);
                setIsAuthorized(true);
            } else {
                setIsAuthorized(false);
                navigate('/');
            }
        } catch (error) {
            console.error('Error verifying admin access:', error);
            navigate('/login');
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/login');
    };

    if (loading) {
        return <div className="admin-page-loading">Verificando acceso...</div>;
    }

    if (!isAuthorized) {
        return <div className="admin-page-error">Acceso denegado. Se requieren permisos de administrador.</div>;
    }

    return (
        <div className="admin-page">
            {/* Header */}
            <header className="admin-header">
                <div className="admin-header-content">
                    <div className="header-left">
                        <h1>Portal Administrativo</h1>
                        <span className="tenant-badge">{user?.tenantName}</span>
                    </div>
                    <div className="header-right">
                        <div className="user-info">
                            <span className="user-name">{user?.name}</span>
                            <span className="user-role">{user?.role}</span>
                        </div>
                        <button onClick={handleLogout} className="btn-logout">
                            Cerrar Sesión
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="admin-main">
                <AdminDashboard />
            </main>

            {/* Footer */}
            <footer className="admin-footer">
                <p>&copy; 2025 Sistema de E-commerce Core. Todos los derechos reservados.</p>
            </footer>
        </div>
    );
}