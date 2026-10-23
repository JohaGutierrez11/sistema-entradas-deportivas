/** Etiqueta + campo + mensaje de error. El error del backend aparece debajo del campo correcto. */
export default function FormField({ id, label, error, hint, ...inputProps }) {
  const messageId = `${id}-message`;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} name={id} aria-invalid={error ? 'true' : undefined}
             aria-describedby={error || hint ? messageId : undefined} {...inputProps} />
      {error
        ? <small id={messageId} className="field-error">{error}</small>
        : hint && <small id={messageId} className="muted">{hint}</small>}
    </div>
  );
}