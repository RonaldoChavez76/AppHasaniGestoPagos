package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
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

    @Test
    void respondeCatalogoPaginadoConLogoUrlYMetadatosDePagina() throws Exception {
        GestoPagoCatalogProductResponse item = new GestoPagoCatalogProductResponse(
                10L,
                "582",
                "582",
                "Recarga Telcel $50",
                new java.math.BigDecimal("50.00"),
                true,
                null,
                null,
                "Telcel",
                "133",
                "1",
                "Recarga inmediata"
        );
        PageResponse<GestoPagoCatalogProductResponse> pageResponse =
                new PageResponse<>(List.of(item), 1, 12, 25L, 3, false, false);
        when(catalogService.consultarCatalogoPaginado(1, 12, "Telcel", "Recargas Telefónicas", true))
                .thenReturn(ResponseDTO.success(0, "Catálogo cargado desde Redis", pageResponse));

        mockMvc.perform(get("/api/gestopago/catalogo/paginado")
                        .param("page", "1")
                        .param("size", "12")
                        .param("busqueda", "Telcel")
                        .param("categoria", "Recargas Telefónicas")
                        .param("soloDisponibles", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.content[0].logoUrl")
                        .value("https://gestopago.portalventas.net/sistema/images/gestopago/servicios/133.png"));
    }
}
