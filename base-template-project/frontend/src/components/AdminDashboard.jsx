import React, { useEffect, useState } from 'react';
import api from '../services/api';
import {
    BarChart, Bar,
    LineChart, Line,
    PieChart, Pie,
    Cell,
    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import './AdminDashboard.css';

export default function AdminDashboard() {
    const [stats, setStats] = useState(null);
    const [dailySales, setDailySales] = useState([]);
    const [topProducts, setTopProducts] = useState([]);
    const [paymentMethods, setPaymentMethods] = useState([]);
    const [orderStatus, setOrderStatus] = useState([]);
    const [pendingPayments, setPendingPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);

            // Cargar todas las estadísticas en paralelo
            const [statsRes, salesRes, productsRes, methodsRes, statusRes, paymentsRes] = await Promise.all([
                api.get('/api/admin/stats/dashboard'),
                api.get('/api/admin/stats/daily-sales?days=30'),
                api.get('/api/admin/stats/top-products?limit=5'),
                api.get('/api/admin/stats/payment-methods'),
                api.get('/api/admin/stats/order-status'),
                api.get('/api/admin/stats/pending-payments')
            ]);

            setStats(statsRes.data);
            setDailySales(salesRes.data);
            setTopProducts(productsRes.data);
            setPaymentMethods(Object.entries(methodsRes.data).map(([key, value]) => ({
                name: key,
                value: value
            })));
            setOrderStatus(Object.entries(statusRes.data).map(([key, value]) => ({
                name: key,
                value: value
            })));
            setPendingPayments(paymentsRes.data);
            setError(null);
        } catch (err) {
            console.error('Error loading dashboard:', err);
            setError('Error cargando datos del dashboard');
        } finally {
            setLoading(false);
        }
    };

    const handleApprovePayment = async (paymentId) => {
        try {
            await api.patch(`/api/payments/${paymentId}/approve`);
            alert('Pago aprobado exitosamente');
            loadDashboardData(); // Recargar datos
        } catch (err) {
            console.error('Error approving payment:', err);
            alert('Error al aprobar el pago');
        }
    };

    const handleRejectPayment = async (paymentId) => {
        const reason = prompt('Motivo del rechazo:');
        if (!reason) return;

        try {
            await api.patch(`/api/payments/${paymentId}/reject`, { reason });
            alert('Pago rechazado');
            loadDashboardData(); // Recargar datos
        } catch (err) {
            console.error('Error rejecting payment:', err);
            alert('Error al rechazar el pago');
        }
    };

    if (loading) return <div className="admin-loading">Cargando dashboard...</div>;
    if (error) return <div className="admin-error">{error}</div>;
    if (!stats) return <div className="admin-error">No hay datos disponibles</div>;

    const COLORS = ['#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981'];

    return (
        <div className="admin-dashboard">
            <h1>Dashboard Administrativo</h1>

            {/* KPIs Principales */}
            <div className="kpi-grid">
                <div className="kpi-card">
                    <h3>Ventas Totales</h3>
                    <p className="kpi-value">${stats.totalSales.toFixed(2)}</p>
                    <span className="kpi-label">{stats.totalOrders} órdenes</span>
                </div>

                <div className="kpi-card">
                    <h3>Ingresos Mes</h3>
                    <p className="kpi-value">${stats.monthlyRevenue.toFixed(2)}</p>
                    <span className="kpi-label">Este mes</span>
                </div>

                <div className="kpi-card">
                    <h3>Ticket Promedio</h3>
                    <p className="kpi-value">${stats.averageTicket.toFixed(2)}</p>
                    <span className="kpi-label">por compra</span>
                </div>

                <div className="kpi-card">
                    <h3>Clientes Activos</h3>
                    <p className="kpi-value">{stats.activeCustomers}</p>
                    <span className="kpi-label">han comprado</span>
                </div>

                <div className="kpi-card alert">
                    <h3>Órdenes Pendientes</h3>
                    <p className="kpi-value">{stats.pendingOrders}</p>
                    <span className="kpi-label">sin confirmar</span>
                </div>

                <div className="kpi-card alert">
                    <h3>Pagos Pendientes</h3>
                    <p className="kpi-value">{stats.pendingPayments}</p>
                    <span className="kpi-label">en revisión</span>
                </div>
            </div>

            {/* Gráficos */}
            <div className="charts-grid">
                {/* Ventas Diarias */}
                <div className="chart-card">
                    <h3>Ventas Últimos 30 Días</h3>
                    <ResponsiveContainer width="100%" height={300}>
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
                </div>

                {/* Métodos de Pago */}
                <div className="chart-card">
                    <h3>Distribución de Métodos de Pago</h3>
                    <ResponsiveContainer width="100%" height={300}>
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
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                            <Tooltip />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* Estados de Órdenes */}
                <div className="chart-card">
                    <h3>Estados de Órdenes</h3>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={orderStatus}
                                cx="50%"
                                cy="50%"
                                labelLine={false}
                                label={({ name, value }) => `${name}: ${value}`}
                                outerRadius={80}
                                fill="#8884d8"
                                dataKey="value"
                            >
                                {orderStatus.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                ))}
                            </Pie>
                            <Tooltip />
                        </PieChart>
                    </ResponsiveContainer>
                </div>

                {/* Productos Top */}
                <div className="chart-card">
                    <h3>Productos Más Vendidos</h3>
                    <ResponsiveContainer width="100%" height={300}>
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
                            <Bar dataKey="quantity" fill="#8B5CF6" name="Cantidad Vendida" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Pagos Pendientes */}
            <div className="pending-payments-section">
                <h3>Pagos Pendientes de Revisión ({pendingPayments.length})</h3>
                {pendingPayments.length > 0 ? (
                    <div className="payments-table">
                        <table>
                            <thead>
                            <tr>
                                <th>Cliente</th>
                                <th>Correo</th>
                                <th>Monto</th>
                                <th>Método</th>
                                <th>Comprobante</th>
                                <th>Fecha</th>
                                <th>Acciones</th>
                            </tr>
                            </thead>
                            <tbody>
                            {pendingPayments.map(payment => (
                                <tr key={payment.id}>
                                    <td>{payment.customerName}</td>
                                    <td>{payment.customerEmail}</td>
                                    <td>${payment.amount.toFixed(2)}</td>
                                    <td><span className="badge">{payment.method}</span></td>
                                    <td>
                                        {payment.receiptUrl ? (
                                            <a href={payment.receiptUrl} target="_blank" rel="noopener noreferrer">
                                                Ver →
                                            </a>
                                        ) : (
                                            <span className="text-muted">-</span>
                                        )}
                                    </td>
                                    <td>{new Date(payment.createdAt).toLocaleDateString()}</td>
                                    <td>
                                        <button
                                            className="btn-small btn-approve"
                                            onClick={() => handleApprovePayment(payment.id)}
                                        >
                                            Aprobar
                                        </button>
                                        <button
                                            className="btn-small btn-reject"
                                            onClick={() => handleRejectPayment(payment.id)}
                                        >
                                            Rechazar
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-center text-muted">No hay pagos pendientes</p>
                )}
            </div>

            {/* Botón de Actualizar */}
            <div className="action-footer">
                <button onClick={loadDashboardData} className="btn-refresh">
                    Actualizar Datos
                </button>
            </div>
        </div>
    );
}