package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.RecargaTelefonicaRequest;
import com.proyecto.servicios.model.gestopago.TransaccionGestoPagoRequest;
import com.proyecto.servicios.service.GestoPagoTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/gestopago/transacciones", "/transacciones"})
@Tag(name = "GestoPago Transacciones (sendTx)", description = "Operaciones de venta de tiempo aire (recargas) y pago de servicios mediante sendTx")
public class GestoPagoTransactionController {

    private final GestoPagoTransactionService transactionService;

    public GestoPagoTransactionController(GestoPagoTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping(value = "/recarga", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Realizar recarga telefónica / tiempo aire", description = "Diferenciación de operación para recarga telefónica (tipoFront = 1)")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarRecarga(
            @Valid @RequestBody RecargaTelefonicaRequest request) {
        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @PostMapping(value = "/servicio", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Realizar pago de servicio", description = "Diferenciación de operación para pago de recibos o servicios (tipoFront = 2)")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarPagoServicio(
            @Valid @RequestBody PagoServicioRequest request) {
        GestoPagoTransaccionResponse response = transactionService.procesarPagoServicio(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Procesar transacción unificada (con bandera o discriminador)", description = "Endpoint unificado que evalúa si la transacción es recarga o pago de servicio según el flag o campos enviados")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> procesarTransaccionUnificada(
            @Valid @RequestBody TransaccionGestoPagoRequest request) {
        GestoPagoTransaccionResponse response = transactionService.procesarTransaccion(request);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response));
    }

    @GetMapping(value = "/{upc}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar estado de transacción por UPC", description = "Obtiene los detalles y comprobante de una transacción registrada en base de datos mediante su código UPC")
    public ResponseEntity<ApiResponse<GestoPagoTransaccionResponse>> consultarPorUpc(
            @PathVariable String upc) {
        return transactionService.consultarTransaccionPorUpc(upc)
                .map(tx -> ResponseEntity.ok(ApiResponse.success(
                        HttpStatus.OK.value(), "Transacción consultada correctamente", tx)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure(
                                HttpStatus.NOT_FOUND.value(), "No se encontró ninguna transacción con el UPC especificado")));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Historial de transacciones", description = "Lista las últimas transacciones registradas localmente en PostgreSQL")
    public ResponseEntity<ApiResponse<List<GestoPagoTransaccionResponse>>> listarHistorial() {
        List<GestoPagoTransaccionResponse> historial = transactionService.listarUltimasTransacciones();
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Historial de transacciones consultado correctamente", historial));
    }
}
