import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function ForbiddenPage() {
  const { isAdmin } = useAuth();
  return (
    <section className="card">
      <h1>Acceso denegado</h1>
      <p>No tienes permiso para ver esta página.</p>
      <Link to={isAdmin ? '/admin' : '/'}>Volver al inicio</Link>
    </section>
  );
}