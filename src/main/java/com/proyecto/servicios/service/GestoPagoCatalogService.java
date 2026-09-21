package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.model.ResponseDTO;

import java.util.List;

public interface GestoPagoCatalogService {

    void refreshCatalogFromExternalSource();

    ResponseDTO<List<GestoPagoCatalogProduct>> consultarCatalogo();

    ResponseDTO<List<GestoPagoCatalogProduct>> consultarCatalogoDesdeBaseLocal();
}
