import { useState } from 'react';
import { fieldErrorMap } from '../../api/http';
import { matchApi } from '../../api/matchApi';
import { todayIso } from '../../utils/matchStatus';
import FormField from '../FormField';

const EMPTY = { name: '', description: '', matchDate: '', matchTime: '', stadiumId: '' };

/**
 * Crear o editar un partido. Al editar, el estadio queda fijo (regla del backend: los asientos
 * reservados pertenecen al estadio original) y el estado no se toca aquí: se cambia con los botones.
 */
export default function MatchForm({ match, stadiums, onSaved, onCancel }) {
  const editing = Boolean(match);
  const [form, setForm] = useState(editing
    ? { name: match.name, description: match.description ?? '', matchDate: match.matchDate,
        matchTime: match.matchTime, stadiumId: String(match.stadium.id) }
    : EMPTY);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

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
    const body = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      matchDate: form.matchDate || null,
      matchTime: form.matchTime || null,
      stadiumId: form.stadiumId ? Number(form.stadiumId) : null,
    };
    try {
      const saved = editing
        ? await matchApi.updateMatch(match.id, { ...body, status: match.status })
        : await matchApi.createMatch(body);
      onSaved(saved, editing);
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors(fieldErrorMap(err));
        setError('Revisa los campos marcados.');
      } else {
        setError(err.message);   // 404 estadio · 422 fecha pasada o partido que ya no se puede modificar
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
      <h2>{editing ? `Editar ${match.name}` : 'Nuevo partido'}</h2>
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <div className="form-grid">
        {field('name', 'Nombre', { required: true, maxLength: 100, hint: 'Ej. Liga de Quito vs Emelec' })}

        <div className="field">
          <label htmlFor="stadiumId">Escenario</label>
          <select id="stadiumId" name="stadiumId" value={form.stadiumId} onChange={handleChange}
                  disabled={editing} aria-invalid={fieldErrors.stadiumId ? 'true' : undefined}
                  aria-describedby="stadiumId-message">
            <option value="">Elige un estadio o coliseo</option>
            {stadiums.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          {fieldErrors.stadiumId
            ? <small id="stadiumId-message" className="field-error">{fieldErrors.stadiumId}</small>
            : editing && <small id="stadiumId-message" className="muted">No se cambia: cancela el partido y crea otro</small>}
        </div>

        {field('matchDate', 'Fecha', { required: true, type: 'date', min: todayIso() })}
        {field('matchTime', 'Hora', { required: true, type: 'time', hint: 'Hora de Ecuador' })}
      </div>
      {field('description', 'Descripción (opcional)', { maxLength: 250, hint: 'Ej. LigaPro · Fecha 18' })}

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="btn btn-solid" disabled={saving}>
          {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear partido'}
        </button>
      </div>
    </form>
  );
}
