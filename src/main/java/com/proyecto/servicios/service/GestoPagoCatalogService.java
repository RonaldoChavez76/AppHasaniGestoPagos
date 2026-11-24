package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;

import java.util.List;

public interface GestoPagoCatalogService {

    void refreshCatalogFromExternalSource();

    ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogo();

    ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> consultarCatalogoPaginado(
            int page,
            int size,
            String busqueda,
            String categoria,
            Boolean soloDisponibles
    );

    ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogoDesdeBaseLocal();
}
