import Toast from '../components/Toast';

export default function CartPage() {
    // ... tu código actual ...

    // 1️⃣ Estado para el toast
    const [toast, setToast] = useState(null);

    // 2️⃣ Función helper para mostrar toasts
    const showToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast(null), 3000); // Se auto-oculta en 3 segundos
    };

    // 3️⃣ Usar en tus funciones
    const updateQuantityHandler = (itemId, newQuantity) => {
        if (newQuantity <= 0) {
            removeFromCart(itemId);
            showToast('Producto eliminado del carrito', 'info');
        } else {
            updateQuantity(itemId, newQuantity);
            showToast('Cantidad actualizada', 'success');
        }
    };

    const handleClearCart = () => {
        clearCart();
        setConfirmClear(false);
        showToast('Carrito vaciado', 'success');
    };

    // 4️⃣ Renderizar el toast al final del componente
    return (
        <div className="cart-page">
            {/* Todo tu código actual del carrito */}

            {/* ✨ AGREGAR ESTO AL FINAL, antes del </div> final */}
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