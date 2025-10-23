import React from 'react';
import { useCart } from '../store/CartContext';
import './ServiceCard.css';

export default function ServiceCard({ service }) {
    const { addToCart } = useCart();

    const handleAddToCart = () => {
        addToCart(service, 1);
        alert(`${service.name} agregado al carrito`);
    };

    return (
        <div className="service-card">
            {/* Imagen */}
            <div className="service-image">
                {service.imageUrl ? (
                    <img src={service.imageUrl} alt={service.name} />
                ) : (
                    <div className="service-no-image">⚙️</div>
                )}

                {/* Badge de duración */}
                {service.durationMinutes && (
                    <span className="service-badge">
                        ⏱️ {service.durationMinutes} min
                    </span>
                )}
            </div>

            {/* Info */}
            <div className="service-info">
                <h3 className="service-name">{service.name}</h3>

                {service.category && (
                    <span className="service-category">{service.category}</span>
                )}

                {service.description && (
                    <p className="service-description">{service.description}</p>
                )}

                {/* Features */}
                <div className="service-features">
                    {service.scheduleType && (
                        <span className="service-feature">
                            📅 {formatScheduleType(service.scheduleType)}
                        </span>
                    )}
                    {service.maxCapacity && (
                        <span className="service-feature">
                            👥 Máx. {service.maxCapacity} personas
                        </span>
                    )}
                    {service.requiresBooking && (
                        <span className="service-feature">
                            🎫 Requiere reserva
                        </span>
                    )}
                </div>

                {/* Precio */}
                <div className="service-footer">
                    <span className="service-price">
                        ${parseFloat(service.price).toFixed(2)}
                    </span>

                    <button className="btn-add-cart" onClick={handleAddToCart}>
                        🛒 Agregar
                    </button>
                </div>
            </div>
        </div>
    );
}

// Helper para formatear tipo de schedule
function formatScheduleType(type) {
    const types = {
        'ON_DEMAND': 'Bajo demanda',
        'SCHEDULED': 'Con turno',
        'RECURRING': 'Recurrente'
    };
    return types[type] || type;
}