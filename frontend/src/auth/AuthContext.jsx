import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { clearSession, loadSession, saveSession } from './session';

const AuthContext = createContext(null);

/** Comparte con toda la app quién está conectado. Un solo lugar para iniciar y cerrar sesión. */
export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());

  // Si http.js detecta un 401, la sesión se cierra en toda la app
  useEffect(() => {
    const onExpired = () => setSession(null);
    window.addEventListener('session-expired', onExpired);
    return () => window.removeEventListener('session-expired', onExpired);
  }, []);

  const value = useMemo(() => {
    async function login(username, password) {
      const response = await authApi.login(username, password);
      const newSession = {
        token: response.accessToken,
        username: response.username,
        role: response.role,
        mustChangePassword: response.mustChangePassword,
        expiresAt: Date.now() + response.expiresIn * 1000,
      };
      saveSession(newSession);
      setSession(newSession);
      return newSession;
    }

    function logout() {
      clearSession();
      setSession(null);
    }

    return {
      session,
      isAuthenticated: session !== null,
      isAdmin: session?.role === 'ADMIN',
      isClient: session?.role === 'CLIENTE',
      login,
      logout,
    };
  }, [session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** Hook para usar la sesión en cualquier pantalla: const { session, login } = useAuth(); */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return context;
}