import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { fieldErrorMap } from '../../api/http';
import FormField from '../../components/FormField';

const EMPTY_FORM = {
  firstName: '', middleName: '', lastName: '', cedula: '',
  nationality: 'Ecuatoriana', address: '', phone: '', email: '',
};

export default function RegisterPage() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  /** Un solo manejador para los 8 campos: usa el id del input como nombre del campo. */
  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: undefined }));   // al corregir, se quita su error
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);
    try {
      const trimmed = Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim()]));
      setResult(await authApi.register(trimmed));
    } catch (err) {
      if (err.status === 400) {
        setFieldErrors(fieldErrorMap(err));
        setError('Revisa los campos marcados.');
      } else {
        setError(err.message);   // 409: cédula o correo ya registrados
      }
    } finally {
      setLoading(false);
    }
  }

  if (result) {
    return (
      <main className="auth-page">
        <section className="card auth-card">
          <h1>¡Registro completo!</h1>
          <p className="alert alert-success">
            Tu usuario es <strong>{result.username}</strong>. Te enviamos tu contraseña temporal
            a <strong>{result.email}</strong>.
          </p>
          <p className="muted">Al iniciar sesión por primera vez, te pediremos cambiarla.</p>
          <Link className="btn btn-primary btn-link" to="/login">Ir a iniciar sesión</Link>
        </section>
      </main>
    );
  }

  const field = (id, label, props = {}) => (
    <FormField id={id} label={label} value={form[id]} onChange={handleChange}
               error={fieldErrors[id]} {...props} />
  );

  return (
    <main className="auth-page">
      <form className="card auth-card auth-card-wide" onSubmit={handleSubmit} noValidate>
        <h1>Crear cuenta</h1>
        {error && <p className="alert alert-error" role="alert">{error}</p>}

        <div className="form-grid">
          {field('firstName', 'Primer nombre', { required: true, autoComplete: 'given-name' })}
          {field('middleName', 'Segundo nombre (opcional)', { autoComplete: 'additional-name' })}
          {field('lastName', 'Apellido', { required: true, autoComplete: 'family-name' })}
          {field('cedula', 'Cédula', { required: true, inputMode: 'numeric', maxLength: 10, hint: '10 dígitos' })}
          {field('phone', 'Teléfono', { required: true, inputMode: 'tel', hint: 'Ej. 0991234567', autoComplete: 'tel' })}
          {field('email', 'Correo', { required: true, type: 'email', autoComplete: 'email' })}
          {field('nationality', 'Nacionalidad', { required: true })}
          {field('address', 'Dirección', { required: true, autoComplete: 'street-address' })}
        </div>

        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Registrando…' : 'Crear cuenta'}
        </button>
        <p className="muted">¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
      </form>
    </main>
  );
}