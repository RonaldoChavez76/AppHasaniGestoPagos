package com.proyecto.servicios.service;

import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;

import java.util.List;

public interface GestoPagoCatalogService {

    void refreshCatalogFromExternalSource();

    ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogo();

    ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogoDesdeBaseLocal();
}
