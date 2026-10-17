package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gestopago")
public class GestoPagoCatalogController {

    private final GestoPagoCatalogService catalogService;

    public GestoPagoCatalogController(GestoPagoCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/catalogo")
    public ResponseEntity<ApiResponse<List<GestoPagoCatalogProductResponse>>> obtenerCatalogo() {
        ResponseDTO<List<GestoPagoCatalogProductResponse>> response = catalogService.consultarCatalogo();
        if (!Integer.valueOf(0).equals(response.getCodigo()) || response.getData() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getMensaje()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response.getData()));
    }
}
