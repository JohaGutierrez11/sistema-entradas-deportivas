package ec.edu.uce.tickets.auth.exception;

public class DuplicateResourceException extends RuntimeException{

    public DuplicateResourceException(String message){
        super(message);
    }

}
