package com.proyecto.servicios.service.onboarding;

public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException() {
        super("No está autorizado para consultar o modificar este usuario.");
    }
}
