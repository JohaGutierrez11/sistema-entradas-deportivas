/**
 * Reglas del plano del estadio, copiadas de match-service (SectionBox) para avisar ANTES de guardar.
 * El backend sigue siendo quien decide: esto solo evita viajes al servidor que ya sabemos que fallan.
 */

/** La cancha fija del backend (SectionBox.FIELD), en % del plano. */
export const FIELD_BOX = { x: 28, y: 25, width: 44, height: 50 };

export const SECTION_TYPES = [
  { value: 'PALCOS', label: 'Palcos' },
  { value: 'PREFERENCIA', label: 'Preferencia' },
  { value: 'TRIBUNAS', label: 'Tribunas' },
  { value: 'GENERAL', label: 'General' },
];

export const sectionTypeLabel = (value) => SECTION_TYPES.find((type) => type.value === value)?.label ?? value;

/** Igual que SectionBox.overlaps: tocarse en el borde NO es superponerse. */
export function boxesOverlap(a, b) {
  return a.x < b.x + b.width && b.x < a.x + a.width
      && a.y < b.y + b.height && b.y < a.y + a.height;
}

/**
 * Devuelve por qué el recuadro no se puede guardar, o null si está bien.
 * Mismo orden que el backend: límites del plano → cancha → otras secciones.
 */
export function findBoxProblem(box, sections, ignoreSectionId) {
  const values = [box.x, box.y, box.width, box.height];
  if (values.some((value) => !Number.isInteger(value))) return 'Completa los cuatro valores con números enteros.';
  if (box.x < 0 || box.y < 0 || box.width < 5 || box.height < 5) return 'Posición mínima 0 y tamaño mínimo 5.';
  if (box.x + box.width > 100 || box.y + box.height > 100) return 'El recuadro se sale del plano.';
  if (boxesOverlap(box, FIELD_BOX)) return 'El recuadro pisa la cancha.';
  const other = sections.find((section) => section.id !== ignoreSectionId && section.box && boxesOverlap(box, section.box));
  return other ? `El recuadro pisa la sección "${other.name}".` : null;
}

/** Punto de partida para una sección nueva: arriba a la izquierda, fuera de la cancha. */
export const DEFAULT_BOX = { x: 2, y: 2, width: 24, height: 20 };
