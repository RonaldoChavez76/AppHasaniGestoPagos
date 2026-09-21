package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gestopago")
@RequiredArgsConstructor
public class GestoPagoCatalogController {

    private final GestoPagoCatalogService catalogService;

    @GetMapping("/catalogo")
    public ResponseEntity<ResponseDTO<List<GestoPagoCatalogProduct>>> obtenerCatalogo() {
        ResponseDTO<List<GestoPagoCatalogProduct>> response = catalogService.consultarCatalogo();
        return ResponseEntity.ok(response);
    }
}
