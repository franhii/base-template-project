import { useState } from 'react';
import { AlertCircle, Truck, Clock, CheckCircle } from 'lucide-react';
import './ShippingPreview.css';

const ShippingPreview = ({ cartTotal, onShippingSelect }) => {
    const [postalCode, setPostalCode] = useState('');
    const [shippingOptions, setShippingOptions] = useState([]);
    const [selectedOption, setSelectedOption] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleCalculate = async () => {
        // Validar CP
        if (!postalCode || postalCode.length !== 4 || !/^\d{4}$/.test(postalCode)) {
            setError('Ingresa un código postal válido (4 dígitos)');
            return;
        }

        setLoading(true);
        setError('');
        setShippingOptions([]);
        setSelectedOption(null);

        try {
            const response = await fetch('/api/shipping/quote-by-postalcode', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Tenant-Subdomain': window.location.hostname.split('.')[0] || 'default'
                },
                body: JSON.stringify({
                    postalCode: postalCode,
                    orderTotal: cartTotal
                })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Error al calcular envío');
            }

            const options = await response.json();

            if (options.length === 0) {
                setError('No hay opciones de envío disponibles para este código postal');
            } else {
                setShippingOptions(options);
                // Auto-seleccionar la opción más barata
                const cheapest = options.reduce((min, opt) =>
                    opt.cost < min.cost ? opt : min
                );
                setSelectedOption(cheapest);
                onShippingSelect?.(cheapest);
            }

        } catch (err) {
            console.error('Error al calcular envío:', err);
            setError(err.message || 'No se pudo calcular el envío. Intenta nuevamente.');
        } finally {
            setLoading(false);
        }
    };

    const handleSelectOption = (option) => {
        setSelectedOption(option);
        onShippingSelect?.(option);
    };

    const handlePostalCodeChange = (e) => {
        const value = e.target.value.replace(/\D/g, '').slice(0, 4);
        setPostalCode(value);
        // Limpiar resultados si cambia el CP
        if (value !== postalCode) {
            setShippingOptions([]);
            setSelectedOption(null);
            setError('');
        }
    };

    return (
        <div className="shipping-preview">
            <div className="shipping-preview-header">
                <Truck className="icon" />
                <h3>Calcular Envío</h3>
            </div>

            <div className="shipping-input-group">
                <input
                    type="text"
                    placeholder="Código Postal (ej: 1234)"
                    value={postalCode}
                    onChange={handlePostalCodeChange}
                    maxLength={4}
                    className="postal-code-input"
                />
                <button
                    onClick={handleCalculate}
                    disabled={loading || !postalCode}
                    className="calculate-btn"
                >
                    {loading ? 'Calculando...' : 'Calcular'}
                </button>
            </div>

            {error && (
                <div className="shipping-error">
                    <AlertCircle size={16} />
                    <span>{error}</span>
                </div>
            )}

            {shippingOptions.length > 0 && (
                <div className="shipping-options">
                    <div className="options-header">
                        <span>Opciones de envío</span>
                        <span className="estimated-badge">Estimado</span>
                    </div>

                    {shippingOptions.map((option) => (
                        <div
                            key={option.shippingMethodId}
                            className={`shipping-option ${selectedOption?.shippingMethodId === option.shippingMethodId ? 'selected' : ''}`}
                            onClick={() => handleSelectOption(option)}
                        >
                            <div className="option-radio">
                                {selectedOption?.shippingMethodId === option.shippingMethodId && (
                                    <CheckCircle size={20} className="check-icon" />
                                )}
                            </div>

                            <div className="option-details">
                                <div className="option-name">
                                    {option.name}
                                    {option.isFree && <span className="free-badge">GRATIS</span>}
                                </div>
                                <div className="option-delivery">
                                    <Clock size={14} />
                                    <span>{option.estimatedDelivery}</span>
                                </div>
                                {option.carrier && (
                                    <div className="option-carrier">{option.carrier}</div>
                                )}
                            </div>

                            <div className="option-price">
                                {option.isFree ? (
                                    <span className="free-text">GRATIS</span>
                                ) : (
                                    <span className="price-text">${option.cost.toFixed(2)}</span>
                                )}
                            </div>
                        </div>
                    ))}

                    <div className="shipping-note">
                        <AlertCircle size={14} />
                        <span>El costo final se confirmará en el checkout con tu dirección completa</span>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ShippingPreview;