package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.ActualizarClienteConIdRequest;
import com.proyecto.servicios.model.onboarding.BuscarClientesRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ConsultarClienteRequest;
import com.proyecto.servicios.model.onboarding.DarDeBajaClienteRequest;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.model.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteOnboardingController {

    private final RegistroClienteService registroClienteService;
    private final ClienteConsultaService clienteConsultaService;

    public ClienteOnboardingController(
            RegistroClienteService registroClienteService,
            ClienteConsultaService clienteConsultaService
    ) {
        this.registroClienteService = registroClienteService;
        this.clienteConsultaService = clienteConsultaService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RegistroClienteResponse>> registrar(
            @Valid @RequestBody RegistroClienteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                HttpStatus.CREATED.value(), "Cliente registrado correctamente.",
                registroClienteService.registrar(request)));
    }

    @PostMapping(value = "/busqueda", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Buscar clientes",
            description = "El usuario EJECUTIVO puede combinar filtros. El usuario CLIENTE solo puede "
                    + "buscar usando el número de su propia cuenta.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Buscar por nombre",
                                            value = "{\"nombre\":\"ana\",\"page\":0,\"size\":20}"),
                                    @ExampleObject(name = "Listar clientes (EJECUTIVO)",
                                            value = "{\"page\":0,\"size\":20}")
                            })))
    public ApiResponse<PageResponse<ClienteResponse>> buscar(
            @Valid @RequestBody BuscarClientesRequest filtros,
            HttpServletRequest request
    ) {
        var usuario = usuarioAutenticado(request);
        if (rolAutenticado(request) == RolUsuario.CLIENTE) {
            if (filtros.numeroCuenta() == null || filtros.nombre() != null || filtros.rfc() != null
                    || filtros.curp() != null || filtros.correo() != null || filtros.clienteId() != null
                    || filtros.activo() != null || filtros.registradoDesde() != null
                    || filtros.registradoHasta() != null) {
                throw new AccesoNoAutorizadoException(
                        "Los clientes solo pueden buscar usando el número de su propia cuenta. "
                                + "Los demás filtros requieren un usuario EJECUTIVO.");
            }
            filtros = new BuscarClientesRequest(null, null, null, null, filtros.numeroCuenta(),
                    clienteAutenticadoId(usuario), null, null, null, filtros.page(), filtros.size());
        }
        return ApiResponse.success(200, "Búsqueda de clientes completada.",
                PageResponse.from(clienteConsultaService.buscar(filtros)));
    }

    @PostMapping(value = "/consulta", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar un cliente por ID",
            description = "Esta operación consulta un solo cliente y recibe únicamente clienteId. "
                    + "Para filtrar o listar varios clientes, usa POST /clientes/busqueda.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Consultar cliente",
                                    value = "{\"clienteId\":1}"))))
    public ApiResponse<ClienteResponse> consultar(
            @Valid @RequestBody ConsultarClienteRequest consulta,
            HttpServletRequest request
    ) {
        validarClientePropioOEjecutivo(consulta.clienteId(), request);
        return ApiResponse.success(200, "Cliente consultado correctamente.",
                clienteConsultaService.obtenerPorId(consulta.clienteId()));
    }

    @PutMapping(value = "/actualizar", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ClienteResponse> actualizar(
            @Valid @RequestBody ActualizarClienteConIdRequest actualizacion,
            HttpServletRequest request
    ) {
        validarClientePropioOEjecutivo(actualizacion.clienteId(), request);
        return ApiResponse.success(200, "Cliente actualizado correctamente.",
                clienteConsultaService.actualizar(actualizacion.clienteId(), actualizacion.datos()));
    }

    @DeleteMapping(value = "/baja", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> darDeBaja(
            @Valid @RequestBody DarDeBajaClienteRequest baja,
            HttpServletRequest request
    ) {
        validarClientePropioOEjecutivo(baja.clienteId(), request);
        registroClienteService.darDeBaja(baja.clienteId());
        return ResponseEntity.ok(ApiResponse.success(200, "Cliente dado de baja correctamente.", null));
    }

    private void validarClientePropioOEjecutivo(Long clienteId, HttpServletRequest request) {
        if (rolAutenticado(request) == RolUsuario.CLIENTE
                && !clienteId.equals(usuarioAutenticado(request).clienteId())) {
            throw new AccesoNoAutorizadoException(
                    "El usuario autenticado tiene rol CLIENTE y solo puede consultar, actualizar o dar de baja "
                            + "su propio cliente. Verifica que el usuario tenga rol EJECUTIVO si debe acceder "
                            + "al cliente " + clienteId + ".");
        }
    }

    private Long clienteAutenticadoId(JwtTokenService.AuthenticatedUser usuario) {
        if (usuario.clienteId() == null) {
            throw new AccesoNoAutorizadoException();
        }
        return usuario.clienteId();
    }

    private JwtTokenService.AuthenticatedUser usuarioAutenticado(HttpServletRequest request) {
        Object authenticated = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(authenticated instanceof JwtTokenService.AuthenticatedUser user)) {
            throw new AccesoNoAutorizadoException();
        }
        return user;
    }

    private RolUsuario rolAutenticado(HttpServletRequest request) {
        Object role = request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE);
        if (!(role instanceof RolUsuario rol)) {
            throw new AccesoNoAutorizadoException();
        }
        return rol;
    }
}
