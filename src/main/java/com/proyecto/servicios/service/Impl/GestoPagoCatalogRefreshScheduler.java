package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.GestoPagoCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GestoPagoCatalogRefreshScheduler {

    private final GestoPagoCatalogService catalogService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshCatalog() {
        log.info("Ejecutando tarea programada de actualización del catálogo GestoPago");
        catalogService.refreshCatalogFromExternalSource();
    }
}
