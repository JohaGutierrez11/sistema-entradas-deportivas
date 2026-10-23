import { freeSeats } from '../utils/seats';

// Lienzo cuadrado: con la cancha fija del backend (44 % × 50 %) queda vertical, con los arcos
// arriba y abajo. Así "Norte" y "Sur" quedan detrás de los arcos, como en un estadio real.
const SIZE = 300;
const px = (percent) => (percent * SIZE) / 100;

/** Reparte el nombre en 1 o 2 líneas y elige un tamaño de letra que quepa en el recuadro. */
function fitLabel(name, maxWidth, maxSize = 9, minSize = 5) {
  const widthOf = (text, size) => text.length * size * 0.56;   // ancho aproximado de cada letra
  if (widthOf(name, maxSize) <= maxWidth) return { lines: [name], size: maxSize };

  const words = name.split(' ');
  let lines = [name];
  if (words.length > 1) {
    // El corte más equilibrado: que la línea más larga sea lo más corta posible
    let best = null;
    for (let i = 1; i < words.length; i++) {
      const candidate = [words.slice(0, i).join(' '), words.slice(i).join(' ')];
      const longest = Math.max(...candidate.map((line) => line.length));
      if (!best || longest < best.longest) best = { lines: candidate, longest };
    }
    lines = best.lines;
  }
  const longest = Math.max(...lines.map((line) => line.length));
  const size = Math.max(minSize, Math.min(maxSize, maxWidth / (longest * 0.56)));
  return { lines, size };
}

/** Líneas de la cancha (vertical): áreas, arcos, mitad, círculo y puntos penales. */
function Pitch({ field }) {
  const fx = px(field.x), fy = px(field.y), fw = px(field.width), fh = px(field.height);
  const cx = fx + fw / 2;
  const box = { w: fw * 0.6, h: fh * 0.17 };     // área grande
  const small = { w: fw * 0.3, h: fh * 0.07 };   // área chica
  const goal = fw * 0.14;

  return (
    <g className="map-pitch">
      <rect x={fx} y={fy} width={fw} height={fh} fill="url(#grass)" />
      <g className="pitch-lines">
        <rect x={fx + 3} y={fy + 3} width={fw - 6} height={fh - 6} />
        <line x1={fx + 3} y1={fy + fh / 2} x2={fx + fw - 3} y2={fy + fh / 2} />
        <circle cx={cx} cy={fy + fh / 2} r={fw * 0.14} />
        <rect x={cx - box.w / 2} y={fy + 3} width={box.w} height={box.h} />
        <rect x={cx - small.w / 2} y={fy + 3} width={small.w} height={small.h} />
        <rect x={cx - box.w / 2} y={fy + fh - 3 - box.h} width={box.w} height={box.h} />
        <rect x={cx - small.w / 2} y={fy + fh - 3 - small.h} width={small.w} height={small.h} />
      </g>
      <circle cx={cx} cy={fy + fh / 2} r="1.2" className="pitch-spot" />
      <circle cx={cx} cy={fy + 3 + box.h * 0.7} r="1" className="pitch-spot" />
      <circle cx={cx} cy={fy + fh - 3 - box.h * 0.7} r="1" className="pitch-spot" />
      <rect x={cx - goal / 2} y={fy - 1} width={goal} height="3" className="pitch-goal" />
      <rect x={cx - goal / 2} y={fy + fh - 2} width={goal} height="3" className="pitch-goal" />
    </g>
  );
}

/** Plano del estadio: la cancha y cada sección en su recuadro. Tocar una sección la selecciona. */
export default function StadiumMap({ layout, occupied, selectedSectionId, onSelectSection }) {
  const placed = layout.sections.filter((section) => section.box);
  const unplaced = layout.sections.filter((section) => !section.box);
  const { field } = layout;

  return (
    <div className="stadium">
      <svg viewBox={`0 0 ${SIZE} ${SIZE}`} className="stadium-map" role="group" aria-label="Plano del estadio">
        <defs>
          <pattern id="grass" width="24" height="24" patternUnits="userSpaceOnUse">
            <rect width="24" height="12" fill="#3f8f4f" />
            <rect y="12" width="24" height="12" fill="#46985a" />
          </pattern>
        </defs>

        <rect x="2" y="2" width={SIZE - 4} height={SIZE - 4} rx="48" className="map-bowl" />
        <rect x={px(field.x) - 7} y={px(field.y) - 7} width={px(field.width) + 14} height={px(field.height) + 14}
              rx="14" className="map-track" />
        <Pitch field={field} />

        {placed.map((section) => {
          const free = freeSeats(section, occupied);
          const { box } = section;
          const width = px(box.width);
          const centerX = px(box.x + box.width / 2);
          const centerY = px(box.y + box.height / 2);
          const label = fitLabel(section.name, width - 8);
          const lineHeight = label.size * 1.15;
          const firstLineY = centerY - ((label.lines.length - 1) * lineHeight) / 2 - 4;
          const classes = ['map-section', `type-${section.sectionType.toLowerCase()}`,
            section.id === selectedSectionId ? 'is-selected' : '', free === 0 ? 'is-full' : ''].join(' ');

          return (
            <g key={section.id} className={classes} role="button" tabIndex={0}
               aria-label={`${section.name}: ${free} asientos libres`} aria-pressed={section.id === selectedSectionId}
               onClick={() => onSelectSection(section.id)}
               onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelectSection(section.id); } }}>
              <rect x={px(box.x)} y={px(box.y)} width={width} height={px(box.height)} rx="6" />
              <text textAnchor="middle" className="map-label" fontSize={label.size}>
                {label.lines.map((line, i) => (
                  <tspan key={line} x={centerX} y={firstLineY + i * lineHeight}>{line}</tspan>
                ))}
              </text>
              <text x={centerX} y={firstLineY + label.lines.length * lineHeight + 3} textAnchor="middle" className="map-sub">
                {free === 0 ? 'Agotado' : `${free} libres`}
              </text>
            </g>
          );
        })}
      </svg>

      {unplaced.length > 0 && (
        <div className="unplaced">
          <span className="muted">Otras secciones:</span>
          {unplaced.map((section) => (
            <button key={section.id} type="button" className="btn btn-small" onClick={() => onSelectSection(section.id)}>
              {section.name} · {freeSeats(section, occupied)} libres
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
