import React, { useEffect, useState } from 'react';
import { itemService } from '../services/api';
import ProductCard from '../components/ProductCard';
import ServiceCard from '../components/ServiceCard';
import './HomePage.css';

export default function HomePage({ config }) {
    const [products, setProducts] = useState([]);
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('all'); // 'all', 'products', 'services'

    useEffect(() => {
        loadItems();
    }, []);

    const loadItems = async () => {
        try {
            setLoading(true);

            // Cargar productos y servicios si están habilitados en el config
            const promises = [];

            if (config?.config?.features?.products !== false) {
                promises.push(itemService.getProducts());
            }

            if (config?.config?.features?.services !== false) {
                promises.push(itemService.getServices());
            }

            const results = await Promise.all(promises);

            if (config?.config?.features?.products !== false) {
                setProducts(results[0]?.data || []);
            }

            if (config?.config?.features?.services !== false) {
                const servicesIndex = config?.config?.features?.products !== false ? 1 : 0;
                setServices(results[servicesIndex]?.data || []);
            }

            setError(null);
        } catch (err) {
            console.error('Error loading items:', err);
            setError('Error cargando productos y servicios');
        } finally {
            setLoading(false);
        }
    };

    const filteredProducts = activeTab === 'all' || activeTab === 'products' ? products : [];
    const filteredServices = activeTab === 'all' || activeTab === 'services' ? services : [];

    if (loading) {
        return (
            <div className="home-loading">
                <div className="spinner"></div>
                <p>Cargando productos...</p>
            </div>
        );
    }

    if (error) {
        return <div className="home-error">{error}</div>;
    }

    return (
        <div className="home-page">
            {/* Hero Section */}
            <section className="hero">
                <div className="hero-content">
                    <h1>Bienvenido a {config?.businessName || 'Nuestra Tienda'}</h1>
                    <p>Encuentra los mejores productos y servicios</p>
                </div>
            </section>

            {/* Tabs */}
            <div className="tabs-container">
                <button
                    className={`tab ${activeTab === 'all' ? 'active' : ''}`}
                    onClick={() => setActiveTab('all')}
                >
                    Todo ({products.length + services.length})
                </button>

                {products.length > 0 && (
                    <button
                        className={`tab ${activeTab === 'products' ? 'active' : ''}`}
                        onClick={() => setActiveTab('products')}
                    >
                        Productos ({products.length})
                    </button>
                )}

                {services.length > 0 && (
                    <button
                        className={`tab ${activeTab === 'services' ? 'active' : ''}`}
                        onClick={() => setActiveTab('services')}
                    >
                        Servicios ({services.length})
                    </button>
                )}
            </div>

            {/* Products Grid */}
            {filteredProducts.length > 0 && (
                <section className="items-section">
                    <h2>Productos</h2>
                    <div className="items-grid">
                        {filteredProducts.map(product => (
                            <ProductCard key={product.id} product={product} />
                        ))}
                    </div>
                </section>
            )}

            {/* Services Grid */}
            {filteredServices.length > 0 && (
                <section className="items-section">
                    <h2>Servicios</h2>
                    <div className="items-grid">
                        {filteredServices.map(service => (
                            <ServiceCard key={service.id} service={service} />
                        ))}
                    </div>
                </section>
            )}

            {/* Empty State */}
            {products.length === 0 && services.length === 0 && (
                <div className="empty-state">
                    <div className="empty-icon">📦</div>
                    <h3>No hay productos disponibles</h3>
                    <p>Vuelve pronto para ver nuestras ofertas</p>
                </div>
            )}
        </div>
    );
}