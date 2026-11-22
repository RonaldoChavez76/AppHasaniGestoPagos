package com.proyecto.servicios.service.onboarding;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo electrónico o contraseña incorrectos.");
    }
}
