import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Toast from '../components/Toast';
import './SuperAdminServiceCreationPage.css';

export default function SuperAdminServiceCreationPage() {
    const navigate = useNavigate();
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingTenants, setLoadingTenants] = useState(true);
    const [toast, setToast] = useState(null);

    const [formData, setFormData] = useState({
        tenantId: '',
        name: '',
        description: '',
        price: '',
        category: '',
        imageUrl: '',
        durationMinutes: '',
        scheduleType: 'ON_DEMAND',
        maxCapacity: '',
        requiresBooking: false,
        availableDays: [],
        workStartTime: '',
        workEndTime: '',
        slotIntervalMinutes: ''
    });

    useEffect(() => {
        loadTenants();
    }, []);

    const loadTenants = async () => {
        try {
            setLoadingTenants(true);
            const response = await api.get('/api/super-admin/tenants');
            // Filtrar solo tenants activos que tengan servicios habilitados
            const tenantsWithServices = response.data.filter(t =>
                t.active &&
                t.config?.features?.services === true
            );
            setTenants(tenantsWithServices);
        } catch (err) {
            console.error('Error loading tenants:', err);
            showToast('Error cargando tenants', 'error');
        } finally {
            setLoadingTenants(false);
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleDayToggle = (day) => {
        const newDays = formData.availableDays.includes(day)
            ? formData.availableDays.filter(d => d !== day)
            : [...formData.availableDays, day];

        setFormData(prev => ({ ...prev, availableDays: newDays }));
    };

    const showToast = (message, type) => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validaciones
        if (!formData.tenantId) {
            showToast('Debes seleccionar un tenant', 'error');
            return;
        }

        if (!formData.name || !formData.price || !formData.durationMinutes) {
            showToast('Completa los campos obligatorios', 'error');
            return;
        }

        if (formData.requiresBooking) {
            if (formData.availableDays.length === 0) {
                showToast('Selecciona al menos un día disponible', 'error');
                return;
            }

            if (!formData.workStartTime || !formData.workEndTime) {
                showToast('Define el horario de atención', 'error');
                return;
            }

            if (!formData.slotIntervalMinutes) {
                showToast('Define el intervalo entre turnos', 'error');
                return;
            }
        }

        try {
            setLoading(true);

            const payload = {
                tenantId: formData.tenantId,
                name: formData.name,
                description: formData.description,
                price: parseFloat(formData.price),
                category: formData.category,
                imageUrl: formData.imageUrl,
                durationMinutes: parseInt(formData.durationMinutes),
                scheduleType: formData.scheduleType,
                maxCapacity: formData.maxCapacity ? parseInt(formData.maxCapacity) : null,
                requiresBooking: formData.requiresBooking
            };

            if (formData.requiresBooking) {
                payload.availableDays = formData.availableDays;
                payload.workStartTime = formData.workStartTime;
                payload.workEndTime = formData.workEndTime;
                payload.slotIntervalMinutes = parseInt(formData.slotIntervalMinutes);
            }

            await api.post('/api/super-admin/services', payload);

            showToast('Servicio creado exitosamente', 'success');

            // Limpiar formulario
            setFormData({
                tenantId: '',
                name: '',
                description: '',
                price: '',
                category: '',
                imageUrl: '',
                durationMinutes: '',
                scheduleType: 'ON_DEMAND',
                maxCapacity: '',
                requiresBooking: false,
                availableDays: [],
                workStartTime: '',
                workEndTime: '',
                slotIntervalMinutes: ''
            });

        } catch (err) {
            console.error('Error creating service:', err);
            const errorMsg = err.response?.data?.message ||
                err.response?.data?.error ||
                'Error al crear el servicio';
            showToast(errorMsg, 'error');
        } finally {
            setLoading(false);
        }
    };

    const dayLabels = {
        'MONDAY': 'Lun',
        'TUESDAY': 'Mar',
        'WEDNESDAY': 'Mié',
        'THURSDAY': 'Jue',
        'FRIDAY': 'Vie',
        'SATURDAY': 'Sáb',
        'SUNDAY': 'Dom'
    };

    if (loadingTenants) {
        return (
            <div className="sa-service-page">
                <div className="sa-loading">Cargando tenants...</div>
            </div>
        );
    }

    return (
        <div className="sa-service-page">
            <div className="sa-service-container">
                <div className="sa-service-header">
                    <h1>Crear Servicio</h1>
                    <button
                        onClick={() => navigate('/super-admin')}
                        className="btn-back"
                    >
                        ← Volver
                    </button>
                </div>

                {tenants.length === 0 && (
                    <div className="no-tenants-message">
                        <p>⚠️ No hay tenants con servicios habilitados</p>
                        <p className="hint">Los tenants deben tener la feature "services" activada</p>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="sa-service-form">
                    {/* Selector de Tenant */}
                    <div className="form-section tenant-section">
                        <label className="form-label required">
                            Tenant
                        </label>
                        <select
                            name="tenantId"
                            value={formData.tenantId}
                            onChange={handleChange}
                            required
                            className="form-select"
                        >
                            <option value="">-- Selecciona un tenant --</option>
                            {tenants.map(t => (
                                <option key={t.id} value={t.id}>
                                    {t.businessName} ({t.subdomain})
                                </option>
                            ))}
                        </select>
                        <small className="form-hint">
                            ⚠️ Como Super Admin, debes especificar a qué tenant pertenece el servicio
                        </small>
                    </div>

                    {/* Información Básica */}
                    <div className="form-row">
                        <div className="form-group">
                            <label className="form-label required">Nombre del Servicio</label>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                placeholder="Ej: Corte de Cabello"
                                required
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Categoría</label>
                            <input
                                type="text"
                                name="category"
                                value={formData.category}
                                onChange={handleChange}
                                placeholder="Ej: Peluquería"
                                className="form-input"
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Descripción</label>
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Describe el servicio..."
                            rows="3"
                            className="form-textarea"
                        />
                    </div>

                    {/* Precios y Duración */}
                    <div className="form-row">
                        <div className="form-group">
                            <label className="form-label required">Precio</label>
                            <input
                                type="number"
                                name="price"
                                value={formData.price}
                                onChange={handleChange}
                                placeholder="0.00"
                                step="0.01"
                                min="0"
                                required
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label required">Duración (minutos)</label>
                            <input
                                type="number"
                                name="durationMinutes"
                                value={formData.durationMinutes}
                                onChange={handleChange}
                                placeholder="60"
                                min="1"
                                required
                                className="form-input"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Capacidad Máxima</label>
                            <input
                                type="number"
                                name="maxCapacity"
                                value={formData.maxCapacity}
                                onChange={handleChange}
                                placeholder="10"
                                min="1"
                                className="form-input"
                            />
                        </div>
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label className="form-label">Tipo de Agenda</label>
                            <select
                                name="scheduleType"
                                value={formData.scheduleType}
                                onChange={handleChange}
                                className="form-select"
                            >
                                <option value="ON_DEMAND">Bajo Demanda</option>
                                <option value="SCHEDULED">Con Turno</option>
                                <option value="RECURRING">Recurrente</option>
                            </select>
                        </div>

                        <div className="form-group">
                            <label className="form-label">URL de Imagen</label>
                            <input
                                type="url"
                                name="imageUrl"
                                value={formData.imageUrl}
                                onChange={handleChange}
                                placeholder="https://ejemplo.com/imagen.jpg"
                                className="form-input"
                            />
                        </div>
                    </div>

                    {/* Checkbox Requiere Booking */}
                    <div className="form-group checkbox-group">
                        <label className="checkbox-label">
                            <input
                                type="checkbox"
                                name="requiresBooking"
                                checked={formData.requiresBooking}
                                onChange={handleChange}
                                className="checkbox-input"
                            />
                            Requiere Reserva Previa
                        </label>
                    </div>

                    {/* Configuración de Booking */}
                    {formData.requiresBooking && (
                        <div className="booking-config">
                            <h3 className="booking-title">📅 Configuración de Turnos</h3>

                            <div className="form-group">
                                <label className="form-label required">Días Disponibles</label>
                                <div className="days-selector">
                                    {['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].map((day) => (
                                        <button
                                            key={day}
                                            type="button"
                                            onClick={() => handleDayToggle(day)}
                                            className={`day-btn ${formData.availableDays.includes(day) ? 'active' : ''}`}
                                        >
                                            {dayLabels[day]}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label className="form-label required">Hora Inicio</label>
                                    <input
                                        type="time"
                                        name="workStartTime"
                                        value={formData.workStartTime}
                                        onChange={handleChange}
                                        required={formData.requiresBooking}
                                        className="form-input"
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label required">Hora Fin</label>
                                    <input
                                        type="time"
                                        name="workEndTime"
                                        value={formData.workEndTime}
                                        onChange={handleChange}
                                        required={formData.requiresBooking}
                                        className="form-input"
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label required">Intervalo (min)</label>
                                    <input
                                        type="number"
                                        name="slotIntervalMinutes"
                                        value={formData.slotIntervalMinutes}
                                        onChange={handleChange}
                                        placeholder="30"
                                        min="5"
                                        required={formData.requiresBooking}
                                        className="form-input"
                                    />
                                </div>
                            </div>

                            <small className="booking-hint">
                                ℹ️ Ejemplo: Si tu horario es 9:00 a 18:00 con turnos de 30 minutos, se crearán slots cada media hora.
                            </small>
                        </div>
                    )}

                    {/* Botón Submit */}
                    <button
                        type="submit"
                        className="btn-submit"
                        disabled={loading}
                    >
                        {loading ? 'Creando...' : 'Crear Servicio'}
                    </button>
                </form>
            </div>

            {/* Toast */}
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