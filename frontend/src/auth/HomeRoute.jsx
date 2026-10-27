import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import MatchesPage from '../pages/public/MatchesPage';

/** "/" depende de quién entra: el ADMIN va a su panel; el público y el cliente, al catálogo. */
export default function HomeRoute() {
  const { isAdmin } = useAuth();
  return isAdmin ? <Navigate to="/admin" replace /> : <MatchesPage />;
}
