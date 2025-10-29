import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './FailurePage.css';

export default function FailurePage() {
    const navigate = useNavigate();
    const [errorDetails, setErrorDetails] = useState(null);

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const paymentId = urlParams.get('payment_id');
        const status = urlParams.get('status');
        const statusDetail = urlParams.get('status_detail');

        setErrorDetails({
            paymentId,
            status,
            statusDetail,
            date: new Date().toLocaleString('es-AR')
        });
    }, []);

    const getErrorMessage = () => {
        if (!errorDetails?.statusDetail) return 'No se pudo procesar tu pago.';

        const messages = {
            'cc_rejected_insufficient_amount': 'Fondos insuficientes',
            'cc_rejected_bad_filled_security_code': 'Código de seguridad incorrecto',
            'cc_rejected_bad_filled_date': 'Fecha de vencimiento incorrecta',
            'cc_rejected_bad_filled_other': 'Revisa los datos de tu tarjeta',
            'cc_rejected_call_for_authorize': 'Debes autorizar el pago con tu banco',
            'cc_rejected_card_disabled': 'Tarjeta deshabilitada',
            'cc_rejected_duplicated_payment': 'Ya existe un pago con esos datos',
            'cc_rejected_high_risk': 'Pago rechazado por seguridad',
            'cc_rejected_max_attempts': 'Superaste el límite de intentos'
        };

        return messages[errorDetails.statusDetail] || 'No se pudo procesar tu pago.';
    };

    return (
        <div className="failure-page">
            <div className="failure-container">
                <div className="failure-card">
                    <div className="failure-header">
                        <div className="failure-icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h1>Pago Rechazado</h1>
                        <p className="error-message">{getErrorMessage()}</p>
                    </div>

                    {errorDetails && (
                        <div className="error-details">
                            <h2>Detalles del Error</h2>
                            <div className="details-grid">
                                {errorDetails.paymentId && (
                                    <div className="detail-row">
                                        <span className="detail-label">ID de Intento:</span>
                                        <span className="detail-value">{errorDetails.paymentId}</span>
                                    </div>
                                )}
                                {errorDetails.status && (
                                    <div className="detail-row">
                                        <span className="detail-label">Estado:</span>
                                        <span className="badge badge-error">{errorDetails.status}</span>
                                    </div>
                                )}
                                {errorDetails.statusDetail && (
                                    <div className="detail-row">
                                        <span className="detail-label">Detalle:</span>
                                        <span className="detail-value-small">{errorDetails.statusDetail}</span>
                                    </div>
                                )}
                                <div className="detail-row">
                                    <span className="detail-label">Fecha:</span>
                                    <span className="detail-value">{errorDetails.date}</span>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="help-box">
                        <div className="help-icon">💡</div>
                        <div className="help-content">
                            <h3>¿Qué puedes hacer?</h3>
                            <ul>
                                <li>Verifica que los datos de tu tarjeta sean correctos</li>
                                <li>Asegúrate de tener fondos suficientes</li>
                                <li>Intenta con otro método de pago</li>
                                <li>Contacta a tu banco si el problema persiste</li>
                            </ul>
                        </div>
                    </div>

                    <div className="action-buttons">
                        <button onClick={() => navigate('/checkout')} className="btn-primary">
                            🔄 Intentar Nuevamente
                        </button>
                        <button onClick={() => navigate('/')} className="btn-secondary">
                            🛒 Volver al Catálogo
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