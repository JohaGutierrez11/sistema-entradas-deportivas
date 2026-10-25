import { Navigate, Route, Routes } from 'react-router-dom';
import HomeRoute from './auth/HomeRoute';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import ChangePasswordPage from './pages/client/ChangePasswordPage';
import MyReservationsPage from './pages/client/MyReservationsPage';
import ReservationDetailPage from './pages/client/ReservationDetailPage';
import AdminHomePage from './pages/admin/AdminHomePage';
import MatchesAdminPage from './pages/admin/MatchesAdminPage';
import ReportsAdminPage from './pages/admin/ReportsAdminPage';
import SettingsAdminPage from './pages/admin/SettingsAdminPage';
import UsersAdminPage from './pages/admin/UsersAdminPage';
import StadiumSectionsPage from './pages/admin/StadiumSectionsPage';
import StadiumsAdminPage from './pages/admin/StadiumsAdminPage';
import ForbiddenPage from './pages/ForbiddenPage';
import LoginPage from './pages/public/LoginPage';
import MatchDetailPage from './pages/public/MatchDetailPage';
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
        <Route path="/" element={<HomeRoute />} />
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
          <Route path="/admin" element={<AdminHomePage />} />
          <Route path="/admin/estadios" element={<StadiumsAdminPage />} />
          <Route path="/admin/estadios/:stadiumId" element={<StadiumSectionsPage />} />
          <Route path="/admin/partidos" element={<MatchesAdminPage />} />
          <Route path="/admin/usuarios" element={<UsersAdminPage />} />
          <Route path="/admin/reportes" element={<ReportsAdminPage />} />
          <Route path="/admin/configuracion" element={<SettingsAdminPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}