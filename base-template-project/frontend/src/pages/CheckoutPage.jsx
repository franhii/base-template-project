// ========== CheckoutPage.jsx ==========
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../store/CartContext';
import api from '../services/api';
import './CheckoutPage.css';

export default function CheckoutPage() {
    const navigate = useNavigate();
    const { cart, getTotal, clearCart } = useCart();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [paymentMethod, setPaymentMethod] = useState('MERCADO_PAGO');
    const [notes, setNotes] = useState('');

    const handleCheckout = async () => {
        if (cart.length === 0) {
            setError('El carrito está vacío');
            return;
        }

        try {
            setLoading(true);
            setError('');

            // 1. Crear orden
            const orderData = {
                items: cart.map(item => ({
                    itemId: item.id,
                    quantity: item.quantity
                })),
                paymentMethod,
                notes
            };

            const orderResponse = await api.post('/api/orders', orderData);
            const order = orderResponse.data;

            // 2. Crear pago
            const paymentData = {
                orderId: order.id,
                method: paymentMethod
            };

            const paymentResponse = await api.post('/api/payments', paymentData);
            const payment = paymentResponse.data;

            // 3. Redirigir según método
            if (paymentMethod === 'MERCADO_PAGO' && payment.paymentLink) {
                // Redirigir a MercadoPago
                window.location.href = payment.paymentLink;
            } else {
                // Limpar carrito y mostrar éxito
                clearCart();
                alert('¡Orden creada exitosamente!');
                navigate('/');
            }
        } catch (err) {
            console.error('Checkout error:', err);
            setError(err.response?.data?.message || 'Error procesando el pago');
        } finally {
            setLoading(false);
        }
    };

    if (cart.length === 0) {
        return (
            <div className="checkout-page">
                <div className="checkout-empty">
                    <h2>No hay items en el carrito</h2>
                    <button onClick={() => navigate('/')} className="btn">
                        Volver a la tienda
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="checkout-page">
            <div className="checkout-container">
                <h1>Finalizar Compra</h1>

                {error && <div className="error-message">{error}</div>}

                {/* Resumen */}
                <div className="checkout-summary">
                    <h2>Resumen de Compra</h2>
                    {cart.map(item => (
                        <div key={item.id} className="summary-item">
                            <span>{item.name} x{item.quantity}</span>
                            <span>${(item.price * item.quantity).toFixed(2)}</span>
                        </div>
                    ))}
                    <div className="summary-total">
                        <strong>Total:</strong>
                        <strong>${getTotal().toFixed(2)}</strong>
                    </div>
                </div>

                {/* Método de pago */}
                <div className="payment-method">
                    <h2>Método de Pago</h2>
                    <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        disabled={loading}
                    >
                        <option value="MERCADO_PAGO">MercadoPago</option>
                        <option value="BANK_TRANSFER">Transferencia Bancaria</option>
                        <option value="CASH">Efectivo</option>
                    </select>
                </div>

                {/* Notas */}
                <div className="checkout-notes">
                    <label>Notas (opcional)</label>
                    <textarea
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        placeholder="Instrucciones especiales..."
                        disabled={loading}
                    />
                </div>

                {/* Botones */}
                <div className="checkout-actions">
                    <button
                        onClick={() => navigate('/cart')}
                        className="btn-back"
                        disabled={loading}
                    >
                        Volver al Carrito
                    </button>
                    <button
                        onClick={handleCheckout}
                        className="btn-pay"
                        disabled={loading}
                    >
                        {loading ? 'Procesando...' : 'Confirmar Pago'}
                    </button>
                </div>
            </div>
        </div>
    );
}