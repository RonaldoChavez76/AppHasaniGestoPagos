package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gestopago")
public class GestoPagoCatalogController {

    private final GestoPagoCatalogService catalogService;

    public GestoPagoCatalogController(GestoPagoCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping({"/catalogo", "/catalogo/productos"})
    public ResponseEntity<ApiResponse<List<GestoPagoCatalogProductResponse>>> obtenerCatalogo() {
        ResponseDTO<List<GestoPagoCatalogProductResponse>> response = catalogService.consultarCatalogo();
        if (!Integer.valueOf(0).equals(response.getCodigo()) || response.getData() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getMensaje()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response.getData()));
    }

    @GetMapping({"/catalogo/paginado", "/catalogo/productos/paginado"})
    public ResponseEntity<ApiResponse<PageResponse<GestoPagoCatalogProductResponse>>> obtenerCatalogoPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean soloDisponibles
    ) {
        ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> response =
                catalogService.consultarCatalogoPaginado(page, size, busqueda, categoria, soloDisponibles);
        if (!Integer.valueOf(0).equals(response.getCodigo()) || response.getData() == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.failure(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getMensaje()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), response.getMensaje(), response.getData()));
    }
}
