package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoProductService;
import com.proyecto.servicios.service.Impl.GestoPagoIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductControllerTest {

    @Mock
    private GestoPagoProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GestoPagoProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debeRetornarProductos() throws Exception {
        when(productService.obtenerProductos()).thenReturn(new GestoPagoProductListResponse());

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void debeMapearErrorDeAutenticacion() throws Exception {
        when(productService.obtenerProductos()).thenThrow(new GestoPagoIntegrationException(
                "La autenticación con GestoPago fue rechazada", HttpStatus.UNAUTHORIZED, new RuntimeException()));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}