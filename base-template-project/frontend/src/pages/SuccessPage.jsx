import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './SuccessPage.css';

export default function SuccessPage() {
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

        // Limpiar carrito
        localStorage.removeItem('cart');
    }, []);

    return (
        <div className="success-page">
            <div className="success-container">
                <div className="success-card">
                    <div className="success-header">
                        <div className="success-icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h1>¡Pago Exitoso!</h1>
                        <p>Tu compra ha sido procesada correctamente</p>
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
                                        <span className="badge badge-success">{orderDetails.status}</span>
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
                        <div className="info-icon">ℹ️</div>
                        <div className="info-content">
                            <h3>Información importante</h3>
                            <p>
                                Recibirás un correo de confirmación con todos los detalles de tu compra.
                                Guarda tu número de orden para futuras consultas.
                            </p>
                        </div>
                    </div>

                    <div className="action-buttons">
                        <button onClick={() => navigate('/')} className="btn-primary">
                            🛒 Seguir Comprando
                        </button>
                        <button onClick={() => navigate('/')} className="btn-secondary">
                            📦 Ver Productos
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
    );
}