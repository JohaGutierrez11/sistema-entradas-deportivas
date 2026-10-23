import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import { useAuth } from '../../auth/AuthContext';
import StatusBadge from '../../components/StatusBadge';
import { formatShortDate } from '../../utils/format';
import { todayIso } from '../../utils/matchStatus';

const MODULES = [
  { to: '/admin/estadios', title: 'Estadios', text: 'Estadios, secciones y su ubicación en el plano.' },
  { to: '/admin/partidos', title: 'Partidos', text: 'Programar, iniciar, finalizar o cancelar.' },
  { to: '/admin/usuarios', title: 'Usuarios y roles', text: 'Activar, desactivar y desbloquear cuentas.' },
  { to: '/admin/reportes', title: 'Reportes', text: 'Reservas, ocupación por sección y PDF.' },
];

/** Inicio del administrador: lo que pasa hoy y los accesos a cada módulo. */
export default function AdminHomePage() {
  const { session } = useAuth();
  const [live, setLive] = useState(null);
  const [upcoming, setUpcoming] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([
      matchApi.searchMatchesByStatus({ status: 'EN_CURSO' }),
      matchApi.searchMatchesByStatus({ status: 'PROGRAMADO' }),
    ])
      .then(([liveMatches, scheduled]) => { setLive(liveMatches); setUpcoming(scheduled); })
      .catch((err) => setError(err.message));
  }, []);

  const today = todayIso();
  const todayMatches = [...(live ?? []), ...(upcoming ?? []).filter((m) => m.matchDate === today)];

  return (
    <section>
      <div className="page-header">
        <h1>Hola, {session?.username}</h1>
        <p className="muted">Panel del administrador</p>
      </div>

      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <div className="kpi-grid">
        <div className="card kpi">
          <span className="kpi-label">En curso ahora</span>
          <span className="kpi-value">{live ? live.length : '–'}</span>
        </div>
        <div className="card kpi">
          <span className="kpi-label">Programados desde hoy</span>
          <span className="kpi-value">{upcoming ? upcoming.length : '–'}</span>
        </div>
      </div>

      <h2 className="section-title">Hoy</h2>
      {live && upcoming && todayMatches.length === 0 && (
        <div className="card empty-state"><p className="muted">No hay partidos hoy.</p></div>
      )}
      {todayMatches.length > 0 && (
        <div className="card table-card">
          <table className="data-table">
            <tbody>
              {todayMatches.map((m) => (
                <tr key={m.id}>
                  <td><strong>{m.name}</strong><div className="muted small">{m.stadium.name}</div></td>
                  <td className="nowrap">{formatShortDate(m.matchDate)} · {m.matchTime}</td>
                  <td><StatusBadge status={m.status} /></td>
                  <td>
                    <div className="row-actions">
                      <Link className="btn btn-small" to={`/partidos/${m.id}`}>Ver mapa</Link>
                      <Link className="btn btn-small" to="/admin/partidos">Gestionar</Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2 className="section-title">Módulos</h2>
      <div className="module-grid">
        {MODULES.map((m) => (
          <Link key={m.to} to={m.to} className="card module-card">
            <strong>{m.title}</strong>
            <span className="muted small">{m.text}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
