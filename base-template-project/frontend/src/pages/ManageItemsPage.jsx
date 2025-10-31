import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Toast from '../components/Toast';
import './ManageItemsPage.css';

export function ManageItemsPage() {
    const navigate = useNavigate();
    const [itemType, setItemType] = useState('product'); // 'product' o 'service'
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [config, setConfig] = useState(null);
    const [toast, setToast] = useState(null);


    // Cargar config del tenant
    useEffect(() => {
        api.get('/api/config/current')
            .then(response => {
                setConfig(response.data);
                // Si solo tiene servicios habilitados, cambiar tab inicial
                if (response.data?.config?.features?.products === false &&
                    response.data?.config?.features?.services !== false) {
                    setItemType('service');
                }
            })
            .catch(err => console.error('Error loading config:', err));
    }, []);

    // Form data para producto
    const [productData, setProductData] = useState({
        name: '',
        description: '',
        price: '',
        category: '',
        imageUrl: '',
        stock: '',
        sku: '',
        type: 'PHYSICAL'
    });

    // Form data para servicio
    const [serviceData, setServiceData] = useState({
        name: '',
        description: '',
        price: '',
        category: '',
        imageUrl: '',
        durationMinutes: '',
        scheduleType: 'ON_DEMAND',
        maxCapacity: '',
        requiresBooking: false,
        // Campos de disponibilidad
        availableDays: [],
        workStartTime: '',
        workEndTime: '',
        slotIntervalMinutes: ''
    });

    const handleProductChange = (e) => {
        const {name, value} = e.target;
        setProductData({...productData, [name]: value});
        setError('');
        setSuccess('');
    };

    const handleServiceChange = (e) => {
        const {name, value, type, checked} = e.target;
        setServiceData({
            ...serviceData,
            [name]: type === 'checkbox' ? checked : value
        });
        setError('');
        setSuccess('');
    };

    const handleDayToggle = (day) => {
        const newDays = serviceData.availableDays.includes(day)
            ? serviceData.availableDays.filter(d => d !== day)
            : [...serviceData.availableDays, day];

        setServiceData({
            ...serviceData,
            availableDays: newDays
        });
    };

    const handleSubmitProduct = async (e) => {
        e.preventDefault();

        if (!productData.name || !productData.price || !productData.stock) {
            setToast({ message: 'Completa los campos obligatorios', type: 'error' });
            setTimeout(() => setToast(null), 3000);
            return;
        }

        // 🛡️ Validaciones adicionales
        const price = parseFloat(productData.price);
        const stock = parseInt(productData.stock);

        if (price < 0) {
            setToast({ message: 'El precio no puede ser negativo', type: 'error' });
            setTimeout(() => setToast(null), 3000);
            return;
        }

        if (stock < 0) {
            setToast({ message: 'El stock no puede ser negativo', type: 'error' });
            setTimeout(() => setToast(null), 3000);
            return;
        }

        if (productData.type === 'PHYSICAL' && stock === 0) {
            setToast({
                message: 'Advertencia: Producto físico sin stock. Los clientes no podrán comprarlo.',
                type: 'warning'
            });
            setTimeout(() => setToast(null), 5000);
        }

        try {
            setLoading(true);
            setError('');

            await api.post('/api/items/products', {
                ...productData,
                price,
                stock
            });

            setToast({message: 'Producto creado exitosamente', type: 'success'});
            setTimeout(() => setToast(null), 3000);

            // Limpiar formulario
            setProductData({
                name: '',
                description: '',
                price: '',
                category: '',
                imageUrl: '',
                stock: '',
                sku: '',
                type: 'PHYSICAL'
            });

            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setToast({ message: 'Error al crear', type: 'error' });
            setTimeout(() => setToast(null), 3000);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmitService = async (e) => {
        e.preventDefault();

        if (!serviceData.name || !serviceData.price || !serviceData.durationMinutes) {
            setToast({ message: 'Completa los campos obligatorios', type: 'error' });
            setTimeout(() => setToast(null), 3000);
            return;
        }

        // 🛡️ Validaciones para servicios con reserva
        if (serviceData.requiresBooking) {
            if (serviceData.availableDays.length === 0) {
                setToast({ message: 'Selecciona al menos un día disponible', type: 'error' });
                setTimeout(() => setToast(null), 3000);
                return;
            }

            if (!serviceData.workStartTime || !serviceData.workEndTime) {
                setToast({ message: 'Define el horario de atención', type: 'error' });
                setTimeout(() => setToast(null), 3000);
                return;
            }

            if (!serviceData.slotIntervalMinutes) {
                setToast({ message: 'Define el intervalo entre turnos', type: 'error' });
                setTimeout(() => setToast(null), 3000);
                return;
            }
        }

        try {
            setLoading(true);
            setError('');

            const payload = {
                name: serviceData.name,
                description: serviceData.description,
                price: parseFloat(serviceData.price),
                category: serviceData.category,
                imageUrl: serviceData.imageUrl,
                durationMinutes: parseInt(serviceData.durationMinutes),
                scheduleType: serviceData.scheduleType,
                maxCapacity: serviceData.maxCapacity ? parseInt(serviceData.maxCapacity) : null,
                requiresBooking: serviceData.requiresBooking
            };

            // Solo agregar campos de booking si requiresBooking es true
            if (serviceData.requiresBooking) {
                payload.availableDays = serviceData.availableDays;
                payload.workStartTime = serviceData.workStartTime;
                payload.workEndTime = serviceData.workEndTime;
                payload.slotIntervalMinutes = parseInt(serviceData.slotIntervalMinutes);
            }

            await api.post('/api/items/services', payload);

            setToast({ message: 'Servicio creado exitosamente', type: 'success' });
            setTimeout(() => setToast(null), 3000);

            // Limpiar formulario
            setServiceData({
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

            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            console.error('Error creating service:', err);
            setToast({ message: 'Error al crear: ' + (err.response?.data?.message || err.message), type: 'error' });
            setTimeout(() => setToast(null), 3000);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="manage-items-page">
            <div className="manage-container">
                <div className="manage-header">
                    <h1>Gestión de Inventario</h1>
                    <button onClick={() => navigate('/admin')} className="btn-back">
                        ← Volver al Dashboard
                    </button>
                </div>

                {/* Tabs */}
                <div className="tabs">
                    <button
                        className={`tab ${itemType === 'product' ? 'active' : ''}`}
                        onClick={() => setItemType('product')}
                    >
                        📦 Productos
                    </button>
                    <button
                        className={`tab ${itemType === 'service' ? 'active' : ''}`}
                        onClick={() => setItemType('service')}
                    >
                        ⚙️ Servicios
                    </button>
                </div>

                {/* Mensajes */}
                {error && <div className="message error-message">❌ {error}</div>}
                {success && <div className="message success-message">{success}</div>}

                {/* Formulario de Producto */}
                {itemType === 'product' && (
                    <form onSubmit={handleSubmitProduct} className="item-form">
                        <h2>Nuevo Producto</h2>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Nombre *</label>
                                <input
                                    type="text"
                                    name="name"
                                    value={productData.name}
                                    onChange={handleProductChange}
                                    placeholder="Ej: Remera Lisa"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Categoría</label>
                                <input
                                    type="text"
                                    name="category"
                                    value={productData.category}
                                    onChange={handleProductChange}
                                    placeholder="Ej: Ropa"
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Descripción</label>
                            <textarea
                                name="description"
                                value={productData.description}
                                onChange={handleProductChange}
                                placeholder="Describe el producto..."
                                rows="3"
                            />
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Precio *</label>
                                <input
                                    type="number"
                                    name="price"
                                    value={productData.price}
                                    onChange={handleProductChange}
                                    placeholder="0.00"
                                    step="0.01"
                                    min="0"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Stock *</label>
                                <input
                                    type="number"
                                    name="stock"
                                    value={productData.stock}
                                    onChange={handleProductChange}
                                    placeholder="0"
                                    min="0"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>SKU</label>
                                <input
                                    type="text"
                                    name="sku"
                                    value={productData.sku}
                                    onChange={handleProductChange}
                                    placeholder="Ej: REM-001"
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Tipo</label>
                                <select
                                    name="type"
                                    value={productData.type}
                                    onChange={handleProductChange}
                                >
                                    <option value="PHYSICAL">Físico</option>
                                    <option value="DIGITAL">Digital</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label>URL de Imagen</label>
                                <input
                                    type="url"
                                    name="imageUrl"
                                    value={productData.imageUrl}
                                    onChange={handleProductChange}
                                    placeholder="https://ejemplo.com/imagen.jpg"
                                />
                            </div>
                        </div>

                        <button type="submit" className="btn-submit" disabled={loading}>
                            {loading ? 'Creando...' : 'Crear Producto'}
                        </button>
                    </form>
                )}

                {/* Formulario de Servicio */}
                {itemType === 'service' && (
                    <form onSubmit={handleSubmitService} className="item-form">
                        <h2>Nuevo Servicio</h2>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Nombre *</label>
                                <input
                                    type="text"
                                    name="name"
                                    value={serviceData.name}
                                    onChange={handleServiceChange}
                                    placeholder="Ej: Corte de Cabello"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Categoría</label>
                                <input
                                    type="text"
                                    name="category"
                                    value={serviceData.category}
                                    onChange={handleServiceChange}
                                    placeholder="Ej: Peluquería"
                                />
                            </div>
                        </div>

                        <div className="form-group">
                            <label>Descripción</label>
                            <textarea
                                name="description"
                                value={serviceData.description}
                                onChange={handleServiceChange}
                                placeholder="Describe el servicio..."
                                rows="3"
                            />
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Precio *</label>
                                <input
                                    type="number"
                                    name="price"
                                    value={serviceData.price}
                                    onChange={handleServiceChange}
                                    placeholder="0.00"
                                    step="0.01"
                                    min="0"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Duración (minutos) *</label>
                                <input
                                    type="number"
                                    name="durationMinutes"
                                    value={serviceData.durationMinutes}
                                    onChange={handleServiceChange}
                                    placeholder="60"
                                    min="1"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label>Capacidad Máxima</label>
                                <input
                                    type="number"
                                    name="maxCapacity"
                                    value={serviceData.maxCapacity}
                                    onChange={handleServiceChange}
                                    placeholder="10"
                                    min="1"
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label>Tipo de Agenda</label>
                                <select
                                    name="scheduleType"
                                    value={serviceData.scheduleType}
                                    onChange={handleServiceChange}
                                >
                                    <option value="ON_DEMAND">Bajo Demanda</option>
                                    <option value="SCHEDULED">Con Turno</option>
                                    <option value="RECURRING">Recurrente</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label>URL de Imagen</label>
                                <input
                                    type="url"
                                    name="imageUrl"
                                    value={serviceData.imageUrl}
                                    onChange={handleServiceChange}
                                    placeholder="https://ejemplo.com/imagen.jpg"
                                />
                            </div>
                        </div>

                        <div className="form-group checkbox-group">
                            <label>
                                <input
                                    type="checkbox"
                                    name="requiresBooking"
                                    checked={serviceData.requiresBooking}
                                    onChange={handleServiceChange}
                                />
                                Requiere Reserva Previa
                            </label>
                        </div>

                        {/* 🗓️ CONFIGURACIÓN DE DISPONIBILIDAD (solo si requiresBooking) */}
                        {serviceData.requiresBooking && (
                            <div className="booking-config">
                                <h3>Configuración de Turnos</h3>

                                {/* Días disponibles */}
                                <div className="form-group">
                                    <label>Días Disponibles *</label>
                                    <div className="days-selector">
                                        {['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].map((day) => {
                                            const dayLabels = {
                                                'MONDAY': 'Lun',
                                                'TUESDAY': 'Mar',
                                                'WEDNESDAY': 'Mié',
                                                'THURSDAY': 'Jue',
                                                'FRIDAY': 'Vie',
                                                'SATURDAY': 'Sáb',
                                                'SUNDAY': 'Dom'
                                            };
                                            return (
                                                <button
                                                    key={day}
                                                    type="button"
                                                    className={`day-btn ${serviceData.availableDays.includes(day) ? 'active' : ''}`}
                                                    onClick={() => handleDayToggle(day)}
                                                >
                                                    {dayLabels[day]}
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                {/* Horarios */}
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Hora de Inicio *</label>
                                        <input
                                            type="time"
                                            name="workStartTime"
                                            value={serviceData.workStartTime}
                                            onChange={handleServiceChange}
                                            required={serviceData.requiresBooking}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label>Hora de Fin *</label>
                                        <input
                                            type="time"
                                            name="workEndTime"
                                            value={serviceData.workEndTime}
                                            onChange={handleServiceChange}
                                            required={serviceData.requiresBooking}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label>Intervalo entre Turnos (min) *</label>
                                        <input
                                            type="number"
                                            name="slotIntervalMinutes"
                                            value={serviceData.slotIntervalMinutes}
                                            onChange={handleServiceChange}
                                            placeholder="30"
                                            min="5"
                                            required={serviceData.requiresBooking}
                                        />
                                    </div>
                                </div>

                                <small className="hint">
                                    ℹ️ Ejemplo: Si tu horario es 9:00 a 18:00 con turnos de 30 minutos, se crearán slots cada media hora.
                                </small>
                            </div>
                        )}

                        <button type="submit" className="btn-submit" disabled={loading}>
                            {loading ? 'Creando...' : 'Crear Servicio'}
                        </button>
                    </form>
                )}
            </div>
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
        </div>
    );
}