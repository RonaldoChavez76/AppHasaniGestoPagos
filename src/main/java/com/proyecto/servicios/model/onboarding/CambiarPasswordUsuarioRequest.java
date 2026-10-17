package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CambiarPasswordUsuarioRequest(
        @NotNull @Positive Long usuarioId,
        @NotNull @Valid CambiarPasswordRequest cambio
) {
}
