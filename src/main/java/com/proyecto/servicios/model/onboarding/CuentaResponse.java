package com.proyecto.servicios.model.onboarding;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CuentaResponse(
        Long id,
        String numeroCuenta,
        Long clienteId,
        BigDecimal saldo,
        String estatus,
        OffsetDateTime fechaApertura
) {
}
