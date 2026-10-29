package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.service.GestoPagoTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GestoPagoTransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GestoPagoTransactionService transactionService;

    @InjectMocks
    private GestoPagoTransactionController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debeRetornar200AlProcesarRecargaValida() throws Exception {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .monto(new BigDecimal("10.00"))
                .upc("UPC12345")
                .build();

        GestoPagoTransaccionResponse mockResponse = GestoPagoTransaccionResponse.builder()
                .upc("UPC12345")
                .tipoOperacion(TipoOperacionGestoPago.RECARGA)
                .exitoso(true)
                .estado(EstadoTransaccion.EXITOSA)
                .codigo("01")
                .mensaje("Operacion realizada con exito")
                .idTx("291437188")
                .numAutorizacion("100024988")
                .monto(new BigDecimal("10.00"))
                .build();

        when(transactionService.procesarRecarga(any(RecargaTelefonicaRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/gestopago/transacciones/recarga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.upc").value("UPC12345"))
                .andExpect(jsonPath("$.data.exitoso").value(true))
                .andExpect(jsonPath("$.data.idTx").value("291437188"))
                .andExpect(jsonPath("$.data.numAutorizacion").value("100024988"));
    }

    @Test
    void debeRetornar200AlProcesarPagoServicioValido() throws Exception {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .idProducto(205)
                .idServicio(76)
                .referencia("25720381")
                .montoPago(new BigDecimal("601.99"))
                .upc("UPC98765")
                .build();

        GestoPagoTransaccionResponse mockResponse = GestoPagoTransaccionResponse.builder()
                .upc("UPC98765")
                .tipoOperacion(TipoOperacionGestoPago.PAGO_SERVICIO)
                .exitoso(true)
                .estado(EstadoTransaccion.EXITOSA)
                .codigo("01")
                .mensaje("Operacion realizada con exito")
                .idTx("291620941")
                .numAutorizacion("100074211")
                .referencia("25720381")
                .monto(new BigDecimal("601.99"))
                .build();

        when(transactionService.procesarPagoServicio(any(PagoServicioRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/gestopago/transacciones/servicio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.upc").value("UPC98765"))
                .andExpect(jsonPath("$.data.referencia").value("25720381"))
                .andExpect(jsonPath("$.data.tipoOperacion").value("PAGO_SERVICIO"));
    }

    @Test
    void debeRetornar400CuandoFaltanParametrosEnRecarga() throws Exception {
        RecargaTelefonicaRequest requestInvalido = RecargaTelefonicaRequest.builder()
                .idProducto(null)
                .telefono("123") // Menor a 10 dígitos
                .build();

        mockMvc.perform(post("/api/gestopago/transacciones/recarga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void debeConsultarTransaccionPorUpc() throws Exception {
        GestoPagoTransaccionResponse mockResponse = GestoPagoTransaccionResponse.builder()
                .upc("UPC_EXISTENTE")
                .tipoOperacion(TipoOperacionGestoPago.RECARGA)
                .exitoso(true)
                .estado(EstadoTransaccion.EXITOSA)
                .numAutorizacion("AUTH12345")
                .build();

        when(transactionService.consultarTransaccionPorUpc("UPC_EXISTENTE"))
                .thenReturn(Optional.of(mockResponse));

        mockMvc.perform(get("/api/gestopago/transacciones/UPC_EXISTENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upc").value("UPC_EXISTENTE"))
                .andExpect(jsonPath("$.data.numAutorizacion").value("AUTH12345"));
    }

    @Test
    void debeRetornar404SiUpcNoExiste() throws Exception {
        when(transactionService.consultarTransaccionPorUpc("UPC_INEXISTENTE"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/gestopago/transacciones/UPC_INEXISTENTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeListarHistorialDeTransacciones() throws Exception {
        GestoPagoTransaccionResponse item = GestoPagoTransaccionResponse.builder()
                .upc("UPC_HIST")
                .tipoOperacion(TipoOperacionGestoPago.RECARGA)
                .exitoso(true)
                .build();

        when(transactionService.listarUltimasTransacciones())
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/gestopago/transacciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].upc").value("UPC_HIST"));
    }
}
