package ec.edu.uce.tickets.auth.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message){
        super(message);
    }
}
