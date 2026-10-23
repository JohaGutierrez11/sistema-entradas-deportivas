// Dónde vive la sesión: sessionStorage (sobrevive al recargar y se borra al cerrar la pestaña).
// Decisión documentada: sin cookies, así la protección CSRF desactivada en el backend sigue siendo válida.
const KEY = 'entradas.session';

/** La sesión guardada, o null si no hay o si ya venció. */
export function loadSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(KEY));
    if (!session || Date.now() >= session.expiresAt) {
      clearSession();
      return null;
    }
    return session;
  } catch {
    return null;   // dato corrupto o navegador sin acceso a sessionStorage
  }
}

export function saveSession(session) {
  sessionStorage.setItem(KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(KEY);
}