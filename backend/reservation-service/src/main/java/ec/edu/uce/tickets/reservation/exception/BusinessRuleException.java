package ec.edu.uce.tickets.reservation.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message){
        super(message);
    }
}
