import { useState } from 'react';
import { fieldErrorMap } from '../../api/http';
import { matchApi } from '../../api/matchApi';
import { SECTION_TYPES } from '../../utils/stadiumPlan';
import FormField from '../FormField';

const EMPTY = { name: '', sectionType: 'GENERAL', rowCount: '', seatsPerRow: '', description: '' };
const toNumber = (value) => (value === '' ? null : Number(value));

/**
 * Crear una sección. El backend genera las filas (A, B, C…) y los asientos (1..N).
 * Se ubica en el plano después, con el editor de recuadro: así cada formulario hace una sola cosa.
 */
export default function SectionForm({ stadiumId, freeCapacity, onSaved, onCancel }) {
  const [form, setForm] = useState(EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const capacity = (toNumber(form.rowCount) ?? 0) * (toNumber(form.seatsPerRow) ?? 0);
  const exceeds = capacity > freeCapacity;

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setSaving(true);
    try {
      const saved = await matchApi.createSection(stadiumId, {
        name: form.name.trim(),
        sectionType: form.sectionType,
        rowCount: toNumber(form.rowCount),
        seatsPerRow: toNumber(form.seatsPerRow),
        description: form.description.trim() || null,
      });
      onSaved(saved);
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors(fieldErrorMap(err));
        setError('Revisa los campos marcados.');
      } else {
        setError(err.message);   // 409 nombre repetido · 422 supera la capacidad del estadio
      }
    } finally {
      setSaving(false);
    }
  }

  const field = (id, label, props = {}) => (
    <FormField id={id} label={label} value={form[id]} onChange={handleChange} error={fieldErrors[id]} {...props} />
  );

  return (
    <form className="card admin-form" onSubmit={handleSubmit} noValidate>
      <h2>Nueva sección</h2>
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      {field('name', 'Nombre', { required: true, maxLength: 50, hint: 'Ej. Tribuna Norte' })}

      <div className="field">
        <label htmlFor="sectionType">Tipo</label>
        <select id="sectionType" name="sectionType" value={form.sectionType} onChange={handleChange}>
          {SECTION_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
        </select>
      </div>

      <div className="form-grid form-grid-2">
        {field('rowCount', 'Filas', { required: true, type: 'number', min: 1, max: 26, hint: '1 a 26 (A–Z)' })}
        {field('seatsPerRow', 'Asientos por fila', { required: true, type: 'number', min: 1, max: 100, hint: '1 a 100' })}
      </div>

      {field('description', 'Descripción (opcional)', { maxLength: 250 })}

      <p className={exceeds ? 'alert alert-error' : 'muted small'} aria-live="polite">
        Capacidad de la sección: <strong>{capacity}</strong> · disponible en el estadio: <strong>{freeCapacity}</strong>
        {exceeds && ' — supera la capacidad del estadio'}
      </p>

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="btn btn-solid" disabled={saving || exceeds}>
          {saving ? 'Creando…' : 'Crear sección'}
        </button>
      </div>
    </form>
  );
}
