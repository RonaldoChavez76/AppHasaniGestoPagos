package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoCatalogServiceImplTest {

    @Test
    void deberiaRetornarDatosLocalesCuandoElServicioExternoFalla() {
        GestoPagoXmlProductsClient productClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository productRepository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);

        GestoPagoCatalogProduct productoLocal = new GestoPagoCatalogProduct();
        productoLocal.setProductId("P-100");
        productoLocal.setCode("100");
        productoLocal.setName("Café");
        productoLocal.setPrice(new BigDecimal("120.00"));
        productoLocal.setActive(true);

        Mockito.when(productRepository.findAll()).thenReturn(List.of(productoLocal));

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                productClient,
                productRepository,
                tokenService,
                redisTemplate,
                "secret-key",
                83,
                "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProduct>> response = service.consultarCatalogoDesdeBaseLocal();

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertEquals("Respuesta desde caché local por fallo en servicio externo", response.getMensaje());
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void deberiaDeserializarXmlRealDeGestoPago() throws Exception {
        String xml = "<RESPONSE>"
                + "<MENSAJE><CODIGO>0</CODIGO><TEXTO>OK</TEXTO></MENSAJE>"
                + "<PRODUCTOS>"
                + "<producto servicio=\"Mensual\" producto=\"Recarga\" idServicio=\"1\" idProducto=\"100\" idCatTipoServicio=\"10\" tipoFront=\"1\" hasDigitoVerificador=\"true\" precio=\"120.50\" showAyuda=\"false\" tipoReferencia=\"N\">"
                + "<legend><![CDATA[Texto de ayuda 1]]></legend>"
                + "</producto>"
                + "<producto servicio=\"Mensual\" producto=\"Pago\" idServicio=\"2\" idProducto=\"200\" idCatTipoServicio=\"11\" tipoFront=\"2\" hasDigitoVerificador=\"false\" precio=\"89.90\" showAyuda=\"true\" tipoReferencia=\"X\">"
                + "<legend><![CDATA[Texto de ayuda 2]]></legend>"
                + "</producto>"
                + "</PRODUCTOS>"
                + "</RESPONSE>";

        JAXBContext context = JAXBContext.newInstance(GestoPagoProductXmlResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        GestoPagoProductXmlResponse response = (GestoPagoProductXmlResponse) unmarshaller.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );

        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("0", response.getMensaje().getCodigo());
        assertEquals(2, response.getItems().size());
        assertEquals("Recarga", response.getItems().get(0).getProducto());
        assertEquals("Texto de ayuda 2", response.getItems().get(1).getLegend());
    }

    @Test
    @SuppressWarnings("unchecked")
    void deberiaRetornarDeRedisCuandoExisteEnCache() {
        GestoPagoXmlProductsClient productClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository productRepository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);

        GestoPagoCatalogProduct producto = new GestoPagoCatalogProduct();
        producto.setProductId("100");
        producto.setName("Recarga");

        ResponseDTO<List<GestoPagoCatalogProduct>> cachedDto =
                ResponseDTO.success(0, "En cache", List.of(producto));

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get("catalogoGestopago::catalogo-v2")).thenReturn(cachedDto);

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                productClient, productRepository, tokenService, redisTemplate, "secret-key", 83, "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProduct>> resultado = service.consultarCatalogo();

        assertNotNull(resultado);
        assertEquals(0, resultado.getCodigo());
        assertEquals(1, resultado.getData().size());
        assertEquals("Recarga", resultado.getData().get(0).getName());
        Mockito.verifyNoInteractions(productRepository);
        Mockito.verifyNoInteractions(productClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deberiaConsultarPostgreSqlYGuardarEnRedisCuandoHayCacheMiss() {
        GestoPagoXmlProductsClient productClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository productRepository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);

        GestoPagoCatalogProduct producto = new GestoPagoCatalogProduct();
        producto.setProductId("200");
        producto.setName("Pago");

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get("catalogoGestopago::catalogo-v2")).thenReturn(null);
        Mockito.when(productRepository.findAll()).thenReturn(List.of(producto));

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                productClient, productRepository, tokenService, redisTemplate, "secret-key", 83, "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProduct>> resultado = service.consultarCatalogo();

        assertNotNull(resultado);
        assertEquals(0, resultado.getCodigo());
        assertEquals("Catálogo cargado desde PostgreSQL", resultado.getMensaje());
        assertEquals(1, resultado.getData().size());
        Mockito.verify(productRepository).findAll();
        Mockito.verify(valueOps).set(
                Mockito.eq("catalogoGestopago::catalogo-v2"),
                Mockito.any(),
                Mockito.eq(java.time.Duration.ofMinutes(10))
        );
        Mockito.verifyNoInteractions(productClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deberiaSerResilienteSiRedisFallaYConsultarPostgreSql() {
        GestoPagoXmlProductsClient productClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository productRepository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);

        GestoPagoCatalogProduct producto = new GestoPagoCatalogProduct();
        producto.setProductId("300");
        producto.setName("Servicio Luz");

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Mockito.when(valueOps.get(Mockito.anyString())).thenThrow(new RuntimeException("Redis connection error"));
        Mockito.when(productRepository.findAll()).thenReturn(List.of(producto));

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                productClient, productRepository, tokenService, redisTemplate, "secret-key", 83, "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProduct>> resultado = service.consultarCatalogo();

        assertNotNull(resultado);
        assertEquals(0, resultado.getCodigo());
        assertEquals("Catálogo cargado desde PostgreSQL", resultado.getMensaje());
        assertEquals(1, resultado.getData().size());
        Mockito.verify(productRepository).findAll();
    }
}
