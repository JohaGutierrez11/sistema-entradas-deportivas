import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import StadiumForm from '../../components/admin/StadiumForm';
import { venueTypeLabel } from '../../utils/stadiumPlan';

const numberFormat = new Intl.NumberFormat('es-EC');

/** Escenarios deportivos, estadios y coliseos (HU-28, HU-42): listar, crear, editar y eliminar. Las secciones se gestionan en el detalle. */
export default function StadiumsAdminPage() {
  const [stadiums, setStadiums] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [editing, setEditing] = useState(null);           // null · 'new' · estadio
  const [confirmingDelete, setConfirmingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(() => {
    matchApi.listStadiums().then(setStadiums).catch((err) => setError(err.message));
  }, []);

  useEffect(load, [load]);

  function handleSaved(saved, wasEditing) {
    setEditing(null);
    setNotice(wasEditing ? `Se guardaron los cambios de ${saved.name}.` : `Se creó el estadio ${saved.name}.`);
    load();
  }

  function startEditing(value) {
    setNotice(null);
    setError(null);
    setConfirmingDelete(null);
    setEditing(value);
  }

  async function handleDelete(stadium) {
    setError(null);
    setNotice(null);
    setDeleting(true);
    try {
      await matchApi.deleteStadium(stadium.id);
      setNotice(`Se eliminó ${stadium.name}.`);
      load();
    } catch (err) {
      setError(err.message);   // 409: tiene secciones o partidos asociados
    } finally {
      setDeleting(false);
      setConfirmingDelete(null);
    }
  }

  return (
    <section>
      <div className="page-header page-header-row">
        <div>
          <h1>Escenarios</h1>
          <p className="muted">Estadios y coliseos: créalos y luego arma sus secciones y su plano.</p>
        </div>
        {!editing && (
          <button type="button" className="btn btn-solid" onClick={() => startEditing('new')}>Nuevo escenario</button>
        )}
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      {editing && (
        <StadiumForm key={editing === 'new' ? 'new' : editing.id}
                     stadium={editing === 'new' ? null : editing}
                     onSaved={handleSaved} onCancel={() => setEditing(null)} />
      )}

      {stadiums === null && !error && <p className="muted">Cargando escenarios…</p>}

      {stadiums?.length === 0 && (
        <div className="card empty-state">
          <p><strong>Todavía no hay escenarios.</strong></p>
          <p className="muted">Crea el primero para poder programar partidos.</p>
        </div>
      )}

      {stadiums?.length > 0 && (
        <div className="card table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Tipo</th>
                <th scope="col">Ubicación</th>
                <th scope="col" className="num">Capacidad</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {stadiums.map((stadium) => (
                <tr key={stadium.id}>
                  <td><strong>{stadium.name}</strong></td>
                  <td>{venueTypeLabel(stadium.venueType)}</td>
                  <td>{stadium.location}</td>
                  <td className="num">{numberFormat.format(stadium.totalCapacity)}</td>
                  <td>
                    {confirmingDelete === stadium.id ? (
                      <div className="row-actions">
                        <span className="small">¿Eliminar?</span>
                        <button type="button" className="btn btn-small btn-danger" disabled={deleting}
                                onClick={() => handleDelete(stadium)}>
                          {deleting ? 'Eliminando…' : 'Sí, eliminar'}
                        </button>
                        <button type="button" className="btn btn-small" disabled={deleting}
                                onClick={() => setConfirmingDelete(null)}>No</button>
                      </div>
                    ) : (
                      <div className="row-actions">
                        <Link className="btn btn-small" to={`/admin/estadios/${stadium.id}`}>Secciones y plano</Link>
                        <button type="button" className="btn btn-small" onClick={() => startEditing(stadium)}>Editar</button>
                        <button type="button" className="btn btn-small btn-danger-outline"
                                onClick={() => { setNotice(null); setError(null); setConfirmingDelete(stadium.id); }}>
                          Eliminar
                        </button>
                      </div>
                    )}
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
