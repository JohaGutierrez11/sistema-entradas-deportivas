import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { reservationApi } from '../../api/reservationApi';
import StatusBadge from '../../components/StatusBadge';
import { downloadBlob } from '../../utils/download';
import { formatDateTime, formatMatchDate, parseEcuadorDateTime } from '../../utils/format';

const isActive = (status) => status === 'PENDIENTE' || status === 'CONFIRMADA';

/** ¿El partido ya empezó? Sin datos del partido (match-service caído) se asume que no. */
function hasStarted(match) {
  return Boolean(match) && parseEcuadorDateTime(`${match.matchDate}T${match.matchTime}:00`).getTime() <= Date.now();
}

/**
 * Filtros de la lista (en el front: un cliente tiene pocas reservas). "Historial" es TODO:
 * un historial que esconde las canceladas no es un historial.
 */
const FILTERS = [
  // Una PENDIENTE siempre es próxima: si el partido empezó, igual vence al cierre de la venta (HU-40)
  { id: 'upcoming', label: 'Próximas',
    matches: (item) => item.status === 'PENDIENTE' || (item.status === 'CONFIRMADA' && !hasStarted(item.match)) },
  { id: 'played', label: 'Partidos vistos', matches: (item) => item.status === 'CONFIRMADA' && hasStarted(item.match) },
  { id: 'inactive', label: 'Canceladas y vencidas', matches: (item) => !isActive(item.status) },
  { id: 'all', label: 'Historial', matches: () => true },
];

/** Una PENDIENTE ya vencida se muestra como EXPIRADA aunque la tarea automática aún no haya pasado. */
function effectiveStatus(reservation) {
  const expired = reservation.status === 'PENDIENTE'
    && parseEcuadorDateTime(reservation.expiresAt).getTime() <= Date.now();
  return expired ? 'EXPIRADA' : reservation.status;
}

/** "Mis reservas" (HU-20): el historial del cliente del token, con acceso al detalle y al PDF. */
export default function MyReservationsPage() {
  const [items, setItems] = useState(null);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('upcoming');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    let current = true;
    reservationApi.getMyReservations()
      .then((data) => { if (current) setItems(data); })
      .catch((err) => { if (current) setError(err.message); });
    return () => { current = false; };
  }, []);

  const withStatus = useMemo(
    () => (items ?? []).map((item) => ({ ...item, status: effectiveStatus(item.reservation) })),
    [items],
  );
  const counts = useMemo(
    () => Object.fromEntries(FILTERS.map((f) => [f.id, withStatus.filter((item) => f.matches(item)).length])),
    [withStatus],
  );
  const visible = withStatus.filter((item) => FILTERS.find((f) => f.id === filter).matches(item));

  async function downloadPdf() {
    setError(null);
    setDownloading(true);
    try {
      downloadBlob(await reservationApi.getMyReservationsPdf(), 'mis-reservas.pdf');
    } catch (err) {
      setError(err.message);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <section>
      <div className="page-header page-header-row">
        <h1>Mis reservas</h1>
        {items?.length > 0 && (
          <button type="button" className="btn btn-small" onClick={downloadPdf} disabled={downloading}>
            {downloading ? 'Generando…' : 'Descargar historial (PDF)'}
          </button>
        )}
      </div>

      {error && <p className="alert alert-error" role="alert">{error}</p>}

      {items === null && !error && <p className="muted">Cargando tus reservas…</p>}

      {items?.length === 0 && (
        <div className="card empty-state">
          <p><strong>Aún no tienes reservas.</strong></p>
          <p className="muted">Elige un partido y reserva tus asientos.</p>
          <Link className="btn btn-solid" to="/">Ver partidos</Link>
        </div>
      )}

      {items?.length > 0 && (
        <>
          <div className="segmented" role="tablist" aria-label="Filtrar reservas">
            {FILTERS.map((f) => (
              <button key={f.id} type="button" role="tab" aria-selected={filter === f.id}
                      className={filter === f.id ? 'is-active' : ''} onClick={() => setFilter(f.id)}>
                {f.label} <span className="count">{counts[f.id]}</span>
              </button>
            ))}
          </div>

          {visible.length === 0 ? (
            <p className="muted">No tienes reservas en esta lista.</p>
          ) : (
            <ul className="reservation-list">
              {visible.map(({ reservation, match, status }) => {
                const seats = reservation.tickets.length > 0 ? reservation.tickets.length : reservation.seatIds.length;
                return (
                  <li key={reservation.publicId}>
                    <Link to={`/reservas/${reservation.publicId}`} className="card reservation-row">
                      <div className="reservation-main">
                        <p className="reservation-match">
                          {match ? match.name : `Partido #${reservation.matchId}`}
                        </p>
                        <p className="muted">
                          {match
                            ? `${formatMatchDate(match.matchDate)} · ${match.matchTime} · ${match.stadiumName}`
                            : 'Información del partido no disponible'}
                        </p>
                        <p className="muted small">
                          {reservation.code} · {seats === 0 ? 'asientos liberados' : `${seats} ${seats === 1 ? 'asiento' : 'asientos'}`}
                          {status === 'PENDIENTE' && ` · vence ${formatDateTime(reservation.expiresAt)}`}
                        </p>
                      </div>
                      <div className="reservation-side">
                        <StatusBadge status={status} />
                        <span className="chevron" aria-hidden="true">›</span>
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}
    </section>
  );
}
