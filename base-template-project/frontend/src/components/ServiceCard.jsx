import React, { useState } from 'react';
import { useCart } from '../store/CartContext';
import Toast from './Toast';
import BookingModal from './BookingModal';
import './ServiceCard.css';

export default function ServiceCard({ service }) {
    const { addToCart } = useCart();
    const [toast, setToast] = useState(null);
    const [showBookingModal, setShowBookingModal] = useState(false);

    const handleAddToCart = () => {
        // Servicios sin booking se agregan directamente
        if (!service.requiresBooking) {
            addToCart(service, 1);
            setToast({ message: `${service.name} agregado al carrito`, type: 'success' });
            setTimeout(() => setToast(null), 3000);
        } else {
            // Servicios con booking abren el modal
            setShowBookingModal(true);
        }
    };

    const handleBookingConfirm = (bookingData) => {
        // Agregar el servicio al carrito con la fecha y hora seleccionadas
        addToCart({
            ...service,
            bookingDate: bookingData.date,
            bookingTime: bookingData.time
        }, 1);

        setShowBookingModal(false);
        setToast({ message: `Turno reservado para ${bookingData.date} a las ${bookingData.time}`, type: 'success' });
        setTimeout(() => setToast(null), 3000);
    };

    return (
        <>
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
                            {service.requiresBooking ? '📅 Reservar' : '🛒 Agregar'}
                        </button>
                    </div>
                </div>
            </div>

            {/* Modal de reserva */}
            {showBookingModal && (
                <BookingModal
                    service={service}
                    onClose={() => setShowBookingModal(false)}
                    onConfirm={handleBookingConfirm}
                />
            )}

            {/* Toast notifications */}
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
        </>
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