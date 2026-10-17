package com.proyecto.servicios.service.onboarding;

public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException() {
        this("No está autorizado para realizar esta operación.");
    }

    public AccesoNoAutorizadoException(String message) {
        super(message);
    }
}
