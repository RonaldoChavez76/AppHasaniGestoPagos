package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank String password
) {
    public LoginRequest {
        correo = OnboardingText.strip(correo);
    }
}
