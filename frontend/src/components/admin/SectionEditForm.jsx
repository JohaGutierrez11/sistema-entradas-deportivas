import { useState } from 'react';
import { fieldErrorMap } from '../../api/http';
import { matchApi } from '../../api/matchApi';
import FormField from '../FormField';

/**
 * Editar una sección existente: solo nombre y descripción.
 * Las filas y los asientos no se pueden cambiar porque pueden tener entradas vendidas.
 */
export default function SectionEditForm({ section, onSaved, onCancel }) {
  const [form, setForm] = useState({ name: section.name, description: section.description ?? '' });
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
    try {
      onSaved(await matchApi.updateSection(section.id, {
        name: form.name.trim(),
        description: form.description.trim() || null,
      }));
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors(fieldErrorMap(err));
        setError('Revisa los campos marcados.');
      } else {
        setError(err.message);   // 409: otra sección del estadio ya tiene ese nombre
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="card admin-form" onSubmit={handleSubmit} noValidate>
      <h2>Editar sección</h2>
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <FormField id="name" label="Nombre" value={form.name} onChange={handleChange} error={fieldErrors.name}
                 required maxLength={50} />
      <FormField id="description" label="Descripción (opcional)" value={form.description} onChange={handleChange}
                 error={fieldErrors.description} maxLength={250} />

      <p className="muted small">
        {section.rowCount} filas × {section.seatsPerRow} asientos = {section.capacity}. Las filas y los asientos no se
        pueden modificar porque pueden tener entradas vendidas.
      </p>

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={saving}>Cancelar</button>
        <button type="submit" className="btn btn-solid" disabled={saving}>
          {saving ? 'Guardando…' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  );
}
