import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/api';
import './LoginPage.css';

export default function LoginPage({ setUser }) {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        email: '',
        password: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
        setError(''); // Limpiar error al escribir
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.email || !formData.password) {
            setError('Por favor completa todos los campos');
            return;
        }

        try {
            setLoading(true);
            setError('');

            // Login
            const loginResponse = await authService.login(formData);
            const token = loginResponse.data.token;

            // Guardar token
            localStorage.setItem('token', token);

            // Obtener datos del usuario
            const userResponse = await authService.getCurrentUser();
            setUser(userResponse.data);

            // Redirigir según rol
            if (userResponse.data.role === 'ADMIN' || userResponse.data.role === 'VENDEDOR') {
                navigate('/admin');
            } else {
                navigate('/');
            }
        } catch (err) {
            console.error('Login error:', err);
            setError(err.response?.data?.error || 'Credenciales inválidas');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-container">
                <div className="login-card">
                    <h1>Iniciar Sesión</h1>
                    <p className="login-subtitle">Accede a tu cuenta</p>

                    {error && (
                        <div className="error-message">
                            ⚠️ {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="login-form">
                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="tu@email.com"
                                disabled={loading}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="password">Contraseña</label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="••••••••"
                                disabled={loading}
                            />
                        </div>

                        <button
                            type="submit"
                            className="btn-login"
                            disabled={loading}
                        >
                            {loading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
                        </button>
                    </form>

                    <div className="login-footer">
                        <p>
                            ¿No tienes cuenta?{' '}
                            <Link to="/register" className="link">
                                Regístrate aquí
                            </Link>
                        </p>
                    </div>

                    {/* Demo credentials */}
                    <div className="demo-credentials">
                        <p className="demo-title">🔐 Cuenta de prueba:</p>
                        <p className="demo-text">Email: admin@admin.com</p>
                        <p className="demo-text">Contraseña: admin123</p>
                    </div>
                </div>
            </div>
        </div>
    );
}