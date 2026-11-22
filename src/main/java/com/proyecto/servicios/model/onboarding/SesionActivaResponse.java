package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Estado actual de la sesión activa del usuario y resumen de su cuenta")
public record SesionActivaResponse(
        @Schema(description = "Indica si la sesión se encuentra activa y vigente", example = "true")
        boolean activa,

        @Schema(description = "Identificador único de la sesión", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "Plataforma donde está activa la sesión", example = "MOVIL")
        String plataforma,

        @Schema(description = "ID del usuario", example = "2")
        Long usuarioId,

        @Schema(description = "ID del cliente", example = "3")
        Long clienteId,

        @Schema(description = "Nombre completo del cliente", example = "Ana María López García")
        String nombreCliente,

        @Schema(description = "Correo electrónico del cliente", example = "ana.lopez.prueba@example.com")
        String correo,

        @Schema(description = "Número de cuenta principal activa", example = "4229215075268914")
        String numeroCuenta,

        @Schema(description = "Saldo disponible actual de la cuenta", example = "4990.00")
        BigDecimal saldoCuenta,

        @Schema(description = "Fecha de inicio de la sesión", example = "2026-11-09T18:30:00")
        LocalDateTime fechaInicio,

        @Schema(description = "Fecha de expiración de la sesión", example = "2026-11-09T18:45:00")
        LocalDateTime fechaExpiracion
) {
}

