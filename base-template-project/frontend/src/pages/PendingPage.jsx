import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './PendingPage.css';

export default function PendingPage() {
    const navigate = useNavigate();
    const [orderDetails, setOrderDetails] = useState(null);

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const paymentId = urlParams.get('payment_id');
        const status = urlParams.get('status');
        const merchantOrderId = urlParams.get('merchant_order_id');

        setOrderDetails({
            paymentId,
            status,
            merchantOrderId,
            date: new Date().toLocaleString('es-AR')
        });
    }, []);

    return (
        <div className="pending-page">
            <div className="pending-container">
                <div className="pending-card">
                    <div className="pending-header">
                        <div className="pending-icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h1>Pago Pendiente</h1>
                        <p>Tu pago está siendo procesado</p>
                    </div>

                    {orderDetails && (
                        <div className="order-details">
                            <h2>Detalles de la Orden</h2>
                            <div className="details-grid">
                                {orderDetails.paymentId && (
                                    <div className="detail-row">
                                        <span className="detail-label">ID de Pago:</span>
                                        <span className="detail-value">{orderDetails.paymentId}</span>
                                    </div>
                                )}
                                {orderDetails.merchantOrderId && (
                                    <div className="detail-row">
                                        <span className="detail-label">Orden:</span>
                                        <span className="detail-value">{orderDetails.merchantOrderId}</span>
                                    </div>
                                )}
                                {orderDetails.status && (
                                    <div className="detail-row">
                                        <span className="detail-label">Estado:</span>
                                        <span className="badge badge-pending">{orderDetails.status}</span>
                                    </div>
                                )}
                                <div className="detail-row">
                                    <span className="detail-label">Fecha:</span>
                                    <span className="detail-value">{orderDetails.date}</span>
                                </div>
                            </div>
                        </div>
                    )}
                    <div className="info-box">
                        <div className="info-icon">⏳</div>
                        <div className="info-content">
                            <h3>¿Qué significa esto?</h3>
                            <ul>
                                <li>Tu pago está en proceso de verificación</li>
                                <li>Recibirás una confirmación por correo cuando se complete</li>
                                <li>Este proceso puede tomar algunos minutos</li>
                                <li>No es necesario realizar otro pago</li>
                            </ul>
                        </div>
                    </div>
                    <div className="action-buttons">
                        <button onClick={() => navigate('/')} className="btn-primary">
                            🛒 Volver al Catálogo
                        </button>
                        <button onClick={() => window.location.reload()} className="btn-secondary">
                            🔄 Actualizar Estado
                        </button>
                    </div>

                    <div className="support-link">
                        <p>
                            ¿Necesitas ayuda?
                            <a href="#" className="link">Contacta con soporte</a>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    )
}