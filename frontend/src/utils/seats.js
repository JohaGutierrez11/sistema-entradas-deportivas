/** Recorre el layout UNA vez y arma un índice seatId → { sección, fila, número } (como un índice de BD). */
export function buildSeatIndex(layout) {
  const index = new Map();
  layout?.sections.forEach((section) =>
    section.rows.forEach((row) =>
      row.seats.forEach((seat) =>
        index.set(seat.id, { sectionId: section.id, sectionName: section.name, row: row.letter, number: seat.number }),
      ),
    ),
  );
  return index;
}

/** "Palco Occidental · Fila A · Asiento 3" */
export function seatLabel(info) {
  return info ? `${info.sectionName} · Fila ${info.row} · Asiento ${info.number}` : 'Asiento';
}

/** Asientos libres de una sección según la disponibilidad actual. */
export function freeSeats(section, occupied) {
  return section.rows.reduce(
    (total, row) => total + row.seats.filter((seat) => !occupied.has(seat.id)).length, 0);
}