package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse obtenerPorId(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        validarPropietario(id, request);
        return usuarioService.obtenerPorId(id);
    }

    @PutMapping(value = "/{id}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequest cambio,
            HttpServletRequest request
    ) {
        validarPropietario(id, request);
        usuarioService.cambiarPassword(id, cambio);
        return ResponseEntity.noContent().build();
    }

    private void validarPropietario(Long id, HttpServletRequest request) {
        Object authenticated = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(authenticated instanceof JwtTokenService.AuthenticatedUser user) || !id.equals(user.usuarioId())) {
            throw new AccesoNoAutorizadoException();
        }
    }
}
