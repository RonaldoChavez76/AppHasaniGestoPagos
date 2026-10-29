package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoProductDto {

    // Identificadores y nombres estándar para compatibilidad
    private String id;
    private String code;
    private String name;
    private BigDecimal price;
    private Boolean active;

    // Campos detallados del contrato XML de GestoPago
    private String servicio;
    private String producto;
    private String idServicio;
    private String idProducto;
    private String idCatTipoServicio;
    private String tipoFront;
    private Boolean hasDigitoVerificador;
    private String tipoReferencia;
    private BigDecimal precio;
    private Boolean showAyuda;
    private String legend;
}