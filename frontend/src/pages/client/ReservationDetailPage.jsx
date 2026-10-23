import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import { reservationApi } from '../../api/reservationApi';
import StatusBadge from '../../components/StatusBadge';
import TicketQr from '../../components/TicketQr';
import { formatCountdown, useCountdown } from '../../hooks/useCountdown';
import { downloadBlob } from '../../utils/download';
import { formatDateTime, formatMatchDate, parseEcuadorDateTime } from '../../utils/format';
import { buildSeatIndex, seatLabel } from '../../utils/seats';

/** Detalle de una reserva: confirmar con cuenta regresiva (HU-26), entradas con QR (HU-27) y cancelar (HU-19). */
export default function ReservationDetailPage() {
  const { publicId } = useParams();
  const [reservation, setReservation] = useState(null);
  const [match, setMatch] = useState(null);
  const [seatIndex, setSeatIndex] = useState(new Map());
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [confirmingCancel, setConfirmingCancel] = useState(false);

  const loadReservation = useCallback(async () => {
    try {
      setReservation(await reservationApi.getReservation(publicId));
    } catch (err) {
      setError(err.message);   // 404 también si la reserva es de otro cliente
    }
  }, [publicId]);

  useEffect(() => { loadReservation(); }, [loadReservation]);

  // Datos del partido y nombres de los asientos (sección, fila, número); si fallan, la reserva igual se ve
  useEffect(() => {
    if (!reservation) return;
    let current = true;
    (async () => {
      try {
        const loadedMatch = await matchApi.getMatch(reservation.matchId);
        const layout = await matchApi.getStadiumLayout(loadedMatch.stadium.id);
        if (!current) return;
        setMatch(loadedMatch);
        setSeatIndex(buildSeatIndex(layout));
      } catch {
        // degradación elegante: se muestran los ids de los asientos
      }
    })();
    return () => { current = false; };
  }, [reservation?.matchId]);   // eslint-disable-line react-hooks/exhaustive-deps

  const deadline = useMemo(
    () => (reservation?.status === 'PENDIENTE' ? parseEcuadorDateTime(reservation.expiresAt) : null),
    [reservation?.status, reservation?.expiresAt],
  );
  const secondsLeft = useCountdown(deadline);
  const expired = reservation?.status === 'PENDIENTE' && secondsLeft === 0;

  async function runAction(action) {
    setActionError(null);
    setBusy(true);
    try {
      setReservation(await action());
    } catch (err) {
      setActionError(err.message);
      await loadReservation();   // el estado pudo cambiar (por ejemplo, venció): se muestra el real
    } finally {
      setBusy(false);
      setConfirmingCancel(false);
    }
  }

  async function downloadTickets() {
    setActionError(null);
    try {
      downloadBlob(await reservationApi.getTicketsPdf(publicId), `entradas-${reservation.code}.pdf`);
    } catch (err) {
      setActionError(err.message);
    }
  }

  if (error) return <p className="alert alert-error" role="alert">{error}</p>;
  if (!reservation) return <p className="muted">Cargando la reserva…</p>;

  const label = (seatId) => (seatIndex.has(seatId) ? seatLabel(seatIndex.get(seatId)) : `Asiento #${seatId}`);
  const activeTickets = reservation.tickets.filter((ticket) => ticket.status === 'ACTIVO');
  const canCancel = reservation.status === 'PENDIENTE' ? !expired : reservation.status === 'CONFIRMADA';

  return (
    <section className="reservation">
      <Link to="/reservas">← Mis reservas</Link>

      <div className="match-header">
        <div>
          <h1>Reserva {reservation.code}</h1>
          {match && (
            <p className="match-when capitalize">
              {match.name} · {formatMatchDate(match.matchDate)} · {match.matchTime} · {match.stadium.name}
            </p>
          )}
        </div>
        <StatusBadge status={expired ? 'EXPIRADA' : reservation.status} />
      </div>

      {actionError && <p className="alert alert-error" role="alert">{actionError}</p>}

      {/* PENDIENTE: confirmar antes de que venza */}
      {reservation.status === 'PENDIENTE' && (
        <div className={`card countdown ${expired ? 'is-expired' : secondsLeft <= 60 ? 'is-urgent' : ''}`}>
          {expired ? (
            <>
              <h2>Tu reserva venció</h2>
              <p className="muted">Los asientos se liberaron. Puedes elegirlos de nuevo si siguen libres.</p>
              <Link className="btn btn-solid" to={`/partidos/${reservation.matchId}`}>Volver a elegir asientos</Link>
            </>
          ) : (
            <>
              <p className="muted">Tus asientos están retenidos. Confirma antes de que se acabe el tiempo:</p>
              <p className="countdown-time" role="timer" aria-live="off">{formatCountdown(secondsLeft)}</p>
              <p className="muted small">Vence a las {formatDateTime(reservation.expiresAt)}</p>
              <button type="button" className="btn btn-primary" disabled={busy}
                      onClick={() => runAction(() => reservationApi.confirmReservation(publicId))}>
                {busy ? 'Confirmando…' : `Confirmar compra (${reservation.seatIds.length} ${reservation.seatIds.length === 1 ? 'entrada' : 'entradas'})`}
              </button>
            </>
          )}
        </div>
      )}

      {/* Asientos (pendiente) */}
      {reservation.status === 'PENDIENTE' && (
        <div className="card">
          <h2>Asientos</h2>
          <ul className="selected-list">
            {reservation.seatIds.map((id) => <li key={id}>{label(id)}</li>)}
          </ul>
        </div>
      )}

      {/* CONFIRMADA: las entradas con su QR */}
      {reservation.tickets.length > 0 && (
        <div className="card">
          <div className="tickets-header">
            <h2>Tus entradas</h2>
            {activeTickets.length > 0 && (
              <button type="button" className="btn btn-solid btn-small" onClick={downloadTickets}>Descargar PDF</button>
            )}
          </div>
          <div className="ticket-grid">
            {reservation.tickets.map((ticket) => (
              <article key={ticket.publicId} className={`ticket ${ticket.status !== 'ACTIVO' ? 'is-void' : ''}`}>
                {ticket.status === 'ACTIVO'
                  ? <TicketQr ticketPublicId={ticket.publicId} label={label(ticket.seatId)} />
                  : <div className="qr-placeholder void">{ticket.status}</div>}
                <p className="ticket-seat">{label(ticket.seatId)}</p>
              </article>
            ))}
          </div>
          {activeTickets.length > 0 && (
            <p className="muted small">Cada QR es válido para una sola persona. No lo compartas.</p>
          )}
        </div>
      )}

      {(reservation.status === 'CANCELADA' || reservation.status === 'EXPIRADA') && (
        <p className="alert alert-info">
          Esta reserva está {reservation.status === 'CANCELADA' ? 'cancelada' : 'expirada'}: sus asientos ya fueron liberados.
        </p>
      )}

      {/* Cancelar: con confirmación en dos pasos */}
      {canCancel && (
        <div className="cancel-zone">
          {confirmingCancel ? (
            <>
              <span>¿Seguro que quieres cancelar? Se liberarán todos los asientos de esta reserva.</span>
              <button type="button" className="btn btn-danger btn-small" disabled={busy}
                      onClick={() => runAction(() => reservationApi.cancelReservation(publicId))}>
                Sí, cancelar
              </button>
              <button type="button" className="btn btn-small" onClick={() => setConfirmingCancel(false)}>No</button>
            </>
          ) : (
            <button type="button" className="btn btn-danger-outline" onClick={() => setConfirmingCancel(true)}>
              Cancelar reserva
            </button>
          )}
        </div>
      )}
    </section>
  );
}
