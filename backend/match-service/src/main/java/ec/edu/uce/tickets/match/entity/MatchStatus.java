package ec.edu.uce.tickets.match.entity;

public enum MatchStatus {
    PROGRAMADO, EN_CURSO, FINALIZADO, CANCELADO;

    /** FINALIZADO y CANCELADO ya no admiten cambios. */
    public boolean isFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }

    /** Caminos permitidos de la máquina de estados. */
    public boolean canTransitionTo(MatchStatus next) {
        if (this == next) {
            return true;   // mismo estado: solo se editan otros datos (nombre, fecha...)
        }
        return switch (this) {
            case PROGRAMADO -> next == EN_CURSO || next == CANCELADO;
            case EN_CURSO -> next == FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
    }
}