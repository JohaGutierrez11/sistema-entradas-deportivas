import { freeSeats } from '../utils/seats';

const WIDTH = 300;   // el plano se dibuja en un lienzo 3:2; el backend guarda % (0-100)
const HEIGHT = 200;
const x = (percent) => (percent * WIDTH) / 100;
const y = (percent) => (percent * HEIGHT) / 100;

/** Plano del estadio: la cancha y cada sección en su recuadro. Tocar una sección la selecciona. */
export default function StadiumMap({ layout, occupied, selectedSectionId, onSelectSection }) {
  const placed = layout.sections.filter((section) => section.box);
  const unplaced = layout.sections.filter((section) => !section.box);
  const field = layout.field;

  return (
    <div className="stadium">
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="stadium-map" role="group" aria-label="Plano del estadio">
        <rect x="0" y="0" width={WIDTH} height={HEIGHT} rx="10" className="map-bg" />

        <g className="map-field">
          <rect x={x(field.x)} y={y(field.y)} width={x(field.width)} height={y(field.height)} rx="3" />
          <line x1={x(field.x + field.width / 2)} y1={y(field.y)}
                x2={x(field.x + field.width / 2)} y2={y(field.y + field.height)} />
          <circle cx={x(field.x + field.width / 2)} cy={y(field.y + field.height / 2)} r="12" />
          <text x={x(field.x + field.width / 2)} y={y(field.y + field.height) - 6} textAnchor="middle">Cancha</text>
        </g>

        {placed.map((section) => {
          const free = freeSeats(section, occupied);
          const { box } = section;
          const centerX = x(box.x + box.width / 2);
          const centerY = y(box.y + box.height / 2);
          const classes = ['map-section', `type-${section.sectionType.toLowerCase()}`,
            section.id === selectedSectionId ? 'is-selected' : '', free === 0 ? 'is-full' : ''].join(' ');

          return (
            <g key={section.id} className={classes} role="button" tabIndex={0}
               aria-label={`${section.name}: ${free} asientos libres`} aria-pressed={section.id === selectedSectionId}
               onClick={() => onSelectSection(section.id)}
               onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelectSection(section.id); } }}>
              <rect x={x(box.x)} y={y(box.y)} width={x(box.width)} height={y(box.height)} rx="4" />
              <text x={centerX} y={centerY - 3} textAnchor="middle" className="map-label">{section.name}</text>
              <text x={centerX} y={centerY + 8} textAnchor="middle" className="map-sub">
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