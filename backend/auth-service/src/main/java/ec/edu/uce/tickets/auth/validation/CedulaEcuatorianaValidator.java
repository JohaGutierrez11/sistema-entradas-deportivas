package ec.edu.uce.tickets.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CedulaEcuatorianaValidator implements ConstraintValidator<CedulaEcuatoriana, String> {

    private static final int[] COEFFICIENTS = {2, 1, 2, 1, 2, 1, 2, 1, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;   // el "obligatorio" lo valida @NotBlank, no esta regla
        }
        if (!value.matches("\\d{10}")) {
            return false;  // exactamente 10 dígitos
        }

        int province = Integer.parseInt(value.substring(0, 2));
        if (!((province >= 1 && province <= 24) || province == 30)) {
            return false;  // provincia inexistente
        }

        int thirdDigit = value.charAt(2) - '0';
        if (thirdDigit >= 6) {
            return false;  // 6 y 9 son de empresas (RUC), no de personas
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int product = (value.charAt(i) - '0') * COEFFICIENTS[i];
            sum += (product > 9) ? product - 9 : product;
        }
        int checkDigit = (10 - (sum % 10)) % 10;

        return checkDigit == value.charAt(9) - '0';
    }
}