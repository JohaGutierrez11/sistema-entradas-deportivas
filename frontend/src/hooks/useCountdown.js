import { useEffect, useState } from 'react';

/** Segundos que faltan hasta `deadline` (Date), actualizados cada segundo. Nunca negativo. */
export function useCountdown(deadline) {
  const remaining = () => (deadline ? Math.max(0, Math.round((deadline.getTime() - Date.now()) / 1000)) : 0);
  const [seconds, setSeconds] = useState(remaining);

  useEffect(() => {
    setSeconds(remaining());
    if (!deadline) return undefined;
    const id = setInterval(() => setSeconds(remaining()), 1000);
    return () => clearInterval(id);   // al salir de la página, se detiene (sin fuga de memoria)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deadline?.getTime()]);

  return seconds;
}

/** 545 → "09:05" */
export function formatCountdown(seconds) {
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0');
  const rest = String(seconds % 60).padStart(2, '0');
  return `${minutes}:${rest}`;
}
