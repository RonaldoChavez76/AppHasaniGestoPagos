package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de inicio de sesión con soporte para control de sesión única por plataforma y 2FA")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado del cliente", example = "ana.lopez.prueba@example.com")
        @NotBlank @Email @Size(max = 100) String correo,

        @Schema(description = "Contraseña del usuario", example = "Prueba2026!")
        @NotBlank String password,

        @Schema(description = "Plataforma desde la que ingresa el usuario (WEB o MOVIL)", example = "WEB")
        String plataforma,

        @Schema(description = "Indica si desea cerrar una sesión activa previa en otra plataforma", example = "false")
        Boolean forzarCierreSesion,

        @Schema(description = "Código OTP de segundo factor (2FA) de 6 dígitos (opcional si se usa el endpoint directo)", example = "482910")
        String codigo2FA
) {
    public LoginRequest {
        correo = OnboardingText.strip(correo);
        plataforma = (plataforma == null || plataforma.isBlank()) ? "WEB" : plataforma.trim().toUpperCase();
        forzarCierreSesion = Boolean.TRUE.equals(forzarCierreSesion);
        codigo2FA = OnboardingText.strip(codigo2FA);
    }

    public LoginRequest(String correo, String password) {
        this(correo, password, "WEB", false, null);
    }
}
