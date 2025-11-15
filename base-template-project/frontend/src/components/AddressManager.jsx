import React, { useState, useEffect } from 'react';
import './AddressManager.css';

const AddressManager = () => {
  const [addresses, setAddresses] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchAddresses();
  }, []);

  const fetchAddresses = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const response = await fetch('/api/addresses', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        setAddresses(data);
      } else {
        throw new Error('Error al cargar direcciones');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (addressId) => {
    if (!window.confirm('¿Estás seguro de que quieres eliminar esta dirección?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/addresses/${addressId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      });

      if (response.ok) {
        setAddresses(addresses.filter(addr => addr.id !== addressId));
      } else {
        throw new Error('Error al eliminar dirección');
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSetDefault = async (addressId) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/addresses/${addressId}/set-default`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
        }
      });

      if (response.ok) {
        fetchAddresses(); // Refresh to update default status
      } else {
        throw new Error('Error al establecer dirección por defecto');
      }
    } catch (err) {
      setError(err.message);
    }
  };

  const handleEdit = (address) => {
    setEditingAddress(address);
    setShowForm(true);
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditingAddress(null);
    fetchAddresses(); // Refresh addresses after form closes
  };

  if (loading) {
    return (
      <div className="address-manager">
        <div className="loading">Cargando direcciones...</div>
      </div>
    );
  }

  return (
    <div className="address-manager">
      <div className="address-manager-header">
        <h3>Mis Direcciones</h3>
        <button 
          className="btn btn-primary"
          onClick={() => setShowForm(true)}
        >
          <i className="fas fa-plus"></i> Agregar Dirección
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {addresses.length === 0 ? (
        <div className="empty-addresses">
          <i className="fas fa-map-marker-alt"></i>
          <p>No tienes direcciones guardadas</p>
          <button 
            className="btn btn-primary"
            onClick={() => setShowForm(true)}
          >
            Agregar tu primera dirección
          </button>
        </div>
      ) : (
        <div className="addresses-grid">
          {addresses.map(address => (
            <div key={address.id} className="address-card">
              {address.isDefault && (
                <div className="default-badge">
                  <i className="fas fa-star"></i> Por defecto
                </div>
              )}
              
              <div className="address-info">
                <div className="address-title">
                  <strong>{address.street} {address.streetNumber}</strong>
                  {address.apartment && <span>, {address.apartment}</span>}
                </div>
                
                <div className="address-details">
                  {address.localityName && <span>{address.localityName}, </span>}
                  <span>{address.municipalityName}</span>
                </div>
                
                <div className="address-details">
                  <span>{address.provinceName}</span>
                  <span className="postal-code">CP: {address.postalCode}</span>
                </div>

                {address.reference && (
                  <div className="address-reference">
                    <i className="fas fa-info-circle"></i>
                    {address.reference}
                  </div>
                )}
              </div>

              <div className="address-actions">
                {!address.isDefault && (
                  <button
                    className="btn btn-outline"
                    onClick={() => handleSetDefault(address.id)}
                    title="Establecer como dirección por defecto"
                  >
                    <i className="fas fa-star"></i>
                  </button>
                )}
                
                <button
                  className="btn btn-outline"
                  onClick={() => handleEdit(address)}
                  title="Editar dirección"
                >
                  <i className="fas fa-edit"></i>
                </button>
                
                <button
                  className="btn btn-danger-outline"
                  onClick={() => handleDelete(address.id)}
                  title="Eliminar dirección"
                >
                  <i className="fas fa-trash"></i>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <AddressFormModal 
          address={editingAddress}
          onClose={handleFormClose}
        />
      )}
    </div>
  );
};

// Modal Form Component
const AddressFormModal = ({ address, onClose }) => {
  const [formData, setFormData] = useState({
    street: address?.street || '',
    streetNumber: address?.streetNumber || '',
    provinceId: address?.provinceId || '',
    provinceName: address?.provinceName || '',
    municipalityId: address?.municipalityId || '',
    municipalityName: address?.municipalityName || '',
    localityId: address?.localityId || '',
    localityName: address?.localityName || '',
    postalCode: address?.postalCode || '',
    apartment: address?.apartment || '',
    reference: address?.reference || '',
    isDefault: address?.isDefault || false
  });

  const [provinces, setProvinces] = useState([]);
  const [municipalities, setMunicipalities] = useState([]);
  const [localities, setLocalities] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    fetchProvinces();
  }, []);

  useEffect(() => {
    if (formData.provinceId) {
      fetchMunicipalities(formData.provinceId);
    }
  }, [formData.provinceId]);

  useEffect(() => {
    if (formData.municipalityId) {
      fetchLocalities(formData.municipalityId);
    }
  }, [formData.municipalityId]);

  const fetchProvinces = async () => {
    try {
      const response = await fetch('/api/georef/provinces');
      if (response.ok) {
        const data = await response.json();
        console.log('🔍 PROVINCIAS RECIBIDAS:', data);
        console.log('🔍 PRIMERA PROVINCIA:', data[0]);
        setProvinces(data);
      }
    } catch (err) {
      console.error('Error fetching provinces:', err);
    }
  };

  const fetchMunicipalities = async (provinceId) => {
    try {
      const response = await fetch(`/api/georef/municipalities?provinceId=${provinceId}`);
      if (response.ok) {
        const data = await response.json();
        console.log('🔍 MUNICIPIOS RECIBIDOS:', data);
        console.log('🔍 PRIMER MUNICIPIO:', data[0]);
        setMunicipalities(data);
      }
    } catch (err) {
      console.error('Error fetching municipalities:', err);
    }
  };

  const fetchLocalities = async (municipalityId) => {
    try {
      const response = await fetch(`/api/georef/localities?municipalityId=${municipalityId}`);
      if (response.ok) {
        const data = await response.json();
        setLocalities(data);
      }
    } catch (err) {
      console.error('Error fetching localities:', err);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    
    console.log('🔍 CAMPO CAMBIADO:', name, 'VALOR:', value);
    
    if (name === 'provinceId') {
      const province = provinces.find(p => p.id === value);
      console.log('🔍 PROVINCIA ENCONTRADA:', province);
      console.log('🔍 TODAS LAS PROVINCIAS:', provinces);
      setFormData({
        ...formData,
        provinceId: value,
        provinceName: province?.id === '02' ? 'Buenos Aires' : (province?.name || ''),
        municipalityId: '',
        municipalityName: '',
        localityId: '',
        localityName: ''
      });
    } else if (name === 'municipalityId') {
      const municipality = municipalities.find(m => m.id === value);
      setFormData({
        ...formData,
        municipalityId: value,
        municipalityName: municipality?.name || '',
        localityId: '',
        localityName: ''
      });
    } else if (name === 'localityId') {
      const locality = localities.find(l => l.id === value);
      setFormData({
        ...formData,
        localityId: value,
        localityName: locality?.name || ''
      });
    } else {
      setFormData({
        ...formData,
        [name]: type === 'checkbox' ? checked : value
      });
    }
    
    // Clear error for this field
    if (errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.street.trim()) newErrors.street = 'La calle es obligatoria';
    if (!formData.streetNumber.trim()) newErrors.streetNumber = 'El número es obligatorio';
    if (!formData.provinceId) newErrors.provinceId = 'La provincia es obligatoria';
    if (!formData.municipalityId) newErrors.municipalityId = 'El municipio es obligatorio';
    if (!formData.postalCode.trim()) newErrors.postalCode = 'El código postal es obligatorio';
    
    const postalCodePattern = /^\d{4}$/;
    if (formData.postalCode && !postalCodePattern.test(formData.postalCode)) {
      newErrors.postalCode = 'El código postal debe tener 4 dígitos';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const url = address ? `/api/addresses/${address.id}` : '/api/addresses';
      const method = address ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        onClose();
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al guardar la dirección');
      }
    } catch (err) {
      setErrors({ submit: err.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content address-form" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{address ? 'Editar Dirección' : 'Nueva Dirección'}</h3>
          <button className="close-btn" onClick={onClose}>
            <i className="fas fa-times"></i>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="address-form-content">
          {errors.submit && (
            <div className="error-message">{errors.submit}</div>
          )}

          <div className="form-row">
            <div className="form-group">
              <label>Calle *</label>
              <input
                type="text"
                name="street"
                value={formData.street}
                onChange={handleChange}
                className={errors.street ? 'error' : ''}
                placeholder="Ej: Av. Corrientes"
              />
              {errors.street && <span className="error-text">{errors.street}</span>}
            </div>

            <div className="form-group">
              <label>Número *</label>
              <input
                type="text"
                name="streetNumber"
                value={formData.streetNumber}
                onChange={handleChange}
                className={errors.streetNumber ? 'error' : ''}
                placeholder="Ej: 1234"
              />
              {errors.streetNumber && <span className="error-text">{errors.streetNumber}</span>}
            </div>
          </div>

          <div className="form-group">
            <label>Apartamento/Piso</label>
            <input
              type="text"
              name="apartment"
              value={formData.apartment}
              onChange={handleChange}
              placeholder="Ej: Piso 2, Depto B"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Provincia *</label>
              <select
                name="provinceId"
                value={formData.provinceId}
                onChange={handleChange}
                className={errors.provinceId ? 'error' : ''}
              >
                <option value="">Seleccionar provincia</option>
                {provinces.map(province => (
                  <option key={province.id} value={province.id}>
                    {province.id === '02' ? 'Buenos Aires' : province.name}
                  </option>
                ))}
              </select>
              {errors.provinceId && <span className="error-text">{errors.provinceId}</span>}
            </div>

            <div className="form-group">
              <label>Municipio *</label>
              <select
                name="municipalityId"
                value={formData.municipalityId}
                onChange={handleChange}
                className={errors.municipalityId ? 'error' : ''}
                disabled={!formData.provinceId}
              >
                <option value="">Seleccionar municipio</option>
                {municipalities.map(municipality => (
                  <option key={municipality.id} value={municipality.id}>
                    {municipality.name}
                  </option>
                ))}
              </select>
              {errors.municipalityId && <span className="error-text">{errors.municipalityId}</span>}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Localidad</label>
              <select
                name="localityId"
                value={formData.localityId}
                onChange={handleChange}
                disabled={!formData.municipalityId}
              >
                <option value="">Seleccionar localidad</option>
                {localities.map(locality => (
                  <option key={locality.id} value={locality.id}>
                    {locality.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Código Postal *</label>
              <input
                type="text"
                name="postalCode"
                value={formData.postalCode}
                onChange={handleChange}
                className={errors.postalCode ? 'error' : ''}
                placeholder="Ej: 1001"
                maxLength={4}
              />
              {errors.postalCode && <span className="error-text">{errors.postalCode}</span>}
            </div>
          </div>

          <div className="form-group">
            <label>Referencias</label>
            <textarea
              name="reference"
              value={formData.reference}
              onChange={handleChange}
              placeholder="Información adicional para encontrar la dirección"
              rows={3}
            />
          </div>

          <div className="form-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="isDefault"
                checked={formData.isDefault}
                onChange={handleChange}
              />
              <span className="checkmark"></span>
              Establecer como dirección por defecto
            </label>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Guardando...' : (address ? 'Actualizar' : 'Guardar')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddressManager;