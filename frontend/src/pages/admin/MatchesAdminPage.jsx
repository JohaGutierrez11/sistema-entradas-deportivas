import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import MatchForm from '../../components/admin/MatchForm';
import StatusBadge from '../../components/StatusBadge';
import { formatShortDate } from '../../utils/format';
import { isFinalStatus, MATCH_STATUSES, nextStatuses, TRANSITION_ACTIONS } from '../../utils/matchStatus';

/** Partidos (HU-28): crear, editar y mover por la máquina de estados (iniciar, finalizar, cancelar). */
export default function MatchesAdminPage() {
  const [status, setStatus] = useState('PROGRAMADO');
  const [stadiumId, setStadiumId] = useState('');
  const [stadiums, setStadiums] = useState([]);
  const [matches, setMatches] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [editing, setEditing] = useState(null);       // null · 'new' · partido
  const [confirming, setConfirming] = useState(null); // { matchId, next }
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    matchApi.listStadiums().then(setStadiums).catch((err) => setError(err.message));
  }, []);

  const load = useCallback(() => {
    setMatches(null);
    matchApi.searchMatchesByStatus({ status, stadiumId })
      .then(setMatches)
      .catch((err) => setError(err.message));
  }, [status, stadiumId]);

  useEffect(load, [load]);

  function clearMessages() {
    setNotice(null);
    setError(null);
    setConfirming(null);
  }

  function handleSaved(saved, wasEditing) {
    setEditing(null);
    setNotice(wasEditing ? `Se guardaron los cambios de ${saved.name}.` : `Se programó ${saved.name}.`);
    if (!wasEditing && status !== 'PROGRAMADO') setStatus('PROGRAMADO');   // para que se vea el nuevo
    else load();
  }

  /** Cambiar el estado: el PUT necesita todos los campos, así que se reenvían los actuales. */
  async function changeStatus(match, next) {
    clearMessages();
    setBusyId(match.id);
    try {
      const saved = await matchApi.updateMatch(match.id, {
        name: match.name, description: match.description, matchDate: match.matchDate,
        matchTime: match.matchTime, stadiumId: match.stadium.id, status: next,
      });
      const label = MATCH_STATUSES.find((s) => s.value === next).label.toLowerCase();
      setNotice(`${saved.name} pasó a ${label}.`);
      load();
    } catch (err) {
      setError(err.message);   // 422: transición no permitida (otro admin ya lo cambió, por ejemplo)
    } finally {
      setBusyId(null);
    }
  }

  return (
    <section>
      <div className="page-header page-header-row">
        <div>
          <h1>Partidos</h1>
          <p className="muted">Programa partidos y actualiza su estado el día del evento.</p>
        </div>
        {!editing && (
          <button type="button" className="btn btn-solid" disabled={stadiums.length === 0}
                  onClick={() => { clearMessages(); setEditing('new'); }}>
            Nuevo partido
          </button>
        )}
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      {editing && (
        <MatchForm key={editing === 'new' ? 'new' : editing.id} match={editing === 'new' ? null : editing}
                   stadiums={stadiums} onSaved={handleSaved} onCancel={() => setEditing(null)} />
      )}

      <div className="admin-toolbar">
        <div className="segmented" role="tablist" aria-label="Estado de los partidos">
          {MATCH_STATUSES.map((s) => (
            <button key={s.value} type="button" role="tab" aria-selected={status === s.value}
                    className={status === s.value ? 'is-active' : ''}
                    onClick={() => { clearMessages(); setStatus(s.value); }}>
              {s.label}
            </button>
          ))}
        </div>
        <label className="toolbar-filter">
          <span className="sr-only">Estadio</span>
          <select value={stadiumId} onChange={(e) => setStadiumId(e.target.value)}>
            <option value="">Todos los estadios</option>
            {stadiums.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </label>
      </div>
      {status === 'PROGRAMADO' && <p className="muted small">Se muestran los programados desde hoy.</p>}

      {matches === null && !error && <p className="muted">Cargando partidos…</p>}
      {matches?.length === 0 && <div className="card empty-state"><p className="muted">No hay partidos en esta lista.</p></div>}

      {matches?.length > 0 && (
        <div className="card table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Partido</th>
                <th scope="col">Fecha</th>
                <th scope="col">Estadio</th>
                <th scope="col">Estado</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {matches.map((match) => (
                <tr key={match.id}>
                  <td>
                    <strong>{match.name}</strong>
                    {match.description && <div className="muted small">{match.description}</div>}
                  </td>
                  <td className="nowrap">{formatShortDate(match.matchDate)} · {match.matchTime}</td>
                  <td>{match.stadium.name}</td>
                  <td><StatusBadge status={match.status} /></td>
                  <td>
                    <MatchActions match={match} busy={busyId === match.id} confirming={confirming}
                                  onEdit={() => { clearMessages(); setEditing(match); }}
                                  onAsk={(next) => { setNotice(null); setError(null); setConfirming({ matchId: match.id, next }); }}
                                  onCancelAsk={() => setConfirming(null)}
                                  onChange={(next) => changeStatus(match, next)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

/** Solo los botones que la máquina de estados permite. Finalizar y cancelar piden confirmación. */
function MatchActions({ match, busy, confirming, onEdit, onAsk, onCancelAsk, onChange }) {
  if (confirming?.matchId === match.id) {
    const action = TRANSITION_ACTIONS[confirming.next];
    return (
      <div className="row-actions">
        <span className="small">¿{action.label}? No se puede deshacer.</span>
        <button type="button" className="btn btn-small btn-danger" disabled={busy}
                onClick={() => onChange(confirming.next)}>Sí</button>
        <button type="button" className="btn btn-small" disabled={busy} onClick={onCancelAsk}>No</button>
      </div>
    );
  }
  if (isFinalStatus(match.status)) {
    return (
      <div className="row-actions">
        <Link className="btn btn-small" to={`/partidos/${match.id}`}>Ver</Link>
      </div>
    );
  }
  return (
    <div className="row-actions">
      {match.status === 'PROGRAMADO' && <button type="button" className="btn btn-small" onClick={onEdit}>Editar</button>}
      {nextStatuses(match.status).map((next) => {
        const action = TRANSITION_ACTIONS[next];
        const danger = next === 'CANCELADO';
        return (
          <button key={next} type="button" disabled={busy}
                  className={`btn btn-small ${danger ? 'btn-danger-outline' : ''}`}
                  onClick={() => (action.confirm ? onAsk(next) : onChange(next))}>
            {busy ? '…' : action.label}
          </button>
        );
      })}
    </div>
  );
}
