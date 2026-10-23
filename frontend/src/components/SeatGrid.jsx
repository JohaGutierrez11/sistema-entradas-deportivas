const STATE_LABEL = { free: 'libre', held: 'retenido', sold: 'vendido', selected: 'tu selección' };

/** Grilla de asientos de una sección: filas (A, B…) × asientos (1..N), con su estado en vivo. */
export default function SeatGrid({ section, occupied, selectedSeats, onToggleSeat, readOnly }) {
  return (
    <div className="seat-panel">
      <h2 className="seat-title">{section.name}</h2>

      <div className="seat-scroll">
        {section.rows.map((row) => (
          <div className="seat-row" key={row.id}>
            <span className="row-label">{row.letter}</span>
            {row.seats.map((seat) => {
              const selected = selectedSeats.includes(seat.id);
              const status = occupied.get(seat.id);
              const state = selected ? 'selected' : status === 'RESERVED' ? 'sold' : status === 'HELD' ? 'held' : 'free';
              return (
                <button key={seat.id} type="button" className={`seat seat-${state}`}
                        disabled={readOnly || state === 'sold' || state === 'held'}
                        aria-pressed={selected}
                        aria-label={`Fila ${row.letter}, asiento ${seat.number}, ${STATE_LABEL[state]}`}
                        title={`Fila ${row.letter} · Asiento ${seat.number}`}
                        onClick={() => onToggleSeat(seat.id)}>
                  {seat.number}
                </button>
              );
            })}
          </div>
        ))}
      </div>

      <ul className="seat-legend" aria-label="Leyenda">
        <li><span className="seat seat-free" aria-hidden="true" /> Libre</li>
        <li><span className="seat seat-held" aria-hidden="true" /> Retenido</li>
        <li><span className="seat seat-sold" aria-hidden="true" /> Vendido</li>
        <li><span className="seat seat-selected" aria-hidden="true" /> Tu selección</li>
      </ul>
    </div>
  );
}