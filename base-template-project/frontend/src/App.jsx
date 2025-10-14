// frontend/src/App.jsx
import { useEffect, useState } from 'react';
import api from './services/api';

export default function App() {
    const [config, setConfig] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Cargar config del tenant al iniciar
        api.get('/api/config/current')
            .then(response => {
                const tenantConfig = response.data;
                setConfig(tenantConfig);

                // Aplicar branding dinámicamente
                document.documentElement.style.setProperty('--primary', tenantConfig.primaryColor);
                document.documentElement.style.setProperty('--secondary', tenantConfig.secondaryColor);
                document.title = tenantConfig.businessName;

                setLoading(false);
            })
            .catch(err => {
                console.error('Error loading tenant config:', err);
                setLoading(false);
            });
    }, []);

    if (loading) return <div>Loading...</div>;

    return (
        <div>
            <h1>{config.businessName}</h1>

            {/* Mostrar features según configuración */}
            // Después (más limpio y seguro)
            {config.features.delivery && (
                <DeliverySection
                    cost={config.features.deliveryConfig?.deliveryCost}
                    freeFrom={config.features.deliveryConfig?.freeDeliveryThreshold}
                />
            )}
            {config.features.booking && <BookingSection />}

            {/* Resto de la app */}
        </div>
    );
}