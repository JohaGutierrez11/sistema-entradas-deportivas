// Fechas y horas para personas, en español de Ecuador.
// El backend envía "2026-11-07" y "15:30" (hora de Ecuador): se combinan sin conversión de zona.

const DATE = new Intl.DateTimeFormat('es-EC', { weekday: 'long', day: 'numeric', month: 'long' });
const SHORT_DATE = new Intl.DateTimeFormat('es-EC', { day: 'numeric', month: 'short', year: 'numeric' });
const DATE_TIME = new Intl.DateTimeFormat('es-EC', {
  day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'America/Guayaquil',
});

/** "2026-11-07" → "sábado, 7 de noviembre" */
export function formatMatchDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return DATE.format(new Date(year, month - 1, day));
}

/** "2026-11-07" → "7 nov 2026" */
export function formatShortDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  return SHORT_DATE.format(new Date(year, month - 1, day));
}

/**
 * El backend envía fechas-hora de Ecuador SIN zona ("2026-10-23T14:40:00").
 * Ecuador es UTC-5 todo el año (no tiene horario de verano), así que se agrega "-05:00".
 * Sin esto, un navegador en otra zona horaria calcularía mal la cuenta regresiva.
 */
export function parseEcuadorDateTime(localDateTime) {
  return new Date(`${localDateTime.slice(0, 19)}-05:00`);
}

/** "2026-10-23T14:40:00" → "23 oct, 14:40" (hora de Ecuador) */
export function formatDateTime(localDateTime) {
  return DATE_TIME.format(parseEcuadorDateTime(localDateTime));
}
