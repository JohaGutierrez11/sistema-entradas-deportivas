import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import { reservationApi } from '../../api/reservationApi';
import { useAuth } from '../../auth/AuthContext';
import SeatGrid from '../../components/SeatGrid';
import StadiumMap from '../../components/StadiumMap';
import StatusBadge from '../../components/StatusBadge';
import { usePolling } from '../../hooks/usePolling';
import { formatMatchDate } from '../../utils/format';
import { buildSeatIndex, seatLabel } from '../../utils/seats';

const MAX_SEATS = 6;   // comodidad del front; la regla real (con tus otras reservas) la aplica el backend

export default function MatchDetailPage() {
  const { matchId } = useParams();
  const { isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [match, setMatch] = useState(null);
  const [layout, setLayout] = useState(null);
  const [availability, setAvailability] = useState(null);
  const [sectionId, setSectionId] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [reserving, setReserving] = useState(false);

  // 1. La estructura, UNA vez: el partido y el plano de su estadio
  useEffect(() => {
    let current = true;
    (async () => {
      try {
        const loadedMatch = await matchApi.getMatch(matchId);
        const loadedLayout = await matchApi.getStadiumLayout(loadedMatch.stadium.id);
        if (!current) return;
        setMatch(loadedMatch);
        setLayout(loadedLayout);
        setSectionId(loadedLayout.sections[0]?.id ?? null);
      } catch (err) {
        if (current) setError(err.message);
      }
    })();
    return () => { current = false; };
  }, [matchId]);

  // 2. La disponibilidad, ahora y cada refreshSeconds (solo si el partido está programado)
  const loadAvailability = useCallback(async () => {
    try {
      setAvailability(await reservationApi.getAvailability(matchId));
    } catch {
      setNotice('No se pudo actualizar la disponibilidad. Reintentando…');
    }
  }, [matchId]);

  useEffect(() => { loadAvailability(); }, [loadAvailability]);
  usePolling(loadAvailability, availability?.refreshSeconds ?? 5, match?.status === 'PROGRAMADO');

  const seatIndex = useMemo(() => buildSeatIndex(layout), [layout]);
  const occupied = useMemo(
    () => new Map((availability?.occupiedSeats ?? []).map((seat) => [seat.seatId, seat.status])),
    [availability],
  );

  // 3. Si alguien tomó un asiento que tenías elegido, se quita de tu selección y se avisa
  useEffect(() => {
    const taken = selectedSeats.filter((id) => occupied.has(id));
    if (taken.length > 0) {
      setSelectedSeats((current) => current.filter((id) => !occupied.has(id)));
      setNotice(`${taken.map((id) => seatLabel(seatIndex.get(id))).join(', ')} acaba de ser tomado por otra persona.`);
    }
  }, [occupied, selectedSeats, seatIndex]);

  function toggleSeat(seatId) {
    setNotice(null);
    if (selectedSeats.includes(seatId)) {
      setSelectedSeats(selectedSeats.filter((id) => id !== seatId));
    } else if (selectedSeats.length >= MAX_SEATS) {
      setNotice(`Puedes elegir hasta ${MAX_SEATS} asientos por partido.`);
    } else {
      setSelectedSeats([...selectedSeats, seatId]);
    }
  }

  async function handleReserve() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } });   // al volver, sigue aquí
      return;
    }
    setNotice(null);
    setReserving(true);   // el botón se desactiva: un doble clic no crea dos reservas
    try {
      const reservation = await reservationApi.createReservation(Number(matchId), selectedSeats);
      navigate(`/reservas/${reservation.publicId}`);
    } catch (err) {
      // 409: alguien ganó el asiento · 422: superas el máximo o el partido cambió · 404: asiento inválido
      setNotice(err.message);
      if (err.status === 409) await loadAvailability();   // se pinta enseguida quién lo tomó
    } finally {
      setReserving(false);
    }
  }

  if (error) return <p className="alert alert-error" role="alert">{error}</p>;
  if (!match || !layout) return <p className="muted">Cargando el estadio…</p>;

  const section = layout.sections.find((s) => s.id === sectionId);
  const canReserve = match.status === 'PROGRAMADO' && !isAdmin;

  return (
    <section>
      <Link to="/">← Volver a los partidos</Link>
      <div className="match-header">
        <div>
          <h1>{match.name}</h1>
          <p className="match-when capitalize">
            {formatMatchDate(match.matchDate)} · {match.matchTime} · {match.stadium.name}
          </p>
        </div>
        <StatusBadge status={match.status} />
      </div>

      {notice && <p className="alert alert-info" role="status">{notice}</p>}

      <div className="booking">
        <div className="booking-main card">
          <StadiumMap layout={layout} occupied={occupied} selectedSectionId={sectionId} onSelectSection={setSectionId} />
          {section && (
            <SeatGrid section={section} field={layout.field} occupied={occupied} selectedSeats={selectedSeats}
                      onToggleSeat={toggleSeat} readOnly={!canReserve} />
          )}
        </div>

        <aside className="booking-summary card">
          <h2>Tu selección</h2>
          {selectedSeats.length === 0 ? (
            <p className="muted">Elige una sección en el plano y toca tus asientos.</p>
          ) : (
            <ul className="selected-list">
              {selectedSeats.map((id) => (
                <li key={id}>
                  <span>{seatLabel(seatIndex.get(id))}</span>
                  <button type="button" className="link-button" aria-label="Quitar" onClick={() => toggleSeat(id)}>✕</button>
                </li>
              ))}
            </ul>
          )}
          <p className="muted">{selectedSeats.length} de {MAX_SEATS} asientos</p>

          {canReserve ? (
            <button type="button" className="btn btn-primary" disabled={selectedSeats.length === 0 || reserving}
                    onClick={handleReserve}>
              {reserving ? 'Reservando…'
                : isAuthenticated ? `Reservar ${selectedSeats.length || ''}`.trim() : 'Inicia sesión para reservar'}
            </button>
          ) : (
            <p className="muted">{isAdmin ? 'El administrador no reserva entradas.' : 'Este partido ya no admite reservas.'}</p>
          )}
          <p className="muted small">La disponibilidad se actualiza cada {availability?.refreshSeconds ?? 5} s.</p>
        </aside>
      </div>
    </section>
  );
}