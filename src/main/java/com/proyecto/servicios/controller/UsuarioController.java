package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.CambiarPasswordUsuarioRequest;
import com.proyecto.servicios.model.onboarding.UsuarioIdRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping(value = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar usuario",
            description = "El usuario EJECUTIVO puede consultar cualquier usuario. El usuario CLIENTE solo puede "
                    + "consultar el usuario asociado a su propio token.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = "{\"usuarioId\":1}"))))
    public ApiResponse<UsuarioResponse> consultar(
            @Valid @RequestBody UsuarioIdRequest consulta,
            HttpServletRequest request
    ) {
        validarPropietarioOEjecutivo(consulta.usuarioId(), request);
        return ApiResponse.success(200, "Usuario consultado correctamente.",
                usuarioService.obtenerPorId(consulta.usuarioId()));
    }

    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordUsuarioRequest cambio,
            HttpServletRequest request
    ) {
        validarPropietario(cambio.usuarioId(), request);
        usuarioService.cambiarPassword(cambio.usuarioId(), cambio.cambio());
        return ResponseEntity.ok(ApiResponse.success(200, "Contraseña actualizada correctamente.", null));
    }

    private void validarPropietario(Long id, HttpServletRequest request) {
        Object authenticated = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(authenticated instanceof JwtTokenService.AuthenticatedUser user)) {
            throw new AccesoNoAutorizadoException("No se pudo identificar al usuario del token.");
        }
        if (!id.equals(user.usuarioId())) {
            throw new AccesoNoAutorizadoException(
                    "Solo puedes cambiar la contraseña del usuario asociado a tu token.");
        }
    }

    private void validarPropietarioOEjecutivo(Long id, HttpServletRequest request) {
        Object role = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE);
        if (role == RolUsuario.EJECUTIVO) {
            return;
        }
        if (role != RolUsuario.CLIENTE) {
            throw new AccesoNoAutorizadoException(
                    "El token no tiene un rol autorizado para consultar usuarios.");
        }
        Object authenticated = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(authenticated instanceof JwtTokenService.AuthenticatedUser user)) {
            throw new AccesoNoAutorizadoException("No se pudo identificar al usuario del token.");
        }
        if (!id.equals(user.usuarioId())) {
            throw new AccesoNoAutorizadoException(
                    "Un usuario CLIENTE solo puede consultar su propio usuario. "
                            + "Para consultar otro, inicia sesión con un token EJECUTIVO.");
        }
        validarPropietario(id, request);
    }
}
