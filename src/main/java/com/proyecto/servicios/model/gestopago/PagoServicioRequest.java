package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Solicitud para pago de servicios o compra de pines/gift cards")
public class PagoServicioRequest {

    @NotNull(message = "idProducto es requerido")
    @Schema(description = "Identificador numérico del producto GestoPago", example = "205")
    private Integer idProducto;

    @NotNull(message = "idServicio es requerido")
    @Schema(description = "Identificador numérico del servicio GestoPago", example = "76")
    private Integer idServicio;

    @NotBlank(message = "referencia es requerida")
    @Size(min = 1, max = 100, message = "La referencia debe tener entre 1 y 100 caracteres")
    @Schema(description = "Referencia del recibo o factura a pagar", example = "25720381")
    private String referencia;

    @NotNull(message = "montoPago es requerido")
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a 0")
    @Schema(description = "Monto a pagar", example = "601.99")
    private BigDecimal montoPago;

    @Schema(description = "Teléfono de contacto (opcional: por defecto 1111111111 o #1111111111 según tipo)", example = "1111111111")
    private String telefono;

    @Size(min = 3, max = 15, message = "El UPC debe tener entre 3 y 15 caracteres alfanuméricos")
    @Schema(description = "Código único de transacción (opcional, se autogenera si se omite)", example = "UPC987654321")
    private String upc;

    @Size(min = 1, max = 25, message = "La unidad no debe exceder 25 caracteres")
    @Schema(description = "Sucursal o unidad comercial (opcional, por defecto el valor configurado)", example = "083")
    private String unidad;

    @Schema(description = "Número de cuenta bancaria local para debitar el saldo (opcional)", example = "8766090787749132")
    private String numeroCuenta;

    @Schema(description = "Si es true, simula la aprobación de GestoPago para pruebas locales sin depender de lista blanca de IP", example = "false")
    private Boolean simular;

    @Pattern(regexp = "^[0-9]{6}$", message = "El código 2FA debe ser de 6 dígitos numéricos")
    @Schema(description = "Código OTP de 6 dígitos de segundo factor (2FA) para autorizar la operación", example = "482910")
    private String codigo2FA;
}

