package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.IntegrationErrorResponse;
import com.proyecto.servicios.service.Impl.GestoPagoIntegrationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<IntegrationErrorResponse> handleIntegrationException(
            GestoPagoIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new IntegrationErrorResponse(
                        exception.getStatus().value(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<IntegrationErrorResponse> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(new IntegrationErrorResponse(500, "Error interno del servidor", Instant.now()));
    }
}