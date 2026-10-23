const STATE_LABEL = { free: 'libre', held: 'retenido', sold: 'vendido', selected: 'tu selección' };

/** ¿De qué lado de la cancha está la sección? Decide cómo se orienta la grilla. */
function sideOf(box, field) {
  if (!box || !field) return 'bottom';
  if (box.x + box.width <= field.x) return 'left';
  if (box.x >= field.x + field.width) return 'right';
  if (box.y + box.height <= field.y) return 'top';
  return 'bottom';
}

/**
 * Grilla de asientos orientada como en el plano. La fila A siempre queda pegada a la cancha:
 *   arriba de la cancha → la A abajo · abajo → la A arriba · izquierda → la A a la derecha · derecha → la A a la izquierda.
 */
export default function SeatGrid({ section, field, occupied, selectedSeats, onToggleSeat, readOnly }) {
  const side = sideOf(section.box, field);
  const vertical = side === 'left' || side === 'right';
  const rowCount = section.rows.length;
  const seatsPerRow = Math.max(...section.rows.map((row) => row.seats.length));

  // Posición de cada fila contando desde el borde de la grilla (la A queda del lado de la cancha)
  const depthOf = (rowIndex) => (side === 'top' || side === 'left' ? rowCount - rowIndex : rowIndex + 1);

  // Las filas van en el eje "de profundidad"; los asientos, a lo largo. Una celda extra para la letra.
  const gridStyle = vertical
    ? { gridTemplateColumns: `repeat(${rowCount}, 2rem)`, gridTemplateRows: `1.5rem repeat(${seatsPerRow}, 2rem)` }
    : { gridTemplateColumns: `1.5rem repeat(${seatsPerRow}, 2rem)`, gridTemplateRows: `repeat(${rowCount}, 2rem)` };

  return (
    <div className="seat-panel">
      <h2 className="seat-title">{section.name}</h2>

      <div className={`seat-layout side-${side}`}>
        <div className="seat-scroll">
          <div className="seat-grid" style={gridStyle}>
            {section.rows.map((row, rowIndex) => {
              const depth = depthOf(rowIndex);
              const labelPosition = vertical ? { gridColumn: depth, gridRow: 1 } : { gridColumn: 1, gridRow: depth };
              return [
                <span key={`label-${row.id}`} className="row-label" style={labelPosition}>{row.letter}</span>,
                ...row.seats.map((seat, seatIndex) => {
                  const selected = selectedSeats.includes(seat.id);
                  const status = occupied.get(seat.id);
                  const state = selected ? 'selected' : status === 'RESERVED' ? 'sold' : status === 'HELD' ? 'held' : 'free';
                  const position = vertical
                    ? { gridColumn: depth, gridRow: seatIndex + 2 }
                    : { gridColumn: seatIndex + 2, gridRow: depth };
                  return (
                    <button key={seat.id} type="button" className={`seat seat-${state}`} style={position}
                            disabled={readOnly || state === 'sold' || state === 'held'}
                            aria-pressed={selected}
                            aria-label={`Fila ${row.letter}, asiento ${seat.number}, ${STATE_LABEL[state]}`}
                            title={`Fila ${row.letter} · Asiento ${seat.number}`}
                            onClick={() => onToggleSeat(seat.id)}>
                      {seat.number}
                    </button>
                  );
                }),
              ];
            })}
          </div>
        </div>
        <div className="field-strip" aria-hidden="true">Cancha</div>
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
