package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GestoPagoProductController {

    private final GestoPagoProductService productService;

    public GestoPagoProductController(GestoPagoProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GestoPagoProductListResponse>> obtenerProductos() {
        return ResponseEntity.ok(ApiResponse.success(
                200, "Productos consultados correctamente.", productService.obtenerProductos()));
    }
}