/**
 * Máquina de estados del partido, copiada de MatchStatus.canTransitionTo (match-service).
 * El front la usa para ofrecer solo los botones válidos; el backend la vuelve a validar (422).
 *
 *   PROGRAMADO ──► EN_CURSO ──► FINALIZADO
 *        └──────► CANCELADO
 */
export const MATCH_STATUSES = [
  { value: 'PROGRAMADO', label: 'Programados' },
  { value: 'EN_CURSO', label: 'En curso' },
  { value: 'FINALIZADO', label: 'Finalizados' },
  { value: 'CANCELADO', label: 'Cancelados' },
];

const NEXT = {
  PROGRAMADO: ['EN_CURSO', 'CANCELADO'],
  EN_CURSO: ['FINALIZADO'],
  FINALIZADO: [],
  CANCELADO: [],
};

export const nextStatuses = (status) => NEXT[status] ?? [];

/** FINALIZADO y CANCELADO ya son historia: no se editan. */
export const isFinalStatus = (status) => nextStatuses(status).length === 0;

/** Texto del botón y si la acción necesita confirmación (no se puede deshacer). */
export const TRANSITION_ACTIONS = {
  EN_CURSO: { label: 'Iniciar', confirm: false },
  FINALIZADO: { label: 'Finalizar', confirm: true },
  CANCELADO: { label: 'Cancelar partido', confirm: true },
};

/** Hoy en formato YYYY-MM-DD con la fecha LOCAL (toISOString usaría UTC y en la noche daría mañana). */
export function todayIso() {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}
