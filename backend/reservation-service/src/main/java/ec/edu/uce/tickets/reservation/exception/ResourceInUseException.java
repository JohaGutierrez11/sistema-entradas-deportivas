package ec.edu.uce.tickets.reservation.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message){
        super(message);
    }
}
