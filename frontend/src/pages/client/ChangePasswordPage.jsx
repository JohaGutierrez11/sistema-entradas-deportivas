import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { fieldErrorMap } from '../../api/http';
import { useAuth } from '../../auth/AuthContext';
import FormField from '../../components/FormField';

const RULES = 'Mínimo 8 caracteres, con minúscula, mayúscula, número y símbolo.';

export default function ChangePasswordPage() {
  const { session, isAuthenticated, markPasswordChanged } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});

    // Esta regla solo existe en el front: el backend nunca recibe la confirmación
    if (newPassword !== confirmPassword) {
      setFieldErrors({ confirmPassword: 'Las contraseñas no coinciden' });
      return;
    }

    setLoading(true);
    try {
      await authApi.changePassword(currentPassword, newPassword);
      markPasswordChanged();
      navigate(session.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    } catch (err) {
      if (err.status === 400) setFieldErrors(fieldErrorMap(err));
      else setError(err.message);   // 422: la actual no es correcta, o la nueva es igual a la actual
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Cambiar contraseña</h1>
        {session.mustChangePassword && (
          <p className="alert alert-info">Es tu primer acceso: elige una contraseña nueva para continuar.</p>
        )}
        {error && <p className="alert alert-error" role="alert">{error}</p>}

        <FormField id="currentPassword" label="Contraseña actual" type="password" required
                   value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)}
                   error={fieldErrors.currentPassword} autoComplete="current-password" />
        <FormField id="newPassword" label="Contraseña nueva" type="password" required hint={RULES}
                   value={newPassword} onChange={(e) => setNewPassword(e.target.value)}
                   error={fieldErrors.newPassword} autoComplete="new-password" />
        <FormField id="confirmPassword" label="Repite la contraseña nueva" type="password" required
                   value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)}
                   error={fieldErrors.confirmPassword} autoComplete="new-password" />

        <button type="submit" className="btn btn-primary"
                disabled={loading || !currentPassword || !newPassword || !confirmPassword}>
          {loading ? 'Guardando…' : 'Cambiar contraseña'}
        </button>
      </form>
    </main>
  );
}