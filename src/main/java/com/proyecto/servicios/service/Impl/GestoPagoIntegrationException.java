package com.proyecto.servicios.service.Impl;

import org.springframework.http.HttpStatus;

public class GestoPagoIntegrationException extends RuntimeException {

    private final HttpStatus status;

    public GestoPagoIntegrationException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_GATEWAY, cause);
    }

    public GestoPagoIntegrationException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}