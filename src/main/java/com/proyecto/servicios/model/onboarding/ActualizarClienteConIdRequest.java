package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ActualizarClienteConIdRequest(
        @NotNull @Positive Long clienteId,
        @NotNull @Valid ActualizarClienteRequest datos
) {
}
