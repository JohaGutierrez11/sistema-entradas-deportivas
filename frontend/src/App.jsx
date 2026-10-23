import { Navigate, Route, Routes } from 'react-router-dom';
import ChangePasswordPage from './pages/client/ChangePasswordPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/public/LoginPage';
import RegisterPage from './pages/public/RegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegisterPage />} />
      <Route path="/cambiar-contrasena" element={<ChangePasswordPage />} />
      <Route path="/admin" element={<HomePage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}