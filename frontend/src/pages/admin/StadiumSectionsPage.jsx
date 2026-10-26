import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { matchApi } from '../../api/matchApi';
import BoxEditor from '../../components/admin/BoxEditor';
import SectionEditForm from '../../components/admin/SectionEditForm';
import SectionForm from '../../components/admin/SectionForm';
import StadiumMap from '../../components/StadiumMap';
import { DEFAULT_BOX, FIELD_BOX, findBoxProblem, sectionTypeLabel } from '../../utils/stadiumPlan';

const numberFormat = new Intl.NumberFormat('es-EC');

/** Secciones y plano de un escenario (HU-28 + HU-38): crear secciones y ubicarlas sobre el mapa. */
export default function StadiumSectionsPage() {
  const { stadiumId } = useParams();
  const [stadium, setStadium] = useState(null);
  const [sections, setSections] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [panel, setPanel] = useState(null);   // null · { mode: 'new' } · { mode: 'box', section, box } · { mode: 'edit', section }

  const load = useCallback(() => {
    Promise.all([matchApi.getStadium(stadiumId), matchApi.listSections(stadiumId)])
      .then(([stadiumData, sectionData]) => { setStadium(stadiumData); setSections(sectionData); })
      .catch((err) => setError(err.message));
  }, [stadiumId]);

  useEffect(load, [load]);

  const usedCapacity = useMemo(() => (sections ?? []).reduce((total, s) => total + s.capacity, 0), [sections]);
  const editingId = panel?.mode === 'box' ? panel.section.id : null;
  const problem = panel?.mode === 'box' ? findBoxProblem(panel.box, sections, editingId) : null;

  // El mapa reusa StadiumMap: la sección que se está moviendo se oculta y se dibuja como borrador
  const layout = useMemo(() => ({
    field: FIELD_BOX,
    venueType: stadium?.venueType,
    sections: (sections ?? []).filter((s) => s.id !== editingId).map((s) => ({ ...s, rows: [] })),
  }), [sections, editingId, stadium]);

  function openBoxEditor(section) {
    setNotice(null);
    setPanel({ mode: 'box', section, box: section.box ?? DEFAULT_BOX });
  }

  function handleSectionCreated(section) {
    setNotice(`Se creó ${section.name} con ${section.capacity} asientos. Ahora ubícala en el plano.`);
    setSections((current) => [...current, section]);
    setPanel({ mode: 'box', section, box: DEFAULT_BOX });
  }

  function handleSectionUpdated(section) {
    setNotice(`Se guardaron los cambios de ${section.name}.`);
    setPanel(null);
    load();
  }

  function handleBoxSaved(section) {
    setNotice(`Se guardó la ubicación de ${section.name}.`);
    setPanel(null);
    load();
  }

  if (error && !stadium) {
    return (
      <section>
        <p className="alert alert-error" role="alert">{error}</p>
        <Link to="/admin/estadios">← Volver a escenarios</Link>
      </section>
    );
  }
  if (!stadium || !sections) return <p className="muted">Cargando escenario…</p>;

  const freeCapacity = stadium.totalCapacity - usedCapacity;
  const usedPercent = Math.min(100, Math.round((usedCapacity / stadium.totalCapacity) * 100));
  const unplacedCount = sections.filter((s) => !s.box).length;

  return (
    <section>
      <Link to="/admin/estadios" className="back-link">← Escenarios</Link>
      <div className="page-header page-header-row">
        <div>
          <h1>{stadium.name}</h1>
          <p className="muted">{stadium.location}</p>
        </div>
        {!panel && (
          <button type="button" className="btn btn-solid" disabled={freeCapacity <= 0}
                  onClick={() => { setNotice(null); setPanel({ mode: 'new' }); }}>
            Nueva sección
          </button>
        )}
      </div>

      <div className="capacity" aria-label={`Capacidad usada: ${usedPercent} %`}>
        <div className="capacity-bar"><span style={{ width: `${usedPercent}%` }} /></div>
        <p className="small muted">
          {numberFormat.format(usedCapacity)} de {numberFormat.format(stadium.totalCapacity)} asientos asignados a secciones
          {freeCapacity <= 0 && ' · capacidad completa'}
        </p>
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}
      {unplacedCount > 0 && !panel && (
        <p className="alert alert-warning">
          {unplacedCount === 1 ? 'Hay 1 sección sin ubicar' : `Hay ${unplacedCount} secciones sin ubicar`}:
          el cliente la verá como botón debajo del mapa, no dentro del plano.
        </p>
      )}

      <div className="admin-plan">
        <div className="card">
          <StadiumMap layout={layout} selectedSectionId={editingId}
                      onSelectSection={(id) => openBoxEditor(sections.find((s) => s.id === id))}
                      subLabel={(s) => `${s.capacity} asientos`}
                      draft={panel?.mode === 'box' ? { ...panel.box, invalid: Boolean(problem) } : null} />
          <p className="muted small center">Toca una sección del mapa para moverla.</p>
        </div>

        <div>
          {panel?.mode === 'new' && (
            <SectionForm stadiumId={stadium.id} freeCapacity={freeCapacity}
                         onSaved={handleSectionCreated} onCancel={() => setPanel(null)} />
          )}
          {panel?.mode === 'box' && (
            <BoxEditor key={panel.section.id} section={panel.section} box={panel.box} problem={problem}
                       onChange={(box) => setPanel((current) => ({ ...current, box }))}
                       onSaved={handleBoxSaved} onCancel={() => setPanel(null)} />
          )}
          {panel?.mode === 'edit' && (
            <SectionEditForm key={panel.section.id} section={panel.section}
                             onSaved={handleSectionUpdated} onCancel={() => setPanel(null)} />
          )}
          {!panel && (
            <SectionList sections={sections} onPlace={openBoxEditor}
                         onEdit={(section) => { setNotice(null); setPanel({ mode: 'edit', section }); }} />
          )}
        </div>
      </div>
    </section>
  );
}

function SectionList({ sections, onPlace, onEdit }) {
  if (sections.length === 0) {
    return (
      <div className="card empty-state">
        <p><strong>Este escenario aún no tiene secciones.</strong></p>
        <p className="muted">Sin secciones no hay asientos que vender.</p>
      </div>
    );
  }
  return (
    <div className="card table-card">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Sección</th>
            <th scope="col">Tipo</th>
            <th scope="col" className="num">Asientos</th>
            <th scope="col"><span className="sr-only">Plano</span></th>
          </tr>
        </thead>
        <tbody>
          {sections.map((section) => (
            <tr key={section.id}>
              <td><strong>{section.name}</strong></td>
              <td>{sectionTypeLabel(section.sectionType)}</td>
              <td className="num" title={`${section.rowCount} filas × ${section.seatsPerRow}`}>
                {section.capacity} <span className="muted small">({section.rowCount}×{section.seatsPerRow})</span>
              </td>
              <td className="row-actions">
                <button type="button" className="btn btn-small" onClick={() => onEdit(section)}
                        aria-label={`Editar ${section.name}`}>
                  Editar
                </button>
                <button type="button" className={`btn btn-small ${section.box ? '' : 'btn-solid'}`}
                        onClick={() => onPlace(section)}>
                  {section.box ? 'Mover' : 'Ubicar'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
