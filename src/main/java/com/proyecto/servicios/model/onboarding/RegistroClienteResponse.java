package com.proyecto.servicios.model.onboarding;

public record RegistroClienteResponse(
        Long clienteId,
        Long usuarioId,
        Long cuentaId,
        String numeroCuenta,
        String correo
) {
}
