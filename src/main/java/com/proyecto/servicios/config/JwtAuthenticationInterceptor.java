package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Locale;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ATTRIBUTE =
            JwtAuthenticationInterceptor.class.getName() + ".authenticatedUser";
    public static final String AUTHENTICATED_ROLE_ATTRIBUTE =
            JwtAuthenticationInterceptor.class.getName() + ".authenticatedRole";

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationInterceptor(
            JwtTokenService jwtTokenService,
            UsuarioRepository usuarioRepository,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/clientes".equals(ruta(request))) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return rechazar(response, "Se requiere un token Bearer válido.");
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return rechazar(response, "Se requiere un token Bearer válido.");
        }

        var usuario = jwtTokenService.validar(token);
        if (usuario.isEmpty()) {
            return rechazar(response, HttpStatus.UNAUTHORIZED, "El token es inválido o expiró.");
        }
        var rol = usuarioRepository.findRolActivaById(usuario.get().usuarioId());
        if (rol.isEmpty()) {
            return rechazar(response, HttpStatus.FORBIDDEN, "El usuario está inactivo.");
        }
        request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, usuario.get());
        request.setAttribute(AUTHENTICATED_ROLE_ATTRIBUTE, rol.get());
        if (!estaAutorizado(request, rol.get())) {
            return rechazar(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.");
        }
        return true;
    }

    private boolean estaAutorizado(
            HttpServletRequest request,
            RolUsuario rol
    ) {
        String ruta = ruta(request);
        String metodo = request.getMethod().toUpperCase(Locale.ROOT);
        if (rol == RolUsuario.EJECUTIVO) {
            return true;
        }

        return ("POST".equals(metodo) && (
                "/clientes/busqueda".equals(ruta)
                        || "/clientes/consulta".equals(ruta)
                        || "/cuentas/busqueda".equals(ruta)
                        || "/usuarios/consulta".equals(ruta)))
                || ("PUT".equals(metodo) && (
                "/clientes/actualizar".equals(ruta)
                        || "/usuarios/password".equals(ruta)))
                || ("DELETE".equals(metodo) && "/clientes/baja".equals(ruta));
    }

    private String ruta(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return ruta.toLowerCase(Locale.ROOT);
    }

    private boolean rechazar(HttpServletResponse response, String mensaje) throws IOException {
        return rechazar(response, HttpStatus.UNAUTHORIZED, mensaje);
    }

    private boolean rechazar(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(status.value(), mensaje));
        return false;
    }
}
