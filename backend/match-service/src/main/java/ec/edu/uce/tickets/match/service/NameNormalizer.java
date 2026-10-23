package ec.edu.uce.tickets.match.service;

/**
 * Limpia un nombre antes de guardarlo: sin espacios al inicio ni al final, y uno solo entre palabras.
 * Las tildes y mayúsculas se CONSERVAN ("Estadio Olímpico" se guarda así); la comparación
 * sin tildes la hace la función name_key de la base, la misma que usa el índice único.
 */
final class NameNormalizer {

    private NameNormalizer() {
    }

    static String clean(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }
}
