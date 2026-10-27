import { useEffect, useState } from 'react';
import { reservationApi } from '../../api/reservationApi';

/** Nombre y explicación de cada parámetro, en palabras del negocio. */
const LABELS = {
  MAX_TICKETS_PER_MATCH: {
    title: 'Entradas por cliente y partido',
    help: 'Máximo de entradas que un cliente puede tener en un mismo partido, sumando todas sus reservas activas.',
  },
  HOLD_MINUTES: {
    title: 'Tiempo para confirmar una reserva',
    help: 'Minutos que los asientos quedan retenidos mientras el cliente confirma. Al vencer, se liberan solos.',
  },
  SALES_CLOSE_OFFSET_MINUTES: {
    title: 'Cierre de la venta online',
    help: 'Minutos respecto de la hora de inicio: 10 = cierra 10 minutos después del inicio; -30 = cierra 30 minutos antes.',
  },
  EXPIRATION_JOB_SECONDS: {
    title: 'Revisión de reservas vencidas',
    help: 'Cada cuántos segundos el sistema busca reservas pendientes que ya vencieron.',
  },
  AVAILABILITY_REFRESH_SECONDS: {
    title: 'Actualización del mapa de asientos',
    help: 'Cada cuántos segundos el mapa del cliente vuelve a consultar qué asientos están ocupados.',
  },
  CANCEL_DEADLINE_HOURS: {
    title: 'Plazo para cancelar una reserva confirmada',
    help: 'Horas antes del inicio hasta las que el cliente puede cancelar. Con 0 puede cancelar hasta que empiece el partido. El administrador no tiene este límite.',
  },
  RESERVATION_CODE_PREFIX: {
    title: 'Prefijo del código de reserva',
    help: 'Solo lectura: cambiarlo mezclaría formatos de código en el historial de reservas.',
  },
};

/** Configuración del sistema (HU-43): parámetros de negocio que el administrador ajusta sin tocar código. */
export default function SettingsAdminPage() {
  const [parameters, setParameters] = useState(null);
  const [drafts, setDrafts] = useState({});
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState(null);
  const [savingCode, setSavingCode] = useState(null);

  useEffect(() => {
    reservationApi.getParameters()
      .then((data) => {
        setParameters(data);
        setDrafts(Object.fromEntries(data.map((p) => [p.code, p.value])));
      })
      .catch((err) => setError(err.message));
  }, []);

  /** Mismo rango que el backend, solo para avisar antes de guardar: el backend vuelve a validar (422). */
  function localProblem(parameter, raw) {
    if (!/^-?\d+$/.test(raw.trim())) return 'Escribe un número entero.';
    const value = Number(raw);
    if (value < parameter.min || value > parameter.max) return `Debe estar entre ${parameter.min} y ${parameter.max}.`;
    return null;
  }

  async function save(parameter) {
    const raw = drafts[parameter.code];
    const problem = localProblem(parameter, raw);
    if (problem) {
      setErrors((current) => ({ ...current, [parameter.code]: problem }));
      return;
    }
    setNotice(null);
    setError(null);
    setSavingCode(parameter.code);
    try {
      const saved = await reservationApi.updateParameter(parameter.code, raw.trim());
      setParameters((current) => current.map((p) => (p.code === saved.code ? saved : p)));
      setDrafts((current) => ({ ...current, [saved.code]: saved.value }));
      setNotice(`Se guardó "${LABELS[saved.code]?.title ?? saved.code}": ${saved.value} ${saved.unit}. Aplica a las reservas nuevas.`);
    } catch (err) {
      setErrors((current) => ({ ...current, [parameter.code]: err.message }));
    } finally {
      setSavingCode(null);
    }
  }

  return (
    <section>
      <div className="page-header">
        <h1>Configuración</h1>
        <p className="muted">Reglas del negocio que se ajustan sin cambiar el código. Los cambios aplican a las reservas nuevas.</p>
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}
      {parameters === null && !error && <p className="muted">Cargando configuración…</p>}

      <div className="settings-list">
        {parameters?.map((parameter) => {
          const label = LABELS[parameter.code] ?? { title: parameter.code, help: parameter.description };
          const changed = drafts[parameter.code] !== parameter.value;
          const id = `param-${parameter.code}`;
          return (
            <div key={parameter.code} className="card settings-row">
              <div className="settings-text">
                <label htmlFor={id}><strong>{label.title}</strong></label>
                <p className="muted small">{label.help}</p>
                <code className="small muted">{parameter.code}</code>
              </div>
              <div className="settings-control">
                {parameter.editable ? (
                  <>
                    <div className="settings-input">
                      <input id={id} type="number" min={parameter.min} max={parameter.max} inputMode="numeric"
                             value={drafts[parameter.code] ?? ''}
                             aria-invalid={errors[parameter.code] ? 'true' : undefined}
                             aria-describedby={`${id}-hint`}
                             onChange={(e) => {
                               setDrafts((current) => ({ ...current, [parameter.code]: e.target.value }));
                               setErrors((current) => ({ ...current, [parameter.code]: undefined }));
                             }} />
                      <span className="muted small">{parameter.unit}</span>
                    </div>
                    <small id={`${id}-hint`} className={errors[parameter.code] ? 'field-error' : 'muted'}>
                      {errors[parameter.code] ?? `Entre ${parameter.min} y ${parameter.max}`}
                    </small>
                    <button type="button" className="btn btn-small btn-solid" disabled={!changed || savingCode === parameter.code}
                            onClick={() => save(parameter)}>
                      {savingCode === parameter.code ? 'Guardando…' : 'Guardar'}
                    </button>
                  </>
                ) : (
                  <span className="settings-readonly">{parameter.value}</span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
