package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.validation.CedulaEcuatoriana;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[\\p{L} '-]+$", message = "solo admite letras, espacios, ' y -")
        String firstName,
        @Size(max = 50)
        @Pattern(regexp = "^[\\p{L} '-]*$", message = "solo admite letras, espacios, ' y -")
        String middleName,
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[\\p{L} '-]+$", message = "solo admite letras, espacios, ' y -")
        String lastName,
        @NotBlank @CedulaEcuatoriana
        String cedula,
        @NotBlank @Size(max = 50) String nationality,
        @NotBlank @Size(max = 200) String address,
        @NotBlank @Pattern(regexp = "^(09\\d{8}|0[2-7]\\d{7}|\\+5939\\d{8}|\\+593[2-7]\\d{7})$",
                message = "debe ser un celular (09XXXXXXXX) o un fijo (0XXXXXXXX) ecuatoriano, con o sin +593")
        String phone,
        @NotBlank @Email @Size(max = 100) String email
) {
}