import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../store/CartContext';
import ConfirmModal from '../components/ConfirmModal';
import './CartPage.css';

export default function CartPage() {
    const navigate = useNavigate();
    const { cart, removeFromCart, updateQuantity, getTotal, clearCart } = useCart();
    const [confirmClear, setConfirmClear] = useState(false);

    const updateQuantityHandler = (itemId, newQuantity) => {
        if (newQuantity <= 0) {
            removeFromCart(itemId);
        } else {
            updateQuantity(itemId, newQuantity);
        }
    };

    const handleClearCart = () => {
        clearCart();
        setConfirmClear(false);
    };

    const getTotals = () => {
        const subtotal = getTotal();
        const tax = subtotal * 0.21; // IVA 21%
        const total = subtotal + tax;
        return { subtotal, tax, total };
    };

    const { subtotal, tax, total } = getTotals();

    if (cart.length === 0) {
        return (
            <div className="cart-page">
                <div className="cart-empty">
                    <div className="empty-icon">🛒</div>
                    <h2>Tu carrito está vacío</h2>
                    <p>Agrega productos para comenzar tu compra</p>
                    <button onClick={() => navigate('/')} className="btn-continue">
                        Ver Productos
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="cart-page">
            <ConfirmModal
                isOpen={confirmClear}
                title="Vaciar Carrito"
                message="¿Estás seguro de que deseas eliminar todos los items del carrito?"
                onConfirm={handleClearCart}
                onCancel={() => setConfirmClear(false)}
                confirmText="Sí, vaciar"
                type="danger"
            />

            <div className="cart-container">
                <div className="cart-header">
                    <h1>Mi Carrito</h1>
                    <button onClick={() => setConfirmClear(true)} className="btn-clear">
                        Vaciar Carrito
                    </button>
                </div>

                <div className="cart-content">
                    {/* Items List */}
                    <div className="cart-items">
                        {cart.map(item => (
                            <div key={item.id} className="cart-item">
                                <div className="item-image">
                                    {item.imageUrl ? (
                                        <img src={item.imageUrl} alt={item.name} />
                                    ) : (
                                        <div className="no-image">📦</div>
                                    )}
                                </div>

                                <div className="item-info">
                                    <h3>{item.name}</h3>
                                    {item.category && (
                                        <span className="item-category">{item.category}</span>
                                    )}
                                    <p className="item-price">
                                        ${parseFloat(item.price).toFixed(2)} c/u
                                    </p>
                                </div>

                                <div className="item-quantity">
                                    <button
                                        onClick={() => updateQuantityHandler(item.id, item.quantity - 1)}
                                        className="btn-qty"
                                    >
                                        -
                                    </button>
                                    <span className="qty-value">{item.quantity}</span>
                                    <button
                                        onClick={() => updateQuantityHandler(item.id, item.quantity + 1)}
                                        className="btn-qty"
                                    >
                                        +
                                    </button>
                                </div>

                                <div className="item-subtotal">
                                    <p className="subtotal-label">Subtotal</p>
                                    <p className="subtotal-value">
                                        ${(item.price * item.quantity).toFixed(2)}
                                    </p>
                                </div>

                                <button
                                    onClick={() => removeFromCart(item.id)}
                                    className="btn-remove"
                                    title="Eliminar"
                                >
                                    🗑️
                                </button>
                            </div>
                        ))}
                    </div>

                    {/* Summary */}
                    <div className="cart-summary">
                        <h2>Resumen</h2>

                        <div className="summary-row">
                            <span>Subtotal</span>
                            <span>${subtotal.toFixed(2)}</span>
                        </div>

                        <div className="summary-row">
                            <span>IVA (21%)</span>
                            <span>${tax.toFixed(2)}</span>
                        </div>

                        <div className="summary-divider"></div>

                        <div className="summary-row summary-total">
                            <span>Total</span>
                            <span>${total.toFixed(2)}</span>
                        </div>

                        <button
                            onClick={() => navigate('/checkout')}
                            className="btn-checkout"
                        >
                            Proceder al Pago
                        </button>

                        <button
                            onClick={() => navigate('/')}
                            className="btn-continue-shopping"
                        >
                            Continuar Comprando
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}