import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Toast from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import AddressManager from '../components/AddressManager';
import './MyAccountPage.css';

export default function MyAccountPage() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('overview'); // overview, orders, bookings, profile
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState(null);
    const [orders, setOrders] = useState([]);
    const [bookings, setBookings] = useState([]);
    const [profile, setProfile] = useState(null);
    const [editingProfile, setEditingProfile] = useState(false);
    const [profileForm, setProfileForm] = useState({ name: '', phone: '' });
    const [toast, setToast] = useState(null);
    const [confirmCancel, setConfirmCancel] = useState(null);

    useEffect(() => {
        loadData();
    }, [activeTab]);

    const loadData = async () => {
        try {
            setLoading(true);

            if (activeTab === 'overview') {
                const [statsRes, profileRes] = await Promise.all([
                    api.get('/api/customer/stats'),
                    api.get('/api/customer/profile')
                ]);
                setStats(statsRes.data);
                setProfile(profileRes.data);
            } else if (activeTab === 'orders') {
                const ordersRes = await api.get('/api/customer/orders');
                setOrders(ordersRes.data);
            } else if (activeTab === 'bookings') {
                const bookingsRes = await api.get('/api/customer/bookings');
                setBookings(bookingsRes.data);
            } else if (activeTab === 'profile') {
                const profileRes = await api.get('/api/customer/profile');
                setProfile(profileRes.data);
                setProfileForm({ name: profileRes.data.name, phone: profileRes.data.phone || '' });
            }
        } catch (err) {
            console.error('Error loading data:', err);
            showToast('Error cargando datos', 'error');
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        try {
            const response = await api.put('/api/customer/profile', profileForm);
            setProfile(response.data);
            setEditingProfile(false);
            showToast('Perfil actualizado exitosamente', 'success');
        } catch (err) {
            showToast('Error al actualizar perfil', 'error');
        }
    };

    const handleCancelBooking = (booking) => {
        setConfirmCancel({
            title: '¿Cancelar Reserva?',
            message: `¿Estás seguro de cancelar tu reserva de "${booking.serviceName}" para el ${new Date(booking.bookingDate).toLocaleDateString()}?`,
            type: 'warning',
            bookingId: booking.id
        });
    };

    const confirmCancelBooking = async () => {
        try {
            await api.post(`/api/customer/bookings/${confirmCancel.bookingId}/cancel`, {
                reason: 'Cancelado por el cliente'
            });
            showToast('Reserva cancelada exitosamente', 'success');
            loadData();
        } catch (err) {
            const errorMsg = err.response?.data?.error || 'Error al cancelar reserva';
            showToast(errorMsg, 'error');
        }
        setConfirmCancel(null);
    };

    const showToast = (message, type) => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

    const getStatusBadgeClass = (status) => {
        const classes = {
            'PENDING': 'badge-warning',
            'CONFIRMED': 'badge-success',
            'PREPARING': 'badge-info',
            'READY': 'badge-info',
            'COMPLETED': 'badge-success',
            'CANCELLED': 'badge-danger'
        };
        return classes[status] || 'badge-default';
    };

    if (loading) {
        return <div className="account-loading">Cargando...</div>;
    }

    return (
        <div className="my-account-page">
            {/* Header */}
            <div className="account-header">
                <div className="header-content">
                    <h1>Mi Cuenta</h1>
                    <p>Bienvenido de vuelta, {profile?.name}</p>
                </div>
            </div>

            {/* Tabs */}
            <div className="account-tabs">
                <button
                    className={`tab ${activeTab === 'overview' ? 'active' : ''}`}
                    onClick={() => setActiveTab('overview')}
                >
                    📊 Resumen
                </button>
                <button
                    className={`tab ${activeTab === 'orders' ? 'active' : ''}`}
                    onClick={() => setActiveTab('orders')}
                >
                    🛒 Mis Órdenes
                </button>
                <button
                    className={`tab ${activeTab === 'bookings' ? 'active' : ''}`}
                    onClick={() => setActiveTab('bookings')}
                >
                    📅 Mis Reservas
                </button>
                <button
                    className={`tab ${activeTab === 'addresses' ? 'active' : ''}`}
                    onClick={() => setActiveTab('addresses')}
                >
                    📍 Direcciones
                </button>
                <button
                    className={`tab ${activeTab === 'profile' ? 'active' : ''}`}
                    onClick={() => setActiveTab('profile')}
                >
                    👤 Mi Perfil
                </button>
            </div>

            {/* Content */}
            <div className="account-content">
                {/* Overview Tab */}
                {activeTab === 'overview' && stats && (
                    <div className="overview-section">
                        <div className="stats-grid">
                            <div className="stat-card">
                                <div className="stat-icon">🛒</div>
                                <div className="stat-info">
                                    <h3>{stats.totalOrders}</h3>
                                    <p>Órdenes Totales</p>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon">✅</div>
                                <div className="stat-info">
                                    <h3>{stats.completedOrders}</h3>
                                    <p>Completadas</p>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon">💰</div>
                                <div className="stat-info">
                                    <h3>${parseFloat(stats.totalSpent).toFixed(2)}</h3>
                                    <p>Total Gastado</p>
                                </div>
                            </div>
                            <div className="stat-card">
                                <div className="stat-icon">📅</div>
                                <div className="stat-info">
                                    <h3>{stats.upcomingBookings}</h3>
                                    <p>Reservas Próximas</p>
                                </div>
                            </div>
                        </div>

                        <div className="quick-actions">
                            <h2>Acciones Rápidas</h2>
                            <div className="actions-grid">
                                <button onClick={() => navigate('/')} className="action-btn">
                                    🛍️ Seguir Comprando
                                </button>
                                <button onClick={() => setActiveTab('orders')} className="action-btn">
                                    📦 Ver Mis Órdenes
                                </button>
                                <button onClick={() => setActiveTab('bookings')} className="action-btn">
                                    🗓️ Ver Mis Reservas
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Orders Tab */}
                {activeTab === 'orders' && (
                    <div className="orders-section">
                        <h2>Mis Órdenes ({orders.length})</h2>
                        {orders.length === 0 ? (
                            <div className="empty-state">
                                <p>No tienes órdenes aún</p>
                                <button onClick={() => navigate('/')} className="btn-primary">
                                    Comenzar a Comprar
                                </button>
                            </div>
                        ) : (
                            <div className="orders-list">
                                {orders.map(order => (
                                    <div key={order.id} className="order-card">
                                        <div className="order-header">
                                            <div>
                                                <h3>Orden #{order.id.substring(0, 8)}</h3>
                                                <p className="order-date">
                                                    {new Date(order.createdAt).toLocaleDateString('es-AR', {
                                                        year: 'numeric',
                                                        month: 'long',
                                                        day: 'numeric'
                                                    })}
                                                </p>
                                            </div>
                                            <span className={`badge ${getStatusBadgeClass(order.status)}`}>
                                                {order.status}
                                            </span>
                                        </div>
                                        <div className="order-body">
                                            <div className="order-total">
                                                <span>Total:</span>
                                                <strong>${parseFloat(order.total).toFixed(2)}</strong>
                                            </div>
                                            <div className="order-method">
                                                <span>Método de pago:</span>
                                                <span>{order.paymentMethod}</span>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Bookings Tab */}
                {activeTab === 'bookings' && (
                    <div className="bookings-section">
                        <h2>Mis Reservas ({bookings.length})</h2>
                        {bookings.length === 0 ? (
                            <div className="empty-state">
                                <p>No tienes reservas aún</p>
                                <button onClick={() => navigate('/')} className="btn-primary">
                                    Ver Servicios
                                </button>
                            </div>
                        ) : (
                            <div className="bookings-list">
                                {bookings.map(booking => (
                                    <div key={booking.id} className="booking-card">
                                        <div className="booking-image">
                                            {booking.serviceImageUrl ? (
                                                <img src={booking.serviceImageUrl} alt={booking.serviceName} />
                                            ) : (
                                                <div className="no-image">⚙️</div>
                                            )}
                                        </div>
                                        <div className="booking-info">
                                            <h3>{booking.serviceName}</h3>
                                            <div className="booking-details">
                                                <p>📅 {new Date(booking.bookingDate).toLocaleDateString('es-AR')}</p>
                                                <p>🕒 {booking.startTime} - {booking.endTime}</p>
                                                <p>⏱️ {booking.serviceDuration} minutos</p>
                                            </div>
                                            <span className={`badge ${getStatusBadgeClass(booking.status)}`}>
                                                {booking.status}
                                            </span>
                                        </div>
                                        <div className="booking-actions">
                                            {(booking.status === 'CONFIRMED' || booking.status === 'PENDING') &&
                                                new Date(booking.bookingDate) > new Date() && (
                                                    <button
                                                        onClick={() => handleCancelBooking(booking)}
                                                        className="btn-cancel-booking"
                                                    >
                                                        Cancelar
                                                    </button>
                                                )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* Addresses Tab */}
                {activeTab === 'addresses' && (
                    <div className="addresses-section">
                        <AddressManager />
                    </div>
                )}

                {/* Profile Tab */}
                {activeTab === 'profile' && profile && (
                    <div className="profile-section">
                        <h2>Mi Perfil</h2>
                        {!editingProfile ? (
                            <div className="profile-card">
                                <div className="profile-field">
                                    <label>Nombre:</label>
                                    <span>{profile.name}</span>
                                </div>
                                <div className="profile-field">
                                    <label>Email:</label>
                                    <span>{profile.email}</span>
                                </div>
                                <div className="profile-field">
                                    <label>Teléfono:</label>
                                    <span>{profile.phone || 'No especificado'}</span>
                                </div>
                                <div className="profile-field">
                                    <label>Rol:</label>
                                    <span className="badge badge-info">{profile.role}</span>
                                </div>
                                <button onClick={() => setEditingProfile(true)} className="btn-edit">
                                    Editar Perfil
                                </button>
                            </div>
                        ) : (
                            <form onSubmit={handleUpdateProfile} className="profile-form">
                                <div className="form-group">
                                    <label>Nombre *</label>
                                    <input
                                        type="text"
                                        value={profileForm.name}
                                        onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Teléfono</label>
                                    <input
                                        type="tel"
                                        value={profileForm.phone}
                                        onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                                        placeholder="+54 9 11 1234-5678"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Email (no editable)</label>
                                    <input
                                        type="email"
                                        value={profile.email}
                                        disabled
                                        className="input-disabled"
                                    />
                                    <small className="help-text">Por seguridad, el email no se puede modificar</small>
                                </div>
                                <div className="form-actions">
                                    <button type="button" onClick={() => setEditingProfile(false)} className="btn-cancel">
                                        Cancelar
                                    </button>
                                    <button type="submit" className="btn-save">
                                        Guardar Cambios
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                )}
            </div>

            {/* Modals */}
            {confirmCancel && (
                <ConfirmModal
                    isOpen={true}
                    title={confirmCancel.title}
                    message={confirmCancel.message}
                    type={confirmCancel.type}
                    onConfirm={confirmCancelBooking}
                    onCancel={() => setConfirmCancel(null)}
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