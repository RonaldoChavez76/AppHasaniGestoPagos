package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud unificada para transacciones GestoPago (recarga o pago de servicio)")
public class TransaccionGestoPagoRequest {

    @Schema(description = "Tipo de operación: RECARGA o PAGO_SERVICIO (si se omite, se infiere del campo esServicio o referencia)", example = "RECARGA")
    private TipoOperacionGestoPago tipoOperacion;

    @Schema(description = "Bandera booleana alternativa para indicar si es pago de servicio (true) o recarga (false)", example = "false")
    private Boolean esServicio;

    @NotNull(message = "idProducto es requerido")
    @Schema(description = "Identificador numérico del producto GestoPago", example = "582")
    private Integer idProducto;

    @NotNull(message = "idServicio es requerido")
    @Schema(description = "Identificador numérico del servicio GestoPago", example = "133")
    private Integer idServicio;

    @Schema(description = "Número telefónico (requerido para recargas; para servicios defaults a 1111111111)", example = "5555555551")
    private String telefono;

    @Schema(description = "Referencia del recibo/servicio (requerido para pago de servicios)", example = "25720381")
    private String referencia;

    @Schema(description = "Monto a pagar (requerido para pago de servicios)", example = "10.00")
    private BigDecimal montoPago;

    @Size(min = 3, max = 15, message = "El UPC debe tener entre 3 y 15 caracteres alfanuméricos")
    @Schema(description = "Código único de transacción (opcional, se autogenera si se omite)", example = "UPC123456789")
    private String upc;

    @Size(min = 1, max = 25, message = "La unidad no debe exceder 25 caracteres")
    @Schema(description = "Sucursal o unidad comercial", example = "083")
    private String unidad;

    @Schema(description = "Número de cuenta bancaria local para debitar el saldo (opcional)", example = "8766090787749132")
    private String numeroCuenta;

    @Schema(description = "Si es true, simula la aprobación de GestoPago para pruebas locales sin depender de lista blanca de IP", example = "false")
    private Boolean simular;

    @Pattern(regexp = "^[0-9]{6}$", message = "El código 2FA debe ser de 6 dígitos numéricos")
    @Schema(description = "Código OTP de 6 dígitos de segundo factor (2FA) para autorizar la operación", example = "482910")
    private String codigo2FA;
}

