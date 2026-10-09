package com.proyecto.servicios.model.onboarding;

import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long id,
        Long clienteId,
        String correo,
        boolean activo,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
