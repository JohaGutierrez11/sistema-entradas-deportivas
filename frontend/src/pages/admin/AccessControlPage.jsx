import { useEffect, useRef, useState } from 'react';
import { adminAuthApi } from '../../api/adminAuthApi';
import { matchApi } from '../../api/matchApi';
import { reservationApi } from '../../api/reservationApi';
import { formatShortDate } from '../../utils/format';
import { buildSeatIndex, seatLabel } from '../../utils/seats';

/**
 * Control de acceso en la puerta (HU-47). Se elige el partido y se lee el QR: un lector USB de códigos
 * "escribe" el texto y presiona Enter, así que el campo siempre vuelve a tener el foco después de validar.
 */
export default function AccessControlPage() {
  const [matches, setMatches] = useState(null);
  const [matchId, setMatchId] = useState('');
  const [seatIndex, setSeatIndex] = useState(new Map());
  const [code, setCode] = useState('');
  const [checking, setChecking] = useState(false);
  const [last, setLast] = useState(null);          // última validación (tarjeta grande)
  const [history, setHistory] = useState([]);      // últimas validaciones de esta sesión
  const [error, setError] = useState(null);
  const inputRef = useRef(null);

  // Solo partidos donde se controla el ingreso: en curso y programados
  useEffect(() => {
    Promise.all([
      matchApi.searchMatchesByStatus({ status: 'EN_CURSO' }),
      matchApi.searchMatchesByStatus({ status: 'PROGRAMADO' }),
    ])
      .then(([live, upcoming]) => {
        const list = [...live, ...upcoming];
        setMatches(list);
        if (list.length > 0) setMatchId(String(list[0].id));
      })
      .catch((err) => setError(err.message));
  }, []);

  // Nombres de los asientos del escenario del partido elegido
  useEffect(() => {
    if (!matchId) return;
    setLast(null);
    matchApi.getMatch(matchId)
      .then((match) => matchApi.getStadiumLayout(match.stadium.id))
      .then((layout) => setSeatIndex(buildSeatIndex(layout)))
      .catch(() => setSeatIndex(new Map()));
    inputRef.current?.focus();
  }, [matchId]);

  async function validate(event) {
    event.preventDefault();
    if (!code.trim() || !matchId) return;
    setError(null);
    setChecking(true);
    try {
      const result = await reservationApi.validateTicket(code.trim(), Number(matchId));
      let client = null;
      if (result.clientId) {
        client = (await adminAuthApi.getClients([result.clientId]).catch(() => []))[0] ?? null;
      }
      const entry = { ...result, client, at: new Date() };
      setLast(entry);
      setHistory((current) => [entry, ...current].slice(0, 10));
    } catch (err) {
      setError(err.message);
    } finally {
      setChecking(false);
      setCode('');
      inputRef.current?.focus();
    }
  }

  const seatOf = (entry) => (entry.seatId && seatIndex.has(entry.seatId) ? seatLabel(seatIndex.get(entry.seatId)) : null);

  return (
    <section>
      <div className="page-header">
        <h1>Control de acceso</h1>
        <p className="muted">Lee el código QR de la entrada en la puerta del escenario. Cada entrada sirve una sola vez.</p>
      </div>

      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <form className="card access-form" onSubmit={validate}>
        <div className="field">
          <label htmlFor="matchId">Partido</label>
          <select id="matchId" value={matchId} onChange={(e) => setMatchId(e.target.value)} disabled={!matches?.length}>
            {matches?.length === 0 && <option value="">No hay partidos en curso ni programados</option>}
            {matches?.map((m) => (
              <option key={m.id} value={m.id}>
                {m.status === 'EN_CURSO' ? '🔴 En curso · ' : ''}{formatShortDate(m.matchDate)} {m.matchTime} · {m.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="qrCode">Código de la entrada</label>
          <div className="access-input">
            <input id="qrCode" ref={inputRef} autoFocus autoComplete="off" spellCheck="false" value={code}
                   placeholder="Escanea el QR o pega el código" onChange={(e) => setCode(e.target.value)} />
            <button type="submit" className="btn btn-solid" disabled={checking || !code.trim() || !matchId}>
              {checking ? 'Validando…' : 'Validar'}
            </button>
          </div>
          <small className="muted">Con un lector de QR, el código se escribe solo y se valida al presionar Enter.</small>
        </div>
      </form>

      {last && (
        <div className={`card access-result ${last.valid ? 'is-valid' : 'is-invalid'}`} role="status" aria-live="assertive">
          <p className="access-verdict">{last.valid ? '✓ PUEDE INGRESAR' : '✕ NO PUEDE INGRESAR'}</p>
          <p className="access-message">{last.message}</p>
          {(seatOf(last) || last.client || last.reservationCode) && (
            <dl className="access-details">
              {seatOf(last) && <><dt>Asiento</dt><dd>{seatOf(last)}</dd></>}
              {last.client && <><dt>Cliente</dt><dd>{last.client.fullName} · C.I. ****{last.client.cedulaLast4}</dd></>}
              {last.reservationCode && <><dt>Reserva</dt><dd>{last.reservationCode}</dd></>}
            </dl>
          )}
        </div>
      )}

      {history.length > 0 && (
        <>
          <h2 className="section-title">Últimas lecturas</h2>
          <div className="card table-card">
            <table className="data-table">
              <thead>
                <tr><th scope="col">Hora</th><th scope="col">Resultado</th><th scope="col">Asiento</th><th scope="col">Detalle</th></tr>
              </thead>
              <tbody>
                {history.map((entry, i) => (
                  <tr key={`${entry.at.getTime()}-${i}`}>
                    <td className="nowrap">{entry.at.toLocaleTimeString('es-EC')}</td>
                    <td><span className={`badge ${entry.valid ? 'badge-activo' : 'badge-cancelado'}`}>{entry.valid ? 'Válida' : 'Rechazada'}</span></td>
                    <td>{seatOf(entry) ?? '—'}</td>
                    <td className="small">{entry.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}
