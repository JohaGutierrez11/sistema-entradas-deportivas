import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import Placeholder from './components/Placeholder';
import ChangePasswordPage from './pages/client/ChangePasswordPage';
import MyReservationsPage from './pages/client/MyReservationsPage';
import ReservationDetailPage from './pages/client/ReservationDetailPage';
import ForbiddenPage from './pages/ForbiddenPage';
import LoginPage from './pages/public/LoginPage';
import MatchDetailPage from './pages/public/MatchDetailPage';
import MatchesPage from './pages/public/MatchesPage';
import RegisterPage from './pages/public/RegisterPage';

export default function App() {
  return (
    <Routes>
      {/* Pantallas completas, sin menú */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />

      {/* Con menú */}
      <Route element={<Layout />}>
        {/* Públicas */}
        <Route path="/" element={<MatchesPage />} />
        <Route path="/partidos/:matchId" element={<MatchDetailPage />} />
        <Route path="/acceso-denegado" element={<ForbiddenPage />} />

        {/* Cualquier usuario con sesión */}
        <Route element={<ProtectedRoute />}>
          <Route path="/cambiar-contrasena" element={<ChangePasswordPage />} />
        </Route>

        {/* Solo CLIENTE */}
        <Route element={<ProtectedRoute roles={['CLIENTE']} />}>
          <Route path="/reservas" element={<MyReservationsPage />} />
          <Route path="/reservas/:publicId" element={<ReservationDetailPage />} />
        </Route>

        {/* Solo ADMIN */}
        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="/admin" element={<Placeholder title="Panel del administrador" />} />
          <Route path="/admin/estadios" element={<Placeholder title="Estadios" />} />
          <Route path="/admin/partidos" element={<Placeholder title="Partidos" />} />
          <Route path="/admin/usuarios" element={<Placeholder title="Usuarios y roles" />} />
          <Route path="/admin/reportes" element={<Placeholder title="Reportes" />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}