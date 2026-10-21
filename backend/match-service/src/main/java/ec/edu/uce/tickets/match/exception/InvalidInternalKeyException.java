package ec.edu.uce.tickets.match.exception;

public class InvalidInternalKeyException extends RuntimeException {
    public InvalidInternalKeyException() {
        super("Falta el encabezado X-Internal-Key o no es válido");
    }
}