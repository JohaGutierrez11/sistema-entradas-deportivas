/**
 * Divide una sección grande en bloques para que la grilla quepa en pantalla sin scroll horizontal,
 * como los "sectores" de una ticketera real. Una sección chica queda en un solo bloque.
 */

/** Máximo de asientos que se ven a lo ancho y a lo alto en un bloque. */
const MAX_ACROSS = 12;
const MAX_DOWN = 13;

/** ¿De qué lado de la cancha está la sección? Decide cómo se orienta la grilla. */
export function sideOf(box, field) {
  if (!box || !field) return 'bottom';
  if (box.x + box.width <= field.x) return 'left';
  if (box.x >= field.x + field.width) return 'right';
  if (box.y + box.height <= field.y) return 'top';
  return 'bottom';
}

/** Parte 0..total-1 en tramos parejos de máximo `max`: 25 con máximo 12 → [0-8], [9-16], [17-24]. */
function chunks(total, max) {
  const count = Math.ceil(total / max);
  const base = Math.floor(total / count);
  const extra = total % count;
  const result = [];
  let start = 0;
  for (let i = 0; i < count; i++) {
    const size = base + (i < extra ? 1 : 0);
    result.push({ start, end: start + size - 1 });
    start += size;
  }
  return result;
}

/**
 * Bloques de la sección. En los lados norte y sur los asientos van a lo ancho y las filas hacia abajo;
 * en oriente y occidente es al revés. Cada bloque es un rango de filas × un rango de asientos.
 */
export function buildBlocks(section, side) {
  const vertical = side === 'left' || side === 'right';
  const rowCount = section.rows.length;
  const seatsPerRow = Math.max(...section.rows.map((row) => row.seats.length));
  const rowChunks = chunks(rowCount, vertical ? MAX_ACROSS : MAX_DOWN);
  const seatChunks = chunks(seatsPerRow, vertical ? MAX_DOWN : MAX_ACROSS);

  const blocks = [];
  rowChunks.forEach((rows, depthIndex) => {
    seatChunks.forEach((seats, alongIndex) => {
      blocks.push({
        id: `${depthIndex}-${alongIndex}`,
        depthIndex, alongIndex,
        rows, seats,
        label: `Filas ${section.rows[rows.start].letter}–${section.rows[rows.end].letter} · Asientos ${seats.start + 1}–${seats.end + 1}`,
      });
    });
  });
  return { blocks, depthCount: rowChunks.length, alongCount: seatChunks.length };
}

/** Los asientos de un bloque, fila por fila. */
export function seatsOfBlock(section, block) {
  return section.rows.slice(block.rows.start, block.rows.end + 1)
    .map((row) => ({ ...row, seats: row.seats.slice(block.seats.start, block.seats.end + 1) }));
}

/** Bloque inicial: el más cercano a la cancha y al centro (la mejor vista), como haría un vendedor. */
export function defaultBlock({ blocks, alongCount }) {
  const center = Math.floor((alongCount - 1) / 2);
  return blocks.find((block) => block.depthIndex === 0 && block.alongIndex === center) ?? blocks[0];
}
