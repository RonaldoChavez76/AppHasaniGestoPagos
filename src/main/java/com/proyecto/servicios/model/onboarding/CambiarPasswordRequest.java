package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
                @NotBlank @Size(max = 72) String passwordActual,
                @NotBlank @Size(min = 8, max = 12) @Pattern(regexp = "^(?!.*\\s)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,12}$", message = "La contraseña debe tener entre 8 y 12 caracteres sin espacios, con mayúscula, minúscula, número y símbolo.") String passwordNueva) {
}
