package ec.edu.uce.tickets.auth.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message){
        super(message);
    }
}
