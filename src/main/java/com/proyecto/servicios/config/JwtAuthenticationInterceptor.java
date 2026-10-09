package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.model.IntegrationErrorResponse;
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
import java.time.Instant;
import java.util.Locale;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ATTRIBUTE =
            JwtAuthenticationInterceptor.class.getName() + ".authenticatedUser";
    public static final String AUTHENTICATED_ROLE_ATTRIBUTE =
            JwtAuthenticationInterceptor.class.getName() + ".authenticatedRole";

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationInterceptor(
            JwtTokenService jwtTokenService,
            UsuarioRepository usuarioRepository,
            CuentaRepository cuentaRepository,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
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
        if (!estaAutorizado(request, usuario.get(), rol.get())) {
            return rechazar(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación.");
        }
        return true;
    }

    private boolean estaAutorizado(
            HttpServletRequest request,
            JwtTokenService.AuthenticatedUser usuario,
            RolUsuario rol
    ) {
        String ruta = ruta(request);
        String metodo = request.getMethod().toUpperCase(Locale.ROOT);
        if (rol == RolUsuario.EJECUTIVO) {
            return true;
        }

        if ("/clientes".equals(ruta)
                || ruta.matches("^/clientes/(activos|registrados|curp/.*|rfc/.*|correo/.*)$")
                || "/cuentas/activas".equals(ruta)) {
            return false;
        }

        if (ruta.matches("^/clientes/cuenta/[^/]+$")) {
            String numeroCuenta = ruta.substring("/clientes/cuenta/".length());
            return cuentaRepository.existsByNumeroCuentaAndClienteId(numeroCuenta, usuario.clienteId());
        }

        if (ruta.matches("^/clientes/[0-9]+$")) {
            boolean esPropio = esClientePropio(ruta.substring("/clientes/".length()), usuario.clienteId());
            return esPropio && ("GET".equals(metodo) || "PUT".equals(metodo) || "DELETE".equals(metodo));
        }

        if (ruta.matches("^/cuentas/[^/]+(/saldo)?$") && "GET".equals(metodo)) {
            String numeroCuenta = ruta.substring("/cuentas/".length()).replaceFirst("/saldo$", "");
            return cuentaRepository.existsByNumeroCuentaAndClienteId(numeroCuenta, usuario.clienteId());
        }

        return ruta.matches("^/usuarios/[0-9]+(/password)?$")
                && ("GET".equals(metodo) || "PUT".equals(metodo));
    }

    private boolean esClientePropio(String idRuta, Long clienteId) {
        try {
            return clienteId.equals(Long.valueOf(idRuta));
        } catch (NumberFormatException exception) {
            return false;
        }
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
        objectMapper.writeValue(response.getWriter(), new IntegrationErrorResponse(status.value(), mensaje, Instant.now()));
        return false;
    }
}
