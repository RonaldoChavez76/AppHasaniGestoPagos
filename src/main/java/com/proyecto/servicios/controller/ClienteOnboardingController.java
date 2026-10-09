package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.model.onboarding.ActualizarClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.service.onboarding.PageRequestFactory;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
    public ResponseEntity<RegistroClienteResponse> registrar(
            @Valid @RequestBody RegistroClienteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroClienteService.registrar(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public PageResponse<ClienteResponse> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(clienteConsultaService.listarTodos(PageRequestFactory.create(page, size)));
    }

    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public PageResponse<ClienteResponse> listarActivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(clienteConsultaService.listarActivos(PageRequestFactory.create(page, size)));
    }

    @GetMapping(value = "/registrados", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public PageResponse<ClienteResponse> listarRegistrados(
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(clienteConsultaService.listarRegistradosEntre(
                desde, hasta, PageRequestFactory.create(page, size)));
    }

    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse obtenerPorCurp(@PathVariable String curp) {
        return clienteConsultaService.obtenerPorCurp(curp);
    }

    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse obtenerPorRfc(@PathVariable String rfc) {
        return clienteConsultaService.obtenerPorRfc(rfc);
    }

    @GetMapping(value = "/correo/{correo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse obtenerPorCorreo(@PathVariable String correo) {
        return clienteConsultaService.obtenerPorCorreo(correo);
    }

    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse obtenerPorCuenta(@PathVariable String numeroCuenta) {
        return clienteConsultaService.obtenerPorNumeroCuenta(numeroCuenta);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
        return clienteConsultaService.obtenerPorId(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarClienteRequest request
    ) {
        return clienteConsultaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        registroClienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

}
