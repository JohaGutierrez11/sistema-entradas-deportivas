const LABELS = {
  PROGRAMADO: 'Programado', EN_CURSO: 'En curso', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado',
  PENDIENTE: 'Pendiente', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', EXPIRADA: 'Expirada',
  ACTIVO: 'Activo', INACTIVO: 'Inactivo', BLOQUEADO: 'Bloqueado',
};

/** Etiqueta de estado. Sirve para partidos y reservas: el color sale del estado. */
export default function StatusBadge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status] ?? status}</span>;
}