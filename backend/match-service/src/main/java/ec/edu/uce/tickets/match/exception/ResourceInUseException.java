package ec.edu.uce.tickets.match.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message){
        super(message);
    }
}
