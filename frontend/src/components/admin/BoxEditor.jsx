import { useState } from 'react';
import { matchApi } from '../../api/matchApi';

const FIELDS = [
  { key: 'x', label: 'Desde la izquierda', min: 0, max: 95 },
  { key: 'y', label: 'Desde arriba', min: 0, max: 95 },
  { key: 'width', label: 'Ancho', min: 5, max: 100 },
  { key: 'height', label: 'Alto', min: 5, max: 100 },
];

/**
 * Ubicar una sección en el plano (HU-38). Es "controlado": el recuadro vive en la página
 * para que el mapa lo dibuje mientras se mueve. Aquí solo se edita y se guarda.
 */
export default function BoxEditor({ section, box, problem, onChange, onSaved, onCancel }) {
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  function handleChange(key, value) {
    setError(null);
    onChange({ ...box, [key]: value === '' ? NaN : Number(value) });
  }

  async function handleSave() {
    setError(null);
    setSaving(true);
    try {
      onSaved(await matchApi.updateSectionBox(section.id, box));
    } catch (err) {
      // 400: el recuadro se sale del plano · 422: pisa la cancha u otra sección
      setError(err.fieldErrors.length > 0 ? err.fieldErrors.map((e) => e.message).join('. ') : err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="card admin-form">
      <h2>Ubicar {section.name}</h2>
      <p className="muted small">Valores en % del plano. El recuadro punteado del mapa se mueve mientras editas.</p>

      {FIELDS.map(({ key, label, min, max }) => (
        <div className="field box-field" key={key}>
          <label htmlFor={`box-${key}`}>{label}</label>
          <div className="box-inputs">
            <input type="range" min={min} max={max} value={Number.isNaN(box[key]) ? min : box[key]}
                   aria-label={`${label} (deslizador)`} onChange={(e) => handleChange(key, e.target.value)} />
            <input id={`box-${key}`} type="number" min={min} max={max} inputMode="numeric"
                   value={Number.isNaN(box[key]) ? '' : box[key]} onChange={(e) => handleChange(key, e.target.value)} />
          </div>
        </div>
      ))}

      {(problem || error) && <p className="alert alert-error" role="alert">{error ?? problem}</p>}

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="button" className="btn btn-solid" onClick={handleSave} disabled={saving || Boolean(problem)}>
          {saving ? 'Guardando…' : 'Guardar ubicación'}
        </button>
      </div>
    </div>
  );
}
