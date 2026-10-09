package com.proyecto.servicios.service.onboarding;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe el usuario con ID " + id + ".");
    }
}
