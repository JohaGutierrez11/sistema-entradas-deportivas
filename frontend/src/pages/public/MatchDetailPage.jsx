import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import { formatMatchDate } from '../../utils/format';

/** Temporal: en el paso 4 aquí va el mapa del estadio. */
export default function MatchDetailPage() {
  const { matchId } = useParams();
  const [match, setMatch] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    matchApi.getMatch(matchId).then(setMatch).catch((err) => setError(err.message));
  }, [matchId]);

  if (error) return <p className="alert alert-error">{error}</p>;
  if (!match) return <p className="muted">Cargando…</p>;

  return (
    <section className="card">
      <Link to="/">← Volver a los partidos</Link>
      <h1>{match.name}</h1>
      <p className="capitalize">{formatMatchDate(match.matchDate)} · {match.matchTime} · {match.stadium.name}</p>
      <p className="muted">El mapa de asientos llega en el siguiente paso. 🏟️</p>
    </section>
  );
}