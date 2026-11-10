package com.proyecto.servicios.service.onboarding;

public class SesionInvalidaException extends RuntimeException {

    public SesionInvalidaException() {
        super("La sesión ya no está activa porque fue cerrada o reemplazada desde otro dispositivo.");
    }

    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}

