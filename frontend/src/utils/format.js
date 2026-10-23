// Fechas y horas para personas, en español de Ecuador.
// El backend envía "2026-11-07" y "15:30" (hora de Ecuador): se combinan sin conversión de zona.

const DATE = new Intl.DateTimeFormat('es-EC', { weekday: 'long', day: 'numeric', month: 'long' });
const SHORT_DATE = new Intl.DateTimeFormat('es-EC', { day: 'numeric', month: 'short', year: 'numeric' });

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