package com.proyecto.servicios.service.onboarding;

public class ClienteDuplicadoException extends RuntimeException {

    public ClienteDuplicadoException(String campo) {
        super("Ya existe un cliente registrado con el mismo " + campo + ".");
    }
}
