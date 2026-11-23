package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoCatalogServiceEnrichmentTest {

    @Test
    @SuppressWarnings("unchecked")
    void refreshCatalogDebePersistirProductosActivosConCamposEnriquecidos() {
        GestoPagoXmlProductsClient xmlClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository repository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-activo");
        Mockito.when(tokenService.obtenerTokenActivo(12, "DISP-01")).thenReturn(Optional.of(token));

        GestoPagoProductXmlItem item = new GestoPagoProductXmlItem();
        item.setIdProducto("14302");
        item.setProducto("ABIB 100");
        item.setServicio("ABIB");
        item.setIdServicio("99");
        item.setTipoFront("2");
        item.setPrecio(new BigDecimal("100.00"));
        item.setHasDigitoVerificador(false);
        item.setShowAyuda(false);
        item.setLegend("  Pago ABIB disponible  ");

        GestoPagoProductXmlResponse.GestoPagoProductosXml wrapper =
                new GestoPagoProductXmlResponse.GestoPagoProductosXml();
        wrapper.setProductos(List.of(item));

        GestoPagoProductXmlResponse xmlResponse = new GestoPagoProductXmlResponse();
        xmlResponse.setProductos(wrapper);

        Mockito.when(xmlClient.getProductListXml("Bearer token-activo", "api-key", "application/xml"))
                .thenReturn(xmlResponse);

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                xmlClient,
                repository,
                tokenService,
                redisTemplate,
                "api-key",
                12,
                "DISP-01"
        );

        service.refreshCatalogFromExternalSource();

        ArgumentCaptor<List<GestoPagoCatalogProduct>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(repository).saveAll(captor.capture());
        List<GestoPagoCatalogProduct> guardados = captor.getValue();

        assertEquals(1, guardados.size());
        GestoPagoCatalogProduct guardado = guardados.get(0);
        assertTrue(Boolean.TRUE.equals(guardado.getActive()));
        assertEquals("14302", guardado.getProductId());
        assertEquals("ABIB 100", guardado.getName());
        assertEquals("ABIB", guardado.getServicio());
        assertEquals("99", guardado.getIdServicio());
        assertEquals("2", guardado.getTipoFront());
        assertEquals("Pago ABIB disponible", guardado.getLegend());

        Mockito.verify(valueOps).set(
                Mockito.eq("catalogoGestopago::catalogo-v3"),
                Mockito.any(),
                Mockito.eq(Duration.ofMinutes(10))
        );
    }
}
