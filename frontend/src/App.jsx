import { Navigate, Route, Routes } from 'react-router-dom';
import HomePage from './pages/HomePage';
import LoginPage from './pages/public/LoginPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      {/* Por ahora, admin y cambio de contraseña llevan al inicio; se construyen en los pasos siguientes */}
      <Route path="/admin" element={<HomePage />} />
      <Route path="/cambiar-contrasena" element={<HomePage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}