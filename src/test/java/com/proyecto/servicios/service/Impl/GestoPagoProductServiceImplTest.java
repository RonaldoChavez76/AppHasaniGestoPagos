package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoProductsClient productsClient;

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
        GestoPagoProductListResponse expected = new GestoPagoProductListResponse();
        when(productsClient.getProductList(anyString(), anyString(), anyString())).thenReturn(expected);

        GestoPagoProductListResponse actual = productService.obtenerProductos();

        ArgumentCaptor<String> authorization = ArgumentCaptor.forClass(String.class);
        verify(productsClient).getProductList(authorization.capture(), org.mockito.ArgumentMatchers.eq("configured-api-key"), org.mockito.ArgumentMatchers.eq("application/json"));
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
        assertEquals("Bearer configured-token", authorization.getValue());
    }

    @Test
    void debeTraducirErrorDeAutenticacion() {
        FeignException exception = FeignException.errorStatus("getProductList", Response.builder()
                .status(401)
                .request(Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                        Map.of(), null, StandardCharsets.UTF_8, null))
                .build());
        when(productsClient.getProductList(anyString(), anyString(), anyString())).thenThrow(exception);

        GestoPagoIntegrationException actual = assertThrows(
            GestoPagoIntegrationException.class, () -> productService.obtenerProductos());

        assertEquals(401, actual.getStatus().value());
    }

    @Test
        void debeTraducirTimeout() {
        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
            Map.of(), null, StandardCharsets.UTF_8, null);
        when(productsClient.getProductList(anyString(), anyString(), anyString())).thenThrow(new RetryableException(
            0, "timeout", Request.HttpMethod.GET, new RuntimeException("timeout"), (Long) null, request));

        GestoPagoIntegrationException actual = assertThrows(
            GestoPagoIntegrationException.class, () -> productService.obtenerProductos());

        assertEquals(504, actual.getStatus().value());
    }
}