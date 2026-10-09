package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.IntegrationErrorResponse;
import com.proyecto.servicios.service.Impl.GestoPagoIntegrationException;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import com.proyecto.servicios.service.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.onboarding.UsuarioNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<IntegrationErrorResponse> handleIntegrationException(
            GestoPagoIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new IntegrationErrorResponse(
                        exception.getStatus().value(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<IntegrationErrorResponse> handleClienteDuplicado(ClienteDuplicadoException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<IntegrationErrorResponse> handleConflictoDeDatos(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro o una regla de la base de datos.");
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<IntegrationErrorResponse> handleClienteNoEncontrado(
            ClienteNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<IntegrationErrorResponse> handleUsuarioNoEncontrado(UsuarioNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<IntegrationErrorResponse> handleAccesoNoAutorizado(
            AccesoNoAutorizadoException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<IntegrationErrorResponse> handleCuentaNoEncontrada(CuentaNoEncontradaException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<IntegrationErrorResponse> handleCredencialesInvalidas(
            CredencialesInvalidasException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<IntegrationErrorResponse> handleUsuarioInactivo(UsuarioInactivoException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<IntegrationErrorResponse> handleErrorValidacion(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<IntegrationErrorResponse> handleRequestInvalido(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("La solicitud contiene datos inválidos.");
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<IntegrationErrorResponse> handleJsonInvalido(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no contiene un JSON válido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<IntegrationErrorResponse> handleParametroInvalido(
            MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + exception.getName() + "' tiene un formato inválido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<IntegrationErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Error inesperado al procesar una solicitud.", exception);
        return ResponseEntity.internalServerError()
                .body(new IntegrationErrorResponse(500, "Error interno del servidor", Instant.now()));
    }

    private ResponseEntity<IntegrationErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new IntegrationErrorResponse(status.value(), message, Instant.now()));
    }
}