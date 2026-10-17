package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DarDeBajaClienteRequest(
        @NotNull @Positive Long clienteId
) {
}
