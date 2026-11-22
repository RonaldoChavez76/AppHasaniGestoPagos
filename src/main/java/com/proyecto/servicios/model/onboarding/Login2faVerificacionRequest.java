package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de verificación de código 2FA para completar el inicio de sesión y activar la sesión única")
public record Login2faVerificacionRequest(
        @Schema(description = "Correo electrónico registrado del cliente", example = "ana.lopez.prueba@example.com")
        @NotBlank @Email @Size(max = 100) String correo,

        @Schema(description = "Contraseña del usuario", example = "Prueba2026!")
        @NotBlank String password,

        @Schema(description = "Código OTP de 6 dígitos generado en el servidor", example = "482910")
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String codigo2FA,

        @Schema(description = "Plataforma desde la que se completa el acceso (WEB o MOVIL)", example = "MOVIL")
        String plataforma,

        @Schema(description = "Si es true, cierra cualquier sesión activa previa en otra plataforma", example = "false")
        Boolean forzarCierreSesion
) {
    public Login2faVerificacionRequest {
        correo = OnboardingText.strip(correo);
        codigo2FA = OnboardingText.strip(codigo2FA);
        plataforma = (plataforma == null || plataforma.isBlank()) ? "WEB" : plataforma.trim().toUpperCase();
        forzarCierreSesion = Boolean.TRUE.equals(forzarCierreSesion);
    }
}

