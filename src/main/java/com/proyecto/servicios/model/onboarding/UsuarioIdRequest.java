package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UsuarioIdRequest(
        @NotNull @Positive Long usuarioId
) {
}
