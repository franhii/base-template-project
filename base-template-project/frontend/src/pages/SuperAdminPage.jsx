import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import EditTenantModal from '../components/EditTenantModal';
import ConfirmModal from '../components/ConfirmModal';
import Toast from '../components/Toast';
import './SuperAdminPage.css';

export default function SuperAdminPage() {
    const navigate = useNavigate();
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [user, setUser] = useState(null);
    const [editingTenant, setEditingTenant] = useState(null);
    const [confirmAction, setConfirmAction] = useState(null);
    const [toast, setToast] = useState(null);

    useEffect(() => {
        verifyAccess();
    }, []);

    const verifyAccess = async () => {
        try {
            const response = await api.get('/api/auth/me');
            const userData = response.data;

            if (userData.role !== 'SUPER_ADMIN') {
                navigate('/');
                return;
            }

            setUser(userData);
            loadTenants();
        } catch (error) {
            console.error('Error verifying access:', error);
            navigate('/login');
        }
    };

    const loadTenants = async () => {
        try {
            setLoading(true);
            const response = await api.get('/api/super-admin/tenants');
            setTenants(response.data);
            setError(null);
        } catch (err) {
            console.error('Error loading tenants:', err);
            setError('Error cargando tenants');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleStatus = async (tenant) => {
        setConfirmAction({
            title: tenant.active ? '¿Suspender Tenant?' : '¿Activar Tenant?',
            message: tenant.active
                ? `El tenant "${tenant.businessName}" será suspendido y no podrá acceder a la plataforma.`
                : `El tenant "${tenant.businessName}" será reactivado.`,
            type: tenant.active ? 'danger' : 'info',
            onConfirm: async () => {
                try {
                    await api.patch(`/api/super-admin/tenants/${tenant.id}/toggle-status`);
                    showToast(
                        tenant.active ? 'Tenant suspendido' : 'Tenant activado',
                        'success'
                    );
                    loadTenants();
                } catch (err) {
                    showToast('Error al cambiar estado', 'error');
                }
                setConfirmAction(null);
            }
        });
    };

    const handleEdit = (tenant) => {
        setEditingTenant(tenant);
    };

    const handleSaveEdit = async (updatedTenant) => {
        try {
            await api.put(`/api/super-admin/tenants/${updatedTenant.id}`, updatedTenant);
            showToast('Tenant actualizado exitosamente', 'success');
            loadTenants();
            setEditingTenant(null);
        } catch (err) {
            showToast('Error al actualizar tenant', 'error');
        }
    };

    const showToast = (message, type) => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        navigate('/login');
    };

    if (loading) {
        return <div className="super-admin-loading">Cargando...</div>;
    }

    if (error) {
        return <div className="super-admin-error">{error}</div>;
    }

    return (
        <div className="super-admin-page">
            {/* Header */}
            <header className="super-admin-header">
                <div className="header-content">
                    <div className="header-left">
                        <h1>🔐 Super Admin Panel</h1>
                        <span className="platform-badge">Platform Management</span>
                    </div>
                    <div className="header-right">
                        <div className="user-info">
                            <span className="user-name">{user?.name}</span>
                            <span className="user-role">Super Admin</span>
                        </div>
                        <button onClick={handleLogout} className="btn-logout">
                            Cerrar Sesión
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="super-admin-main">
                <div className="tenants-header">
                    <h2>Gestión de Tenants ({tenants.length})</h2>
                    <button className="btn-refresh" onClick={loadTenants}>
                        🔄 Actualizar
                    </button>
                </div>

                {/* Tenants Table */}
                <div className="tenants-table-container">
                    <table className="tenants-table">
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
                            <tr key={tenant.id} className={!tenant.active ? 'tenant-suspended' : ''}>
                                <td>
                                    <div className="tenant-name">
                                        {tenant.businessName}
                                        {!tenant.active && <span className="suspended-badge">Suspendido</span>}
                                    </div>
                                </td>
                                <td>
                                    <code className="subdomain">{tenant.subdomain}</code>
                                </td>
                                <td>
                                    <span className="type-badge">{tenant.type}</span>
                                </td>
                                <td>
                                        <span className={`status-badge ${tenant.active ? 'active' : 'inactive'}`}>
                                            {tenant.active ? '✅ Activo' : '⛔ Suspendido'}
                                        </span>
                                </td>
                                <td className="date-cell">
                                    {new Date(tenant.createdAt).toLocaleDateString('es-AR', {
                                        year: 'numeric',
                                        month: 'short',
                                        day: 'numeric'
                                    })}
                                </td>
                                <td>
                                    <div className="action-buttons">
                                        <button
                                            onClick={() => handleEdit(tenant)}
                                            className="btn-action btn-edit"
                                            title="Editar"
                                        >
                                            ✏️
                                        </button>
                                        <button
                                            onClick={() => handleToggleStatus(tenant)}
                                            className={`btn-action ${tenant.active ? 'btn-suspend' : 'btn-activate'}`}
                                            title={tenant.active ? 'Suspender' : 'Activar'}
                                        >
                                            {tenant.active ? '⛔' : '✅'}
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </main>

            {/* Modals */}
            {editingTenant && (
                <EditTenantModal
                    tenant={editingTenant}
                    onClose={() => setEditingTenant(null)}
                    onSave={handleSaveEdit}
                />
            )}

            {confirmAction && (
                <ConfirmModal
                    isOpen={true}
                    title={confirmAction.title}
                    message={confirmAction.message}
                    type={confirmAction.type}
                    onConfirm={confirmAction.onConfirm}
                    onCancel={() => setConfirmAction(null)}
                />
            )}

            {toast && (
                <Toast
                    message={toast.message}
                    type={toast.type}
                    onClose={() => setToast(null)}
                />
            )}
        </div>
    );
}