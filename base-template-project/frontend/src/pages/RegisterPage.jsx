import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/api';
import './RegisterPage.css';

export default function RegisterPage({ setUser }) {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validaciones
        if (!formData.name || !formData.email || !formData.password) {
            setError('Por favor completa los campos obligatorios');
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setError('Las contraseñas no coinciden');
            return;
        }

        if (formData.password.length < 6) {
            setError('La contraseña debe tener al menos 6 caracteres');
            return;
        }

        try {
            setLoading(true);
            setError('');

            // Registro
            const registerData = {
                name: formData.name,
                email: formData.email,
                phone: formData.phone,
                password: formData.password
            };

            await authService.register(registerData);

            // Auto-login después del registro
            const loginResponse = await authService.login({
                email: formData.email,
                password: formData.password
            });

            const token = loginResponse.data.token;
            localStorage.setItem('token', token);

            // Obtener datos del usuario
            const userResponse = await authService.getCurrentUser();
            setUser(userResponse.data);

            // Redirigir al home
            navigate('/');
        } catch (err) {
            console.error('Register error:', err);
            setError(
                err.response?.data?.error ||
                'Error al registrarse. El email puede estar en uso.'
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="register-page">
            <div className="register-container">
                <div className="register-card">
                    <h1>Crear Cuenta</h1>
                    <p className="register-subtitle">Regístrate para comenzar</p>

                    {error && (
                        <div className="error-message">
                            ⚠️ {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="register-form">
                        <div className="form-group">
                            <label htmlFor="name">Nombre completo *</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                placeholder="Juan Pérez"
                                disabled={loading}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Email *</label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="tu@email.com"
                                disabled={loading}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="phone">Teléfono (opcional)</label>
                            <input
                                type="tel"
                                id="phone"
                                name="phone"
                                value={formData.phone}
                                onChange={handleChange}
                                placeholder="+54 9 11 1234-5678"
                                disabled={loading}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Contraseña *</label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Mínimo 6 caracteres"
                                disabled={loading}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="confirmPassword">Confirmar contraseña *</label>
                            <input
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Repite tu contraseña"
                                disabled={loading}
                                required
                            />
                        </div>

                        <button
                            type="submit"
                            className="btn-register"
                            disabled={loading}
                        >
                            {loading ? 'Creando cuenta...' : 'Crear Cuenta'}
                        </button>
                    </form>

                    <div className="register-footer">
                        <p>
                            ¿Ya tienes cuenta?{' '}
                            <Link to="/login" className="link">
                                Inicia sesión aquí
                            </Link>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}