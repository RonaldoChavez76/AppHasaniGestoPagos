package com.proyecto.servicios.service.onboarding;

public class UsuarioInactivoException extends RuntimeException {

    public UsuarioInactivoException() {
        super("El usuario está inactivo.");
    }
}
