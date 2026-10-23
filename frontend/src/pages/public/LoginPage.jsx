import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const session = await login(username.trim(), password);
      // Primer acceso → cambiar la contraseña; si no, a donde iba o según el rol
      if (session.mustChangePassword) {
        navigate('/cambiar-contrasena', { replace: true });
      } else {
        const fallback = session.role === 'ADMIN' ? '/admin' : '/';
        navigate(location.state?.from ?? fallback, { replace: true });
      }
    } catch (err) {
      setError(err.message);   // el mensaje del backend: 401 genérico, 423 bloqueada, 403 inactiva
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Iniciar sesión</h1>

        {error && <p className="alert alert-error" role="alert">{error}</p>}

        <label htmlFor="username">Usuario</label>
        <input id="username" value={username} onChange={(e) => setUsername(e.target.value)}
               autoComplete="username" required autoFocus />

        <label htmlFor="password">Contraseña</label>
        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
               autoComplete="current-password" required />

        <button type="submit" className="btn btn-primary" disabled={loading || !username || !password}>
          {loading ? 'Ingresando…' : 'Ingresar'}
        </button>

        <p className="muted">¿No tienes cuenta? <Link to="/registro">Regístrate</Link></p>
      </form>
    </main>
  );
}