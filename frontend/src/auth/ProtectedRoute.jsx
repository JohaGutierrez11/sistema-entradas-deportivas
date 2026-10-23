import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/**
 * Guardia de un grupo de rutas. NO es seguridad (eso lo hace el backend con 401/403):
 * evita mostrar pantallas que el usuario no puede usar.
 *   <Route element={<ProtectedRoute roles={['ADMIN']} />}> ...rutas protegidas... </Route>
 */
export default function ProtectedRoute({ roles }) {
  const { session } = useAuth();
  const location = useLocation();

  if (!session) {
    // Recuerda a dónde iba, para volver ahí después del login
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(session.role)) {
    return <Navigate to="/acceso-denegado" replace />;
  }
  return <Outlet />;
}