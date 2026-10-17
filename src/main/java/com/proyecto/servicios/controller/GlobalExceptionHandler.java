package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.Impl.GestoPagoIntegrationException;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import com.proyecto.servicios.service.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.CuentaNoEncontradaException;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.onboarding.UsuarioNoEncontradoException;
import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrationException(
            GestoPagoIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.failure(exception.getStatus().value(), exception.getMessage()));
    }

    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleClienteDuplicado(ClienteDuplicadoException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictoDeDatos(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro o una regla de la base de datos.");
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleClienteNoEncontrado(
            ClienteNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccesoNoAutorizado(
            AccesoNoAutorizadoException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> handleCuentaNoEncontrada(CuentaNoEncontradaException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiResponse<Void>> handleCredencialesInvalidas(
            CredencialesInvalidasException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioInactivo(UsuarioInactivoException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleErrorValidacion(RuntimeException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleRequestInvalido(MethodArgumentNotValidException exception) {
        Map<String, List<FieldError>> erroresPorCampo = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                erroresPorCampo.computeIfAbsent(error.getField(), campo -> new ArrayList<>()).add(error));

        String detalleErrores = erroresPorCampo.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + mensajesDeCampo(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
        String mensaje = detalleErrores.isEmpty()
                ? "La solicitud contiene datos inválidos."
                : "Datos no válidos: " + detalleErrores;
        return error(HttpStatus.BAD_REQUEST, mensaje);
    }

    private String mensajesDeCampo(
            String campo,
            List<FieldError> errores
    ) {
        boolean requerido = errores.stream().anyMatch(error ->
                tieneRestriccion(error, "NotBlank") || tieneRestriccion(error, "NotNull"));
        if (requerido && errores.stream().anyMatch(error ->
                error.getRejectedValue() == null
                        || error.getRejectedValue() instanceof String valor && valor.isBlank())) {
            return "es obligatorio.";
        }

        Set<String> mensajes = new LinkedHashSet<>();
        errores.forEach(error -> mensajes.add(mensajeValidacion(campo, error)));
        return String.join(" ", mensajes);
    }

    private boolean tieneRestriccion(FieldError error, String restriccion) {
        return error.getCodes() != null && Arrays.stream(error.getCodes())
                .anyMatch(codigo -> codigo.equals(restriccion) || codigo.startsWith(restriccion + "."));
    }

    private String mensajeValidacion(String campo, FieldError error) {
        if (tieneRestriccion(error, "NotBlank") || tieneRestriccion(error, "NotNull")) {
            return "es obligatorio.";
        }
        if (tieneRestriccion(error, "Size")) {
            return "no cumple con la longitud permitida.";
        }
        if (tieneRestriccion(error, "Email")) {
            return "debe tener un formato de correo válido.";
        }
        if (tieneRestriccion(error, "Past")) {
            return "debe ser una fecha pasada.";
        }
        if (tieneRestriccion(error, "DecimalMin")) {
            return "debe ser mayor que cero.";
        }
        if (tieneRestriccion(error, "Digits")) {
            return "debe tener como máximo 10 dígitos enteros y 2 decimales.";
        }
        if (tieneRestriccion(error, "Pattern")) {
            return mensajeFormato(campo);
        }
        return "no es válido.";
    }

    private String mensajeFormato(String campo) {
        return switch (campo) {
            case "primerNombre", "segundoNombre", "apellidoPaterno", "apellidoMaterno" ->
                    "solo puede contener letras y espacios.";
            case "curp" -> "debe tener un formato de CURP válido (18 caracteres).";
            case "rfc" -> "debe tener un formato de RFC válido (12 o 13 caracteres).";
            case "correo" -> "debe tener un formato de correo válido.";
            case "telefonoMovil", "telefonoAlternativo" ->
                    "debe contener exactamente 10 dígitos.";
            case "codigoPostal" -> "debe contener exactamente 5 dígitos.";
            case "password" -> "debe tener al menos 8 caracteres, mayúscula, minúscula, número y símbolo.";
            default -> "tiene un formato inválido.";
        };
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonInvalido(HttpMessageNotReadableException exception) {
        UnrecognizedPropertyException propiedadDesconocida = causa(exception, UnrecognizedPropertyException.class);
        if (propiedadDesconocida != null) {
            return error(HttpStatus.BAD_REQUEST, "El campo '" + propiedadDesconocida.getPropertyName()
                    + "' no pertenece a este endpoint. Revisa los campos del Request DTO.");
        }

        JsonMappingException errorDeMapeo = causa(exception, JsonMappingException.class);
        if (errorDeMapeo != null && !errorDeMapeo.getPath().isEmpty()) {
            String campo = errorDeMapeo.getPath().get(errorDeMapeo.getPath().size() - 1).getFieldName();
            if (campo != null) {
                return error(HttpStatus.BAD_REQUEST,
                        "El campo '" + campo + "' tiene un tipo o formato inválido.");
            }
        }

        return error(HttpStatus.BAD_REQUEST,
                "No se pudo leer la solicitud. Verifica que el JSON tenga comillas, llaves y comas correctas "
                        + "y que sus campos correspondan a este endpoint.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleParametroInvalido(
            MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + exception.getName() + "' tiene un formato inválido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Error inesperado al procesar una solicitud.", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(500, "Error interno del servidor"));
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(status.value(), message));
    }

    private <T extends Throwable> T causa(Throwable exception, Class<T> tipo) {
        Throwable actual = exception;
        while (actual != null) {
            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }
            actual = actual.getCause();
        }
        return null;
    }
}