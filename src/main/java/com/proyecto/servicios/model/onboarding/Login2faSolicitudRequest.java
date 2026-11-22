package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de pre-autenticación y generación de código 2FA para inicio de sesión")
public record Login2faSolicitudRequest(
        @Schema(description = "Correo electrónico registrado del cliente", example = "ana.lopez.prueba@example.com")
        @NotBlank @Email @Size(max = 100) String correo,

        @Schema(description = "Contraseña del usuario", example = "Prueba2026!")
        @NotBlank String password,

        @Schema(description = "Plataforma desde la que se solicita el acceso (WEB o MOVIL)", example = "MOVIL")
        String plataforma,

        @Schema(description = "Si es true, cierra la sesión activa existente en otra plataforma antes de continuar", example = "false")
        Boolean forzarCierreSesion
) {
    public Login2faSolicitudRequest {
        correo = OnboardingText.strip(correo);
        plataforma = (plataforma == null || plataforma.isBlank()) ? "WEB" : plataforma.trim().toUpperCase();
        forzarCierreSesion = Boolean.TRUE.equals(forzarCierreSesion);
    }
}

