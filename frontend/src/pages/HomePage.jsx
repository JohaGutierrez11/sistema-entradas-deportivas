import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/** Temporal: para probar el login. En el paso 3 será el catálogo de partidos. */
export default function HomePage() {
  const { session, isAuthenticated, logout } = useAuth();

  return (
    <main className="container">
      <h1>Entradas deportivas</h1>
      {isAuthenticated ? (
        <div className="card">
          <p>Hola, <strong>{session.username}</strong> · rol <strong>{session.role}</strong></p>
          <button className="btn" onClick={logout}>Cerrar sesión</button>
        </div>
      ) : (
        <p><Link to="/login">Iniciar sesión</Link></p>
      )}
    </main>
  );
}