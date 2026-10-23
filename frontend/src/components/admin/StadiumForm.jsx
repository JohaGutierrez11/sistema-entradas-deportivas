import { useState } from 'react';
import { fieldErrorMap } from '../../api/http';
import { matchApi } from '../../api/matchApi';
import FormField from '../FormField';

const EMPTY = { name: '', location: '', totalCapacity: '', description: '' };

/** Crear o editar un estadio. Con `stadium` edita (PUT); sin él, crea (POST). */
export default function StadiumForm({ stadium, onSaved, onCancel }) {
  const editing = Boolean(stadium);
  const [form, setForm] = useState(editing
    ? { name: stadium.name, location: stadium.location, totalCapacity: String(stadium.totalCapacity),
        description: stadium.description ?? '' }
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
      location: form.location.trim(),
      // Vacío → null: así el backend responde "no debe ser nulo" en el campo correcto
      totalCapacity: form.totalCapacity === '' ? null : Number(form.totalCapacity),
      description: form.description.trim() || null,
    };
    try {
      const saved = editing ? await matchApi.updateStadium(stadium.id, body) : await matchApi.createStadium(body);
      onSaved(saved, editing);
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors(fieldErrorMap(err));
        setError('Revisa los campos marcados.');
      } else {
        setError(err.message);   // 409 nombre repetido · 422 capacidad menor que sus secciones
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
      <h2>{editing ? `Editar ${stadium.name}` : 'Nuevo estadio'}</h2>
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <div className="form-grid">
        {field('name', 'Nombre', { required: true, maxLength: 100 })}
        {field('location', 'Ubicación', { required: true, maxLength: 200, hint: 'Ej. Av. 6 de Diciembre, Quito' })}
        {field('totalCapacity', 'Capacidad total', {
          required: true, type: 'number', min: 1, max: 200000, inputMode: 'numeric',
          hint: editing ? 'No puede ser menor que la suma de sus secciones' : 'Entre 1 y 200 000',
        })}
        {field('description', 'Descripción (opcional)', { maxLength: 250 })}
      </div>

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="btn btn-solid" disabled={saving}>
          {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear estadio'}
        </button>
      </div>
    </form>
  );
}
