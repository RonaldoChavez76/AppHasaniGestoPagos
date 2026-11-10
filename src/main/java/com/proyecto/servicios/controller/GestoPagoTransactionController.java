package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.RecargaTelefonicaRequest;
import com.proyecto.servicios.model.gestopago.TransaccionGestoPagoRequest;
import com.proyecto.servicios.service.GestoPagoTransactionService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.SesionInvalidaException;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/gestopago/transacciones", "/transacciones"})
@Tag(name = "GestoPago Transacciones (sendTx)", description = "Operaciones de venta de tiempo aire (recargas), pago de servicios e historial de compras exitosas")
public class GestoPagoTransactionController {

    private final GestoPagoTransactionService transactionService;
    private final JwtTokenService jwtTokenService;
    private final SesionUsuarioService sesionUsuarioService;

    @Autowired
    public GestoPagoTransactionController(
            GestoPagoTransactionService transactionService,
            @Autowired(required = false) JwtTokenService jwtTokenService,
            @Autowired(required = false) SesionUsuarioService sesionUsuarioService
    ) {
        this.transactionService = transactionService;
        this.jwtTokenService = jwtTokenService;
        this.sesionUsuarioService = sesionUsuarioService;
    }

    public GestoPagoTransactionController(GestoPagoTransactionService transactionService) {
        this(transactionService, null, null);
    }

    @PostMapping(value = "/recarga", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Realizar recarga telefónica / tiempo aire", description = "Diferenciación de operación para recarga telefónica (tipoFront = 1)")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarRecarga(
            @Valid @RequestBody RecargaTelefonicaRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarSesionSiPresente(authorization);
        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @PostMapping(value = "/servicio", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Realizar pago de servicio", description = "Diferenciación de operación para pago de recibos o servicios (tipoFront = 2)")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarPagoServicio(
            @Valid @RequestBody PagoServicioRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarSesionSiPresente(authorization);
        GestoPagoTransaccionResponse response = transactionService.procesarPagoServicio(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Procesar transacción unificada (con bandera o discriminador)", description = "Endpoint unificado que evalúa si la transacción es recarga o pago de servicio según el flag o campos enviados")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarTransaccionUnificada(
            @Valid @RequestBody TransaccionGestoPagoRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarSesionSiPresente(authorization);
        GestoPagoTransaccionResponse response = transactionService.procesarTransaccion(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @GetMapping(value = {"/historial/exitosas", "/mis-compras"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Consultar historial de compras exitosas del usuario",
            description = "Retorna únicamente las operaciones confirmadas como EXITOSAS asociadas al usuario autenticado o cuenta indicada, incluyendo fecha, producto, importe y referencia."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<GestoPagoTransaccionResponse>>> listarComprasExitosas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String numeroCuenta,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        JwtTokenService.AuthenticatedUser user = validarSesionSiPresente(authorization);
        Long clienteIdEfectivo = clienteId != null ? clienteId : (user != null ? user.clienteId() : null);

        List<GestoPagoTransaccionResponse> exitosas =
                transactionService.listarComprasExitosasPorUsuario(clienteIdEfectivo, numeroCuenta);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Historial de compras exitosas consultado correctamente", exitosas));
    }

    @GetMapping(value = "/{upc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar estado y detalle de transacción por UPC", description = "Obtiene los detalles y comprobante de una transacción registrada en base de datos mediante su código UPC")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> consultarPorUpc(
            @PathVariable String upc,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarSesionSiPresente(authorization);
        return transactionService.consultarTransaccionPorUpc(upc)
                .map(tx -> ResponseEntity.ok(ApiResponse.success(
                        HttpStatus.OK.value(), "Transacción consultada correctamente", tx)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(
                                HttpStatus.NOT_FOUND.value(), "No se encontró ninguna transacción con el UPC especificado")));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Historial de transacciones", description = "Lista las transacciones registradas localmente en PostgreSQL, permitiendo filtrar opcionalmente solo exitosas por cuenta o cliente")
    public ResponseEntity<ApiResponse<List<GestoPagoTransaccionResponse>>> listarHistorial(
            @RequestParam(required = false) Boolean soloExitosas,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String numeroCuenta,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        JwtTokenService.AuthenticatedUser user = validarSesionSiPresente(authorization);

        if (Boolean.TRUE.equals(soloExitosas) || clienteId != null || (numeroCuenta != null && !numeroCuenta.isBlank())) {
            Long clienteIdEfectivo = clienteId != null ? clienteId : (user != null ? user.clienteId() : null);
            List<GestoPagoTransaccionResponse> filtradas =
                    transactionService.listarComprasExitosasPorUsuario(clienteIdEfectivo, numeroCuenta);
            return ResponseEntity.ok(ApiResponse.success(
                    HttpStatus.OK.value(), "Historial de compras exitosas consultado correctamente", filtradas));
        }

        List<GestoPagoTransaccionResponse> historial = transactionService.listarUltimasTransacciones();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Historial de transacciones consultado correctamente", historial));
    }

    private JwtTokenService.AuthenticatedUser validarSesionSiPresente(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7) || jwtTokenService == null) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }
        JwtTokenService.AuthenticatedUser user = jwtTokenService.validar(token)
                .orElseThrow(() -> new SesionInvalidaException("El token JWT es inválido o expiró."));

        if (sesionUsuarioService != null && user.sessionId() != null) {
            if (!sesionUsuarioService.esSesionValida(user.usuarioId(), user.sessionId())) {
                throw new SesionInvalidaException();
            }
        }
        return user;
    }
}
