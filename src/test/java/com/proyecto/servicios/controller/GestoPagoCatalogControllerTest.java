package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GestoPagoCatalogControllerTest {

    private final GestoPagoCatalogService catalogService = mock(GestoPagoCatalogService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GestoPagoCatalogController(catalogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void respondeConEnvoltorioEstandarYDtoDeProducto() throws Exception {
        when(catalogService.consultarCatalogo()).thenReturn(
                ResponseDTO.success(0, "Catálogo cargado desde PostgreSQL", List.of(
                        new GestoPagoCatalogProductResponse(
                                4L, "product-4", "code-4", "Servicio",
                                new java.math.BigDecimal("12.50"), true, null, null))));

        mockMvc.perform(get("/api/gestopago/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Catálogo cargado desde PostgreSQL"))
                .andExpect(jsonPath("$.data[0].id").value(4))
                .andExpect(jsonPath("$.data[0].productId").value("product-4"))
                .andExpect(jsonPath("$.data[0].name").value("Servicio"))
                .andExpect(jsonPath("$.data[0].price").value(12.5))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void informaIndisponibilidadEnElMismoEnvoltorioCuandoNoHayCatalogo() throws Exception {
        when(catalogService.consultarCatalogo()).thenReturn(
                ResponseDTO.failure(1, "No hay información disponible localmente ni en servicio externo"));

        mockMvc.perform(get("/api/gestopago/catalogo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "No hay información disponible localmente ni en servicio externo"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
