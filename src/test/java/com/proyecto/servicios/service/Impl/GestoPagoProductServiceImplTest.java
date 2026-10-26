package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoXmlProductsClient productsClient;

    @Mock
    private GestoPagoTokenService tokenService;

    private GestoPagoProductServiceImpl productService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("configured-token");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        productService = new GestoPagoProductServiceImpl(
            productsClient, tokenService, 83, "GPS83-TPV-17", "configured-api-key");
    }

    @Test
    void debeConsultarProductosConBearerToken() {
        when(productsClient.getProductListXml(anyString(), anyString(), anyString()))
            .thenReturn(new GestoPagoProductXmlResponse());

        GestoPagoProductListResponse actual = productService.obtenerProductos();

        ArgumentCaptor<String> authorization = ArgumentCaptor.forClass(String.class);
        verify(productsClient).getProductListXml(authorization.capture(), org.mockito.ArgumentMatchers.eq("configured-api-key"), org.mockito.ArgumentMatchers.eq("application/xml"));
        assertNotNull(actual);
        assertEquals("Bearer configured-token", authorization.getValue());
    }

    @Test
    void debeTraducirErrorDeAutenticacion() {
        FeignException exception = FeignException.errorStatus("getProductList", Response.builder()
                .status(401)
                .request(Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                        Map.of(), null, StandardCharsets.UTF_8, null))
                .build());
        when(productsClient.getProductListXml(anyString(), anyString(), anyString())).thenThrow(exception);

        GestoPagoIntegrationException actual = assertThrows(
            GestoPagoIntegrationException.class, () -> productService.obtenerProductos());

        assertEquals(401, actual.getStatus().value());
    }

    @Test
    void debeTraducirTimeout() {
        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
            Map.of(), null, StandardCharsets.UTF_8, null);
        when(productsClient.getProductListXml(anyString(), anyString(), anyString())).thenThrow(new RetryableException(
            0, "timeout", Request.HttpMethod.GET, new RuntimeException("timeout"), (Long) null, request));

        GestoPagoIntegrationException actual = assertThrows(
            GestoPagoIntegrationException.class, () -> productService.obtenerProductos());

        assertEquals(504, actual.getStatus().value());
    }

    @Test
    void debeMapearTodosLosCamposDetalladosDelXmlDeGestoPago() {
        GestoPagoProductXmlResponse xmlResponse = new GestoPagoProductXmlResponse();
        GestoPagoProductXmlResponse.GestoPagoMensajeXml mensaje = new GestoPagoProductXmlResponse.GestoPagoMensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        xmlResponse.setMensaje(mensaje);

        GestoPagoProductXmlResponse.GestoPagoProductosXml productosXml = new GestoPagoProductXmlResponse.GestoPagoProductosXml();
        com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem item = new com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem();
        item.setServicio("AGUAKAN (Cancun)");
        item.setProducto("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)");
        item.setIdServicio("56");
        item.setIdProducto("185");
        item.setIdCatTipoServicio("15");
        item.setTipoFront("2");
        item.setHasDigitoVerificador(false);
        item.setPrecio(new java.math.BigDecimal("10.0"));
        item.setShowAyuda(false);
        item.setTipoReferencia("c");
        item.setLegend("Para cualquier duda o aclaracion con tu pago...");
        productosXml.setProductos(java.util.List.of(item));
        xmlResponse.setProductos(productosXml);

        when(productsClient.getProductListXml(anyString(), anyString(), anyString())).thenReturn(xmlResponse);

        GestoPagoProductListResponse actual = productService.obtenerProductos();

        assertNotNull(actual);
        assertEquals(1, actual.getCode());
        assertEquals("Operacion realizada con exito", actual.getMessage());
        assertNotNull(actual.getData());
        assertEquals(1, actual.getData().size());

        com.fasterxml.jackson.databind.JsonNode productoJson = actual.getData().get(0);
        assertEquals("AGUAKAN (Cancun)", productoJson.get("servicio").asText());
        assertEquals("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)", productoJson.get("producto").asText());
        assertEquals("56", productoJson.get("idServicio").asText());
        assertEquals("185", productoJson.get("idProducto").asText());
        assertEquals("15", productoJson.get("idCatTipoServicio").asText());
        assertEquals("2", productoJson.get("tipoFront").asText());
        org.junit.jupiter.api.Assertions.assertFalse(productoJson.get("hasDigitoVerificador").asBoolean());
        assertEquals("c", productoJson.get("tipoReferencia").asText());
        assertEquals(10.0, productoJson.get("precio").asDouble());
        org.junit.jupiter.api.Assertions.assertFalse(productoJson.get("showAyuda").asBoolean());
        assertEquals("Para cualquier duda o aclaracion con tu pago...", productoJson.get("legend").asText());
        assertEquals("185", productoJson.get("id").asText());
        assertEquals("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)", productoJson.get("name").asText());
        org.junit.jupiter.api.Assertions.assertTrue(productoJson.get("active").asBoolean());
    }
}