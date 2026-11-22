package com.proyecto.servicios.model.gestopago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GestoPagoCatalogProductResponse(
        Long id,
        String productId,
        String code,
        String name,
        BigDecimal price,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
