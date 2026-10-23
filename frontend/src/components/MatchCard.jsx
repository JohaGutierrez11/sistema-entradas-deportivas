import { Link } from 'react-router-dom';
import { formatMatchDate } from '../utils/format';
import StatusBadge from './StatusBadge';

export default function MatchCard({ match }) {
  return (
    <article className="card match-card">
      <StatusBadge status={match.status} />
      <h2 className="match-name">{match.name}</h2>
      <p className="match-when">
        <span className="capitalize">{formatMatchDate(match.matchDate)}</span> · {match.matchTime}
      </p>
      <p className="muted">{match.stadium.name}</p>
      {match.status === 'PROGRAMADO' && (
        <Link className="btn btn-solid match-cta" to={`/partidos/${match.id}`}>Ver asientos</Link>
      )}
    </article>
  );
}