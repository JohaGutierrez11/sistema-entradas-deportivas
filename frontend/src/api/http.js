import { clearSession, loadSession } from '../auth/session';

/** Mensajes para respuestas que llegan sin body (401/403 de Spring Security) o sin conexión. */
const DEFAULT_MESSAGES = {
  0: 'No se pudo conectar con el servidor. Revisa que los servicios estén encendidos.',
  401: 'Tu sesión venció. Inicia sesión otra vez.',
  403: 'No tienes permiso para realizar esta acción.',
  500: 'Ocurrió un error inesperado. Intenta de nuevo.',
  503: 'El servicio no está disponible en este momento. Intenta en unos minutos.',
};

/** Error de la API con el mensaje del backend (Problem Details) y los errores por campo. */
export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail ?? DEFAULT_MESSAGES[status] ?? `Error ${status}`);
    this.status = status;
    this.fieldErrors = problem?.errors ?? [];   // [{ field, message }] en los 400
  }
}

/**
 * Única puerta hacia el backend. Agrega el token, traduce los errores y
 * detecta la sesión vencida. Ninguna pantalla llama a fetch directamente.
 */
export async function request(path, { method = 'GET', body, query, auth = true } = {}) {
  const session = auth ? loadSession() : null;
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (session) headers.Authorization = `Bearer ${session.token}`;

  let response;
  try {
    response = await fetch(path + toQueryString(query), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, null);   // servidor apagado o sin red
  }

  // Token vencido o inválido: se cierra la sesión y se avisa a toda la app
  if (response.status === 401 && session) {
    clearSession();
    window.dispatchEvent(new Event('session-expired'));
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response));
  }
  if (response.status === 204) return null;

  const type = response.headers.get('Content-Type') ?? '';
  return type.includes('json') ? response.json() : response.blob();   // blob: PDF o imagen QR
}

/** ?a=1&b=2 sin los filtros vacíos (lección del Sprint 1: no enviar parámetros vacíos). */
function toQueryString(query) {
  if (!query) return '';
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.append(key, value);
  });
  const text = params.toString();
  return text ? `?${text}` : '';
}

async function readProblem(response) {
  try {
    return await response.json();
  } catch {
    return null;   // 401/403 de Spring Security llegan sin body
  }
}

/** Convierte los errores por campo del backend en { campo: mensaje } para mostrarlos en el formulario. */
export function fieldErrorMap(error) {
  const map = {};
  (error?.fieldErrors ?? []).forEach(({ field, message }) => {
    if (!map[field]) map[field] = message;   // el primero de cada campo
  });
  return map;
}