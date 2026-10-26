package com.proyecto.servicios.model.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Solicitud para venta de tiempo aire / recarga telefónica")
public class RecargaTelefonicaRequest {

    @NotNull(message = "idProducto es requerido")
    @Schema(description = "Identificador numérico del producto GestoPago", example = "582")
    private Integer idProducto;

    @NotNull(message = "idServicio es requerido")
    @Schema(description = "Identificador numérico del servicio GestoPago", example = "133")
    private Integer idServicio;

    @NotBlank(message = "telefono es requerido")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Número telefónico a recargar (10 dígitos)", example = "5555555551")
    private String telefono;

    @Schema(description = "Monto esperado de la recarga (opcional para validación contra catálogo)", example = "10.00")
    private BigDecimal monto;

    @Size(min = 3, max = 15, message = "El UPC debe tener entre 3 y 15 caracteres alfanuméricos")
    @Schema(description = "Código único de transacción (opcional, se autogenera si se omite)", example = "UPC123456789")
    private String upc;

    @Size(min = 1, max = 25, message = "La unidad no debe exceder 25 caracteres")
    @Schema(description = "Sucursal o unidad comercial (opcional, por defecto el valor configurado)", example = "083")
    private String unidad;
}
