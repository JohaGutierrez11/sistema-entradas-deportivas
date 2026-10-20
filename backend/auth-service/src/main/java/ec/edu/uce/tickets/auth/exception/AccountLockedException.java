package ec.edu.uce.tickets.auth.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(LocalDateTime lockedUntil) {
        super("La cuenta está bloqueada por intentos fallidos hasta las "
                + lockedUntil.format(DateTimeFormatter.ofPattern("HH:mm")));
    }
}