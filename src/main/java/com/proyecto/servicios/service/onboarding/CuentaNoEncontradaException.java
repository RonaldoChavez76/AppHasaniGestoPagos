package com.proyecto.servicios.service.onboarding;

public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No existe la cuenta con número " + numeroCuenta + ".");
    }
}
