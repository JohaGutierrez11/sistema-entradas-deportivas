import { useMemo, useState } from 'react';
import { buildBlocks, defaultBlock, seatsOfBlock, sideOf } from '../utils/seatBlocks';

const STATE_LABEL = { free: 'libre', held: 'retenido', sold: 'vendido', selected: 'tu selección' };

/**
 * Posición en la grilla contando desde el borde: la fila A (índice 0) queda del lado de la cancha.
 *   arriba de la cancha → la A abajo · abajo → la A arriba · izquierda → la A a la derecha · derecha → la A a la izquierda.
 */
const depthPosition = (side, index, count) => (side === 'top' || side === 'left' ? count - index : index + 1);

/**
 * Grilla de asientos orientada como en el plano. Si la sección es grande, primero se elige un bloque
 * en un mini-mapa (también orientado) y luego se ven solo sus asientos.
 */
export default function SeatGrid({ section, field, occupied, selectedSeats, onToggleSeat, readOnly }) {
  const side = sideOf(section.box, field);
  const vertical = side === 'left' || side === 'right';
  const plan = useMemo(() => buildBlocks(section, side), [section, side]);
  // El bloque elegido se recuerda por sección: al cambiar de sección se vuelve al bloque inicial
  const [choice, setChoice] = useState({ sectionId: null, blockId: null });
  const chosen = choice.sectionId === section.id ? plan.blocks.find((b) => b.id === choice.blockId) : null;
  const block = chosen ?? defaultBlock(plan);
  const setBlockId = (blockId) => setChoice({ sectionId: section.id, blockId });

  const stateOf = (seatId) => {
    if (selectedSeats.includes(seatId)) return 'selected';
    const status = occupied.get(seatId);
    return status === 'RESERVED' ? 'sold' : status === 'HELD' ? 'held' : 'free';
  };

  return (
    <div className="seat-panel">
      <h2 className="seat-title">{section.name}</h2>

      {plan.blocks.length > 1 && (
        <BlockPicker section={section} side={side} plan={plan} selectedId={block.id} stateOf={stateOf}
                     onSelect={setBlockId} />
      )}

      <BlockGrid rows={seatsOfBlock(section, block)} side={side} vertical={vertical}
                 stateOf={stateOf} readOnly={readOnly} onToggleSeat={onToggleSeat} />

      <ul className="seat-legend" aria-label="Leyenda">
        <li><span className="seat seat-free" aria-hidden="true" /> Libre</li>
        <li><span className="seat seat-held" aria-hidden="true" /> Retenido</li>
        <li><span className="seat seat-sold" aria-hidden="true" /> Vendido</li>
        <li><span className="seat seat-selected" aria-hidden="true" /> Tu selección</li>
      </ul>
    </div>
  );
}

/** Mini-mapa de bloques con los libres de cada uno. Se orienta igual que la grilla. */
function BlockPicker({ section, side, plan, selectedId, stateOf, onSelect }) {
  const vertical = side === 'left' || side === 'right';
  return (
    <div className="block-picker">
      <p className="muted small">Esta sección es grande: elige un bloque y luego tus asientos.</p>
      <div className={`seat-layout side-${side}`}>
        <div className="block-grid"
             style={vertical
               ? { gridTemplateColumns: `repeat(${plan.depthCount}, 1fr)` }
               : { gridTemplateColumns: `repeat(${plan.alongCount}, 1fr)` }}>
          {plan.blocks.map((b) => {
            const seats = seatsOfBlock(section, b).flatMap((row) => row.seats);
            const free = seats.filter((seat) => stateOf(seat.id) === 'free').length;
            const mine = seats.filter((seat) => stateOf(seat.id) === 'selected').length;
            const depth = depthPosition(side, b.depthIndex, plan.depthCount);
            const position = vertical
              ? { gridColumn: depth, gridRow: b.alongIndex + 1 }
              : { gridColumn: b.alongIndex + 1, gridRow: depth };
            return (
              <button key={b.id} type="button" style={position} title={b.label}
                      className={`block ${b.id === selectedId ? 'is-selected' : ''} ${free === 0 ? 'is-full' : ''}`}
                      aria-pressed={b.id === selectedId}
                      aria-label={`${b.label}: ${free} libres${mine ? `, ${mine} elegidos por ti` : ''}`}
                      onClick={() => onSelect(b.id)}>
                <span className="block-free">{free}</span>
                {mine > 0 && <span className="block-mine" aria-hidden="true">{mine}</span>}
              </button>
            );
          })}
        </div>
        <div className="field-strip" aria-hidden="true">Cancha</div>
      </div>
    </div>
  );
}

/** Los asientos del bloque elegido, con la fila A del lado de la cancha. */
function BlockGrid({ rows, side, vertical, stateOf, readOnly, onToggleSeat }) {
  const rowCount = rows.length;
  const seatsPerRow = Math.max(...rows.map((row) => row.seats.length));
  const cell = 'var(--seat-size)';
  const gridStyle = vertical
    ? { gridTemplateColumns: `repeat(${rowCount}, ${cell})`, gridTemplateRows: `1.25rem repeat(${seatsPerRow}, ${cell})` }
    : { gridTemplateColumns: `1.25rem repeat(${seatsPerRow}, ${cell})`, gridTemplateRows: `repeat(${rowCount}, ${cell})` };

  return (
    <div className={`seat-layout side-${side}`}>
      <div className="seat-scroll">
        <div className="seat-grid" style={gridStyle}>
          {rows.map((row, rowIndex) => {
            const depth = depthPosition(side, rowIndex, rowCount);
            const labelPosition = vertical ? { gridColumn: depth, gridRow: 1 } : { gridColumn: 1, gridRow: depth };
            return [
              <span key={`label-${row.id}`} className="row-label" style={labelPosition}>{row.letter}</span>,
              ...row.seats.map((seat, seatIndex) => {
                const state = stateOf(seat.id);
                const position = vertical
                  ? { gridColumn: depth, gridRow: seatIndex + 2 }
                  : { gridColumn: seatIndex + 2, gridRow: depth };
                return (
                  <button key={seat.id} type="button" className={`seat seat-${state}`} style={position}
                          disabled={readOnly || state === 'sold' || state === 'held'}
                          aria-pressed={state === 'selected'}
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
  );
}
