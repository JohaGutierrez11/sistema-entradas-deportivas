import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Navbar from './Navbar';

/** Estructura común: menú arriba y la página en el hueco (<Outlet />). */
export default function Layout() {
  const { session } = useAuth();
  const location = useLocation();

  // Primer acceso: no se usa la app hasta cambiar la contraseña temporal
  if (session?.mustChangePassword && location.pathname !== '/cambiar-contrasena') {
    return <Navigate to="/cambiar-contrasena" replace />;
  }

  return (
    <>
      <Navbar />
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}