import React, { useState } from 'react';
import './EditTenantModal.css';

export default function EditTenantModal({ tenant, onClose, onSave }) {
    const [formData, setFormData] = useState({
        id: tenant.id,
        businessName: tenant.businessName,
        subdomain: tenant.subdomain,
        type: tenant.type,
        config: tenant.config || {}
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        onSave(formData);
    };

    const businessTypes = [
        'GYM', 'RETAIL', 'RESTAURANT', 'BEAUTY_SALON',
        'COWORKING', 'HEALTH', 'EDUCATION', 'PROFESSIONAL', 'OTHER'
    ];

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content edit-tenant-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>Editar Tenant</h2>
                    <button className="btn-close" onClick={onClose}>✕</button>
                </div>

                <form onSubmit={handleSubmit} className="modal-body">
                    <div className="form-group">
                        <label>Nombre del Negocio *</label>
                        <input
                            type="text"
                            name="businessName"
                            value={formData.businessName}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label>Subdomain (no editable)</label>
                        <input
                            type="text"
                            value={formData.subdomain}
                            disabled
                            className="input-disabled"
                        />
                        <small className="help-text">
                            El subdomain no se puede modificar por seguridad
                        </small>
                    </div>

                    <div className="form-group">
                        <label>Tipo de Negocio *</label>
                        <select
                            name="type"
                            value={formData.type}
                            onChange={handleChange}
                            required
                        >
                            {businessTypes.map(type => (
                                <option key={type} value={type}>
                                    {type.replace('_', ' ')}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="modal-footer">
                        <button type="button" onClick={onClose} className="btn-cancel">
                            Cancelar
                        </button>
                        <button type="submit" className="btn-save">
                            Guardar Cambios
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}