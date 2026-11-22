package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detalle del conflicto cuando el usuario ya tiene una sesión activa en otra plataforma")
public record ConflictoSesionResponse(
        @Schema(description = "Indica que existe una sesión activa previa", example = "true")
        boolean sesionActivaExistente,

        @Schema(description = "Plataforma donde se encuentra activa la sesión actual", example = "MOVIL")
        String plataformaActiva,

        @Schema(description = "Fecha y hora de inicio de la sesión activa", example = "2026-11-09T18:30:00")
        LocalDateTime fechaInicio,

        @Schema(description = "Fecha y hora de expiración de la sesión activa", example = "2026-11-09T18:45:00")
        LocalDateTime fechaExpiracion,

        @Schema(description = "Instrucción para el cliente", example = "Confirme el cierre de la sesión anterior enviando forzarCierreSesion=true.")
        String accionRequerida
) {
}

