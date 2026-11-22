package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 72) String passwordActual,
        @NotBlank
        @Size(max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, mayúscula, minúscula, número y símbolo."
        )
        String passwordNueva
) {
}
