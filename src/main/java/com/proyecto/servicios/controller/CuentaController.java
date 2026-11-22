package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.BuscarCuentasRequest;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.CuentaConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaConsultaService cuentaConsultaService;

    public CuentaController(CuentaConsultaService cuentaConsultaService) {
        this.cuentaConsultaService = cuentaConsultaService;
    }

    @PostMapping(value = "/busqueda", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<CuentaResponse>> buscar(
            @Valid @RequestBody BuscarCuentasRequest filtros,
            HttpServletRequest request
    ) {
        Object role = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE);
        if (!(role instanceof RolUsuario)) {
            throw new AccesoNoAutorizadoException();
        }
        if (role == RolUsuario.CLIENTE) {
            if (filtros.numeroCuenta() == null) {
                throw new AccesoNoAutorizadoException();
            }
            Object authenticated = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
            if (!(authenticated instanceof JwtTokenService.AuthenticatedUser user)) {
                throw new AccesoNoAutorizadoException();
            }
            if (user.clienteId() == null) {
                throw new AccesoNoAutorizadoException();
            }
            filtros = new BuscarCuentasRequest(filtros.numeroCuenta(), user.clienteId(),
                    filtros.soloActivas(), filtros.page(), filtros.size());
        }
        return ApiResponse.success(200, "Búsqueda de cuentas completada.",
                PageResponse.from(cuentaConsultaService.buscar(filtros)));
    }
}
