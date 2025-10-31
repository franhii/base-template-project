import React, { useState, useEffect } from 'react';
import api from '../services/api';
import './BookingModal.css';

export default function BookingModal({ service, onClose, onConfirm }) {
    const [selectedDate, setSelectedDate] = useState('');
    const [availableSlots, setAvailableSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Generar fechas disponibles (próximos 30 días)
    const getAvailableDates = () => {
        const dates = [];
        const today = new Date();

        for (let i = 0; i < 30; i++) {
            const date = new Date(today);
            date.setDate(today.getDate() + i);

            // Obtener el día de la semana (0 = domingo, 1 = lunes, etc.)
            const dayOfWeek = date.getDay();
            const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
            const dayName = dayNames[dayOfWeek];

            // Solo agregar si el servicio está disponible ese día
            if (service.availableDays && service.availableDays.includes(dayName)) {
                dates.push(date.toISOString().split('T')[0]);
            }
        }

        return dates;
    };

    const availableDates = getAvailableDates();

    // Cargar slots disponibles cuando se selecciona una fecha
    useEffect(() => {
        if (selectedDate) {
            loadAvailableSlots(selectedDate);
        }
    }, [selectedDate]);

    const loadAvailableSlots = async (date) => {
        setLoading(true);
        setError('');
        setSelectedSlot(null);

        try {
            const response = await api.get(`/api/bookings/available`, {
                params: {
                    serviceId: service.id,
                    date: date
                }
            });

            setAvailableSlots(response.data);

            if (response.data.length === 0) {
                setError('No hay turnos disponibles para esta fecha');
            }
        } catch (err) {
            console.error('Error loading slots:', err);
            setError('Error al cargar los turnos disponibles');
            setAvailableSlots([]);
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = () => {
        if (!selectedDate || !selectedSlot) {
            setError('Selecciona una fecha y horario');
            return;
        }

        onConfirm({
            date: selectedDate,
            time: selectedSlot.startTime
        });
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Reservar Turno</h2>
                    <button className="btn-close" onClick={onClose}></button>
                </div>

                <div className="modal-body">
                    {/* Información del servicio */}
                    <div className="service-info-modal">
                        <h3>{service.name}</h3>
                        <p>Duración: {service.durationMinutes} minutos</p>
                        <p>Precio: ${parseFloat(service.price).toFixed(2)}</p>
                    </div>

                    {/* Selector de fecha */}
                    <div className="form-group">
                        <label>Selecciona una fecha:</label>
                        <select
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            className="date-select"
                        >
                            <option value="">-- Selecciona una fecha --</option>
                            {availableDates.map((date) => {
                                const dateObj = new Date(date + 'T12:00:00'); // Evitar problemas de zona horaria
                                const dayNames = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
                                const dayName = dayNames[dateObj.getDay()];

                                return (
                                    <option key={date} value={date}>
                                        {dayName}, {dateObj.toLocaleDateString('es-ES', {
                                            day: 'numeric',
                                            month: 'long',
                                            year: 'numeric'
                                        })}
                                    </option>
                                );
                            })}
                        </select>
                    </div>

                    {/* Slots disponibles */}
                    {selectedDate && (
                        <div className="slots-container">
                            <label>Horarios disponibles:</label>

                            {loading && <p className="loading-message">Cargando horarios...</p>}

                            {error && <p className="error-message">{error}</p>}

                            {!loading && availableSlots.length > 0 && (
                                <div className="slots-grid">
                                    {availableSlots.map((slot, index) => (
                                        <button
                                            key={index}
                                            className={`slot-btn ${selectedSlot === slot ? 'selected' : ''}`}
                                            onClick={() => setSelectedSlot(slot)}
                                        >
                                            <div className="slot-time">{slot.startTime}</div>
                                            <div className="slot-capacity">
                                                {slot.availableSpots}/{slot.totalCapacity} disponibles
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </div>

                <div className="modal-footer">
                    <button className="btn-cancel" onClick={onClose}>
                        Cancelar
                    </button>
                    <button
                        className="btn-confirm"
                        onClick={handleConfirm}
                        disabled={!selectedDate || !selectedSlot}
                    >
                        Confirmar Reserva
                    </button>
                </div>
            </div>
        </div>
    );
}
