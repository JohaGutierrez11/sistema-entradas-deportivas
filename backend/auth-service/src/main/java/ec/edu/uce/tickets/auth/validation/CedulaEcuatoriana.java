package ec.edu.uce.tickets.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CedulaEcuatorianaValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedulaEcuatoriana {

    String message() default "no es una cédula ecuatoriana válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}