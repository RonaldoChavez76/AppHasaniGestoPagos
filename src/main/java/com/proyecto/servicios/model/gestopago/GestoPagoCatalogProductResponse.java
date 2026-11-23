package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GestoPagoCatalogProductResponse(
        Long id,
        String productId,
        String code,
        String name,
        BigDecimal price,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String servicio,
        String idServicio,
        String tipoFront,
        String legend
) {
    public GestoPagoCatalogProductResponse(
            Long id,
            String productId,
            String code,
            String name,
            BigDecimal price,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, productId, code, name, price, active, createdAt, updatedAt, null, null, null, null);
    }
}
