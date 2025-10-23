import React, { createContext, useContext, useState, useEffect } from 'react';

const CartContext = createContext();

export const useCart = () => {
    const context = useContext(CartContext);
    if (!context) {
        throw new Error('useCart must be used within a CartProvider');
    }
    return context;
};

export const CartProvider = ({ children }) => {
    const [cart, setCart] = useState([]);

    // Cargar carrito desde localStorage al iniciar
    useEffect(() => {
        const savedCart = localStorage.getItem('cart');
        if (savedCart) {
            setCart(JSON.parse(savedCart));
        }
    }, []);

    // Guardar carrito en localStorage cada vez que cambia
    useEffect(() => {
        localStorage.setItem('cart', JSON.stringify(cart));
    }, [cart]);

    // Agregar item al carrito
    const addToCart = (item, quantity = 1) => {
        setCart(prevCart => {
            const existingItem = prevCart.find(i => i.id === item.id);

            if (existingItem) {
                // Si ya existe, actualizar cantidad
                return prevCart.map(i =>
                    i.id === item.id
                        ? { ...i, quantity: i.quantity + quantity }
                        : i
                );
            } else {
                // Si no existe, agregarlo
                return [...prevCart, { ...item, quantity }];
            }
        });
    };

    // Remover item del carrito
    const removeFromCart = (itemId) => {
        setCart(prevCart => prevCart.filter(i => i.id !== itemId));
    };

    // Actualizar cantidad de un item
    const updateQuantity = (itemId, quantity) => {
        if (quantity <= 0) {
            removeFromCart(itemId);
        } else {
            setCart(prevCart =>
                prevCart.map(i =>
                    i.id === itemId ? { ...i, quantity } : i
                )
            );
        }
    };

    // Limpiar carrito
    const clearCart = () => {
        setCart([]);
        localStorage.removeItem('cart');
    };

    // Calcular total
    const getTotal = () => {
        return cart.reduce((total, item) => {
            return total + (item.price * item.quantity);
        }, 0);
    };

    // Obtener cantidad total de items
    const getTotalItems = () => {
        return cart.reduce((total, item) => total + item.quantity, 0);
    };

    const value = {
        cart,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        getTotal,
        getTotalItems
    };

    return (
        <CartContext.Provider value={value}>
            {children}
        </CartContext.Provider>
    );
};