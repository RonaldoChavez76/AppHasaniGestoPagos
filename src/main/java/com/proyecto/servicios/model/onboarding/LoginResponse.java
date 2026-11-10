package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de inicio de sesión exitoso con token JWT y datos de la sesión única")
public record LoginResponse(
        @Schema(description = "Token JWT firmado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Tiempo de vida del token en segundos", example = "900")
        long expiresIn,

        @Schema(description = "Fecha y hora exacta de expiración del token", example = "2026-11-09T19:00:00Z")
        Instant expiresAt,

        @Schema(description = "Identificador único de la sesión activa en el servidor", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "Plataforma en la que quedó activa la sesión (WEB o MOVIL)", example = "MOVIL")
        String plataforma,

        @Schema(description = "ID del cliente autenticado", example = "3")
        Long clienteId,

        @Schema(description = "Nombre completo del cliente", example = "Ana María López García")
        String nombreCliente,

        @Schema(description = "Correo del cliente", example = "ana.lopez.prueba@example.com")
        String correo,

        @Schema(description = "Número de cuenta bancaria principal activa", example = "4229215075268914")
        String numeroCuenta,

        @Schema(description = "Saldo disponible actual en la cuenta", example = "4990.00")
        BigDecimal saldoCuenta
) {
    public LoginResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt) {
        this(accessToken, tokenType, expiresIn, expiresAt, null, null, null, null, null, null, null);
    }
}
