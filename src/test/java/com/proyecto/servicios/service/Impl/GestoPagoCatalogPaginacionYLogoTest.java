package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoLogoUrl;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestoPagoCatalogPaginacionYLogoTest {

    @Test
    void construyeLogoUrlOficialDeGestoPagoConIdServicio() {
        assertEquals(
                "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/79.png",
                GestoPagoLogoUrl.construir(null, " 79 ")
        );
        assertEquals(
                "https://cdn.ejemplo.com/servicios/133.png",
                GestoPagoLogoUrl.construir("https://cdn.ejemplo.com/servicios/", "133")
        );
        assertNull(GestoPagoLogoUrl.construir(null, "   "));
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoEnriqueceCatalogoSinIdServicioYResisteFalloExterno() {
        GestoPagoXmlProductsClient xmlClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository repository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get("catalogoGestopago::catalogo-v4")).thenReturn(null);

        GestoPagoCatalogProduct heredado = new GestoPagoCatalogProduct();
        heredado.setProductId("582");
        heredado.setName("Recarga Telcel $50");
        heredado.setActive(true);
        Mockito.when(repository.findAll()).thenReturn(List.of(heredado));

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("tok-1");
        Mockito.when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        GestoPagoProductXmlItem item = new GestoPagoProductXmlItem();
        item.setIdProducto("582");
        item.setProducto("Recarga Telcel $50");
        item.setServicio("Telcel");
        item.setIdServicio("133");
        item.setTipoFront("1");
        item.setPrecio(new BigDecimal("50.00"));
        GestoPagoProductXmlResponse.GestoPagoProductosXml wrapper =
                new GestoPagoProductXmlResponse.GestoPagoProductosXml();
        wrapper.setProductos(List.of(item));
        GestoPagoProductXmlResponse xmlResp = new GestoPagoProductXmlResponse();
        xmlResp.setProductos(wrapper);
        Mockito.when(xmlClient.getProductListXml("Bearer tok-1", "api-key", "application/xml"))
                .thenReturn(xmlResp);

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                xmlClient, repository, tokenService, redisTemplate, "api-key", 83, "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProductResponse>> resp = service.consultarCatalogo();
        assertEquals(0, resp.getCodigo());
        assertEquals(1, resp.getData().size());
        assertEquals("133", resp.getData().get(0).idServicio());
        assertEquals(
                "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/133.png",
                resp.getData().get(0).logoUrl()
        );
        Mockito.verify(repository).deleteAllInBatch();
        Mockito.verify(valueOps).set(
                Mockito.eq("catalogoGestopago::catalogo-v4"),
                Mockito.any(),
                Mockito.eq(Duration.ofMinutes(10))
        );

        Mockito.when(xmlClient.getProductListXml("Bearer tok-1", "api-key", "application/xml"))
                .thenThrow(new RuntimeException("Red caída"));
        ResponseDTO<List<GestoPagoCatalogProductResponse>> fallbackResp = service.consultarCatalogo();
        assertEquals(0, fallbackResp.getCodigo());
        assertEquals("Catálogo cargado desde PostgreSQL", fallbackResp.getMensaje());
        assertEquals(1, fallbackResp.getData().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void paginaCatalogoPorTamanoCategoriaBusquedaYDisponibilidad() {
        GestoPagoXmlProductsClient xmlClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository repository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);

        List<GestoPagoCatalogProduct> productos = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            GestoPagoCatalogProduct prod = new GestoPagoCatalogProduct();
            prod.setId((long) i);
            prod.setProductId(String.valueOf(100 + i));
            prod.setCode(String.valueOf(100 + i));
            prod.setName(i <= 15 ? "Recarga Telcel " + i : "Pago CFE " + i);
            prod.setServicio(i <= 15 ? "Telcel" : "CFE");
            prod.setIdServicio(i <= 15 ? "133" : "76");
            prod.setTipoFront(i <= 15 ? "1" : "2");
            prod.setPrice(new BigDecimal("50.00"));
            prod.setActive(i != 2);
            productos.add(prod);
        }

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get("catalogoGestopago::catalogo-v4"))
                .thenReturn(ResponseDTO.success(0, "Desde Redis", productos));

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                xmlClient, repository, tokenService, redisTemplate, "api-key", 83, "GPS83-TPV-17"
        );

        ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> pag1 =
                service.consultarCatalogoPaginado(0, 12, null, "Todos", false);
        assertEquals(0, pag1.getData().page());
        assertEquals(12, pag1.getData().content().size());
        assertEquals(25L, pag1.getData().totalElements());
        assertEquals(3, pag1.getData().totalPages());
        assertTrue(pag1.getData().first());
        assertFalse(pag1.getData().last());

        ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> pag3 =
                service.consultarCatalogoPaginado(2, 12, null, "Todos", false);
        assertEquals(1, pag3.getData().content().size());
        assertTrue(pag3.getData().last());

        ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> filtrado =
                service.consultarCatalogoPaginado(0, 12, "Telcel", "Recargas Telefonicas", true);
        assertEquals(14L, filtrado.getData().totalElements());
        assertEquals(2, filtrado.getData().totalPages());
        assertEquals(12, filtrado.getData().content().size());
    }
}
