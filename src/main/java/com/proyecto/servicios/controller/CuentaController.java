package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoCuentaResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.service.onboarding.CuentaConsultaService;
import com.proyecto.servicios.service.onboarding.PageRequestFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaConsultaService cuentaConsultaService;

    public CuentaController(CuentaConsultaService cuentaConsultaService) {
        this.cuentaConsultaService = cuentaConsultaService;
    }

    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public PageResponse<CuentaResponse> listarActivas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return PageResponse.from(cuentaConsultaService.listarActivas(PageRequestFactory.create(page, size)));
    }

    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public CuentaResponse obtenerPorNumero(@PathVariable String numeroCuenta) {
        return cuentaConsultaService.obtenerPorNumero(numeroCuenta);
    }

    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public SaldoCuentaResponse obtenerSaldo(@PathVariable String numeroCuenta) {
        return cuentaConsultaService.obtenerSaldo(numeroCuenta);
    }
}
