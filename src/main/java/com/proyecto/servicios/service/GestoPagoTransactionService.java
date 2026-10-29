package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.RecargaTelefonicaRequest;
import com.proyecto.servicios.model.gestopago.TransaccionGestoPagoRequest;

import java.util.List;
import java.util.Optional;

public interface GestoPagoTransactionService {

    /**
     * Procesa una venta de tiempo aire / recarga telefónica (tipoFront = 1).
     */
    GestoPagoTransaccionResponse procesarRecarga(RecargaTelefonicaRequest request);

    /**
     * Procesa el pago de un servicio o factura (tipoFront = 2 o gift cards).
     */
    GestoPagoTransaccionResponse procesarPagoServicio(PagoServicioRequest request);

    /**
     * Procesa una transacción unificada mediante discriminador o banderas.
     */
    GestoPagoTransaccionResponse procesarTransaccion(TransaccionGestoPagoRequest request);

    /**
     * Consulta una transacción previamente registrada por su UPC.
     */
    Optional<GestoPagoTransaccionResponse> consultarTransaccionPorUpc(String upc);

    /**
     * Lista las transacciones más recientes registradas en la base de datos local.
     */
    List<GestoPagoTransaccionResponse> listarUltimasTransacciones();
}
