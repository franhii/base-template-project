import './ConfirmModal.css';

export default function ConfirmModal({
                                         isOpen,
                                         title,
                                         message,
                                         onConfirm,
                                         onCancel,
                                         confirmText = 'Confirmar',
                                         cancelText = 'Cancelar',
                                         type = 'warning'
                                     }) {
    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onCancel}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2 className="modal-title">{title}</h2>
                </div>

                <div className="modal-body">
                    <p className="modal-message">{message}</p>
                </div>

                <div className="modal-footer">
                    <button
                        onClick={onCancel}
                        className="btn-modal btn-cancel"
                    >
                        {cancelText}
                    </button>
                    <button
                        onClick={onConfirm}
                        className={`btn-modal btn-confirm btn-${type}`}
                    >
                        {confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
}