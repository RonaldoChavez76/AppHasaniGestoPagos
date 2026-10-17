package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BuscarCuentasRequest(
        @Size(max = 20)
        String numeroCuenta,
        @Positive
        Long clienteId,
        Boolean soloActivas,
        @Min(0)
        Integer page,
        @Min(1)
        @Max(100)
        Integer size
) {
    public BuscarCuentasRequest {
        numeroCuenta = OnboardingText.optional(numeroCuenta);
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        soloActivas = soloActivas == null ? numeroCuenta == null : soloActivas;
    }
}
