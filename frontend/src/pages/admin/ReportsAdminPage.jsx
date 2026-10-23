import { useEffect, useState } from 'react';
import { matchApi } from '../../api/matchApi';
import { reservationApi } from '../../api/reservationApi';
import StatusBadge from '../../components/StatusBadge';
import { downloadBlob } from '../../utils/download';
import { formatDateTime, formatShortDate } from '../../utils/format';
import { MATCH_STATUSES } from '../../utils/matchStatus';

const RESERVATION_STATUSES = ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'EXPIRADA'];
const EMPTY_FILTERS = { from: '', to: '', matchId: '', status: '' };
const numberFormat = new Intl.NumberFormat('es-EC');
const percentFormat = new Intl.NumberFormat('es-EC', { maximumFractionDigits: 1 });

/** Reporte general (HU-21 / HU-28): resumen de reservas y ocupación por partido y sección, en pantalla y PDF. */
export default function ReportsAdminPage() {
  const [form, setForm] = useState(EMPTY_FILTERS);
  const [filters, setFilters] = useState(EMPTY_FILTERS);   // los aplicados con "Generar"
  const [matches, setMatches] = useState([]);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  // Para el selector: partidos de TODOS los estados (la búsqueda del backend es de un estado a la vez)
  useEffect(() => {
    Promise.all(MATCH_STATUSES.map((s) => matchApi.searchMatchesByStatus({ status: s.value })))
      .then((lists) => setMatches(lists.flat().sort((a, b) => a.matchDate.localeCompare(b.matchDate))))
      .catch(() => setMatches([]));   // sin la lista, igual se puede generar el reporte sin filtro de partido
  }, []);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    reservationApi.getAdminReport(filters)
      .then((data) => { if (current) setReport(data); })
      .catch((err) => { if (current) setError(err.message); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [filters]);

  const rangeInvalid = form.from && form.to && form.from > form.to;

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (!rangeInvalid) setFilters(form);
  }

  async function downloadPdf() {
    setError(null);
    setDownloading(true);
    try {
      downloadBlob(await reservationApi.getAdminReportPdf(filters), 'reporte-reservas.pdf');
    } catch (err) {
      setError(err.message);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <section>
      <div className="page-header page-header-row">
        <div>
          <h1>Reportes</h1>
          <p className="muted">Reservas filtradas por fecha de compra, partido y estado. La ocupación es la del momento.</p>
        </div>
        <button type="button" className="btn" onClick={downloadPdf} disabled={downloading || !report}>
          {downloading ? 'Generando…' : 'Descargar PDF'}
        </button>
      </div>

      <form className="card filters" onSubmit={handleSubmit}>
        <div className="filter-field">
          <label htmlFor="from">Desde</label>
          <input id="from" name="from" type="date" value={form.from} onChange={handleChange} />
        </div>
        <div className="filter-field">
          <label htmlFor="to">Hasta</label>
          <input id="to" name="to" type="date" value={form.to} onChange={handleChange} min={form.from || undefined}
                 aria-invalid={rangeInvalid ? 'true' : undefined} />
          {rangeInvalid && <small className="field-error">Debe ser igual o posterior a "Desde"</small>}
        </div>
        <div className="filter-field filter-grow">
          <label htmlFor="matchId">Partido</label>
          <select id="matchId" name="matchId" value={form.matchId} onChange={handleChange}>
            <option value="">Todos los partidos</option>
            {matches.map((m) => (
              <option key={m.id} value={m.id}>{formatShortDate(m.matchDate)} · {m.name}</option>
            ))}
          </select>
        </div>
        <div className="filter-field">
          <label htmlFor="status">Estado</label>
          <select id="status" name="status" value={form.status} onChange={handleChange}>
            <option value="">Todos</option>
            {RESERVATION_STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
        <div className="filter-actions">
          <button type="submit" className="btn btn-solid" disabled={rangeInvalid}>Generar</button>
          <button type="button" className="btn" onClick={() => { setForm(EMPTY_FILTERS); setFilters(EMPTY_FILTERS); }}>
            Limpiar
          </button>
        </div>
      </form>

      {error && <p className="alert alert-error" role="alert">{error}</p>}
      {loading && <p className="muted">Generando el reporte…</p>}

      {report && !loading && (
        <>
          <p className="muted small">Generado el {formatDateTime(report.generatedAt)}</p>
          <Summary summary={report.summary} />

          <h2 className="section-title">Ocupación por partido</h2>
          {report.matches.length === 0 && (
            <div className="card empty-state"><p className="muted">Ninguna reserva coincide con los filtros.</p></div>
          )}
          {report.matches.map((item) => <MatchOccupancy key={item.matchId} item={item} />)}
        </>
      )}
    </section>
  );
}

function Summary({ summary }) {
  return (
    <div className="kpi-grid">
      <div className="card kpi">
        <span className="kpi-label">Reservas</span>
        <span className="kpi-value">{numberFormat.format(summary.totalReservations)}</span>
        <ul className="kpi-breakdown">
          {RESERVATION_STATUSES.map((s) => (
            <li key={s}><StatusBadge status={s} /> {numberFormat.format(summary.byStatus[s] ?? 0)}</li>
          ))}
        </ul>
      </div>
      <div className="card kpi">
        <span className="kpi-label">Entradas vendidas</span>
        <span className="kpi-value">{numberFormat.format(summary.ticketsSold)}</span>
        <span className="muted small">Activas y utilizadas</span>
      </div>
      <div className="card kpi">
        <span className="kpi-label">Entradas anuladas</span>
        <span className="kpi-value">{numberFormat.format(summary.ticketsAnnulled)}</span>
        <span className="muted small">Por reservas canceladas</span>
      </div>
    </div>
  );
}

/** Barra de ocupación: el ancho mínimo visible evita que 0,1 % parezca 0. */
function Bar({ percentage }) {
  const width = percentage > 0 ? Math.max(percentage, 1) : 0;
  return (
    <div className="capacity-bar" role="img" aria-label={`${percentFormat.format(percentage)} % ocupado`}>
      <span style={{ width: `${Math.min(width, 100)}%` }} />
    </div>
  );
}

function MatchOccupancy({ item }) {
  const { match, occupancy } = item;
  return (
    <article className="card report-match">
      <div className="report-match-header">
        <div>
          <h3>{match ? match.name : `Partido #${item.matchId}`}</h3>
          <p className="muted small">
            {match ? `${formatShortDate(match.matchDate)} · ${match.matchTime} · ${match.stadiumName}`
              : 'Información del partido no disponible'}
            {' · '}{item.reservations} {item.reservations === 1 ? 'reserva' : 'reservas'} en el filtro
          </p>
        </div>
        {occupancy && (
          <div className="report-total">
            <strong>{percentFormat.format(occupancy.percentage)} %</strong>
            <span className="muted small">{numberFormat.format(occupancy.occupied)} / {numberFormat.format(occupancy.capacity)}</span>
          </div>
        )}
      </div>

      {occupancy ? (
        <>
          <Bar percentage={occupancy.percentage} />
          <table className="data-table report-sections">
            <thead>
              <tr>
                <th scope="col">Sección</th>
                <th scope="col" className="num">Vendidos</th>
                <th scope="col" className="bar-col">Ocupación</th>
                <th scope="col" className="num">%</th>
              </tr>
            </thead>
            <tbody>
              {occupancy.sections.map((s) => (
                <tr key={s.name}>
                  <td>{s.name}</td>
                  <td className="num">{numberFormat.format(s.occupied)} / {numberFormat.format(s.capacity)}</td>
                  <td className="bar-col"><Bar percentage={s.percentage} /></td>
                  <td className="num">{percentFormat.format(s.percentage)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      ) : (
        <p className="alert alert-warning">match-service no respondió: la ocupación no está disponible ahora.</p>
      )}
    </article>
  );
}
