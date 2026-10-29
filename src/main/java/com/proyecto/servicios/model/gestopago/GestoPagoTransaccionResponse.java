package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta estructurada de la transacción en GestoPago")
public class GestoPagoTransaccionResponse {

    @Schema(description = "Identificador único de la transacción generado para PuntoRed (UPC)", example = "UPC123456789")
    private String upc;

    @Schema(description = "Tipo de operación procesada", example = "RECARGA")
    private TipoOperacionGestoPago tipoOperacion;

    @Schema(description = "Indica si la transacción fue aceptada y aplicada con éxito por el proveedor", example = "true")
    private boolean exitoso;

    @Schema(description = "Estado interno de la transacción", example = "EXITOSA")
    private EstadoTransaccion estado;

    @Schema(description = "Código de respuesta retornado por GestoPago ('01' es éxito)", example = "01")
    private String codigo;

    @Schema(description = "Mensaje textual retornado por GestoPago", example = "Operacion realizada con exito")
    private String mensaje;

    @Schema(description = "Número único de transacción asignado por GestoPago", example = "291437188")
    private String idTx;

    @Schema(description = "Número de autorización asignado por la operadora/proveedor", example = "100024988")
    private String numAutorizacion;

    @Schema(description = "Monto de la transacción aplicada", example = "10.00")
    private BigDecimal monto;

    @Schema(description = "Saldo de la cuenta tras aplicar la transacción", example = "157731.83")
    private String saldo;

    @Schema(description = "Saldo formateado", example = "157,731.83")
    private String saldoFormateado;

    @Schema(description = "Comisión cobrada por la transacción", example = "0.7")
    private String comision;

    @Schema(description = "Comisión formateada", example = "0.70")
    private String comisionFormateada;

    @Schema(description = "Fecha y hora de la transacción según GestoPago", example = "25/Feb/2022 18:15:25")
    private String fecha;

    @Schema(description = "Teléfono asociado a la transacción", example = "5555555551")
    private String telefono;

    @Schema(description = "Referencia del servicio pagado", example = "25720381")
    private String referencia;

    @Schema(description = "PIN, folio o URL para canjear (en gift cards / pines de entretenimiento)", example = "https://cutt.ly/KTP-014525-ewgts456")
    private String pin;

    @Schema(description = "Instrucciones de canje o términos de la transacción", example = "Conserve su ticket de compra.")
    private String legend;

    @Schema(description = "Número de cuenta bancaria debitada (si aplica)", example = "4229215075268914")
    private String numeroCuenta;

    @Schema(description = "Nuevo saldo de la cuenta bancaria tras el débito (si aplica)", example = "4990.00")
    private BigDecimal nuevoSaldoCuenta;

    @Schema(description = "Fecha y hora de registro local", example = "2026-10-26T15:00:00")
    private LocalDateTime fechaRegistro;
}

