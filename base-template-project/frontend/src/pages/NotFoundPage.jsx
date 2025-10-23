import React from 'react';
import { useNavigate } from 'react-router-dom';
import './NotFoundPage.css';

export default function NotFoundPage() {
    const navigate = useNavigate();

    return (
        <div className="notfound-page">
            <div className="notfound-content">
                <h1 className="notfound-title">404</h1>
                <h2>Página no encontrada</h2>
                <p>Lo sentimos, la página que buscas no existe.</p>
                <button onClick={() => navigate('/')} className="btn-home">
                    Volver al Inicio
                </button>
            </div>
        </div>
    );
}