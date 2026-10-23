import { useEffect, useRef } from 'react';

/**
 * Ejecuta `callback` cada `seconds` segundos mientras `enabled` sea true.
 * Si la pestaña no está visible, se salta la llamada: no se gasta red ni servidor.
 */
export function usePolling(callback, seconds, enabled = true) {
  const savedCallback = useRef(callback);

  // Siempre usar la versión más nueva del callback, sin reiniciar el intervalo
  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  useEffect(() => {
    if (!enabled || !seconds) return undefined;
    const id = setInterval(() => {
      if (document.visibilityState === 'visible') savedCallback.current();
    }, seconds * 1000);
    return () => clearInterval(id);   // al salir de la página, se detiene
  }, [seconds, enabled]);
}