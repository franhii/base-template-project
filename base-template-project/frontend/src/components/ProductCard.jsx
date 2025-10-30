import React from 'react';
import { useCart } from '../store/CartContext';
import './ProductCard.css';

export default function ProductCard({ product }) {
    const { addToCart } = useCart();
    const [toast, setToast] = useState(null);

    const handleAddToCart = () => {
        addToCart(product, 1);
        setToast({ message: `${product.name} agregado al carrito`, type: 'success' });
        setTimeout(() => setToast(null), 3000);
    };

    return (
        <div className="product-card">
            {/* Imagen */}
            <div className="product-image">
                {product.imageUrl ? (
                    <img src={product.imageUrl} alt={product.name} />
                ) : (
                    <div className="product-no-image">📦</div>
                )}

                {/* Badge de stock */}
                {product.stock <= 5 && product.stock > 0 && (
                    <span className="product-badge badge-warning">
                        ¡Últimas {product.stock} unidades!
                    </span>
                )}
                {product.stock === 0 && (
                    <span className="product-badge badge-danger">
                        Sin stock
                    </span>
                )}
            </div>

            {/* Info */}
            <div className="product-info">
                <h3 className="product-name">{product.name}</h3>

                {product.category && (
                    <span className="product-category">{product.category}</span>
                )}

                {product.description && (
                    <p className="product-description">{product.description}</p>
                )}

                {/* Precio */}
                <div className="product-footer">
                    <span className="product-price">
                        ${parseFloat(product.price).toFixed(2)}
                    </span>

                    <button
                        className="btn-add-cart"
                        onClick={handleAddToCart}
                        disabled={product.stock === 0}
                    >
                        {product.stock === 0 ? '❌ Sin stock' : '🛒 Agregar'}
                    </button>
                </div>

                {/* SKU (opcional) */}
                {product.sku && (
                    <small className="product-sku">SKU: {product.sku}</small>
                )}
            </div>
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
        </div>
    );
}