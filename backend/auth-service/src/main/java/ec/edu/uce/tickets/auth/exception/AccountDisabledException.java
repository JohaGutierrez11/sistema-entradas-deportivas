package ec.edu.uce.tickets.auth.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("La cuenta está inactiva. Comuníquese con el administrador");
    }
}