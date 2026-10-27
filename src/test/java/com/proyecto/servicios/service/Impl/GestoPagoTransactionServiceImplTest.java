package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTransactionServiceImplTest {

    @Mock
    private GestoPagoTransactionClient transactionClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private GestoPagoCryptoService cryptoService;

    @Mock
    private GestoPagoCatalogProductRepository catalogProductRepository;

    @Mock
    private GestoPagoTransaccionRepository transaccionRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    private GestoPagoTransactionServiceImpl transactionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid-jwt-token");
        lenient().when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        lenient().when(cryptoService.encrypt(anyString())).thenReturn("mocked-encrypted-payload");

        lenient().when(transaccionRepository.save(any(GestoPagoTransaccion.class)))
                .thenAnswer(invocation -> {
                    GestoPagoTransaccion entidad = invocation.getArgument(0);
                    entidad.setId(1L);
                    entidad.setCreatedAt(LocalDateTime.now());
                    return entidad;
                });

        transactionService = new GestoPagoTransactionServiceImpl(
                transactionClient,
                tokenService,
                cryptoService,
                catalogProductRepository,
                transaccionRepository,
                cuentaRepository,
                objectMapper,
                83,
                "GPS83-TPV-17",
                "test-api-key",
                "083",
                false
        );
    }

    @Test
    void debeProcesarRecargaTelefonicaExitosamente() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .monto(new BigDecimal("10.00"))
                .upc("UPC_REC_001")
                .build();

        GestoPagoCatalogProduct catalogProduct = new GestoPagoCatalogProduct();
        catalogProduct.setProductId("582");
        catalogProduct.setName("Recarga Telcel 10");
        when(catalogProductRepository.findByProductId("582")).thenReturn(Optional.of(catalogProduct));

        GestoPagoSendTxXmlResponse xmlResponse = new GestoPagoSendTxXmlResponse();
        xmlResponse.setIdTx("291437188");
        xmlResponse.setNumAutorizacion("100024988");
        xmlResponse.setSaldo("157731.83");
        xmlResponse.setSaldoF("157,731.83");
        xmlResponse.setComision("0.7");
        xmlResponse.setComisionF("0.7");
        xmlResponse.setFecha("25/Feb/2022 18:15:25");
        xmlResponse.setMonto("10.0");

        GestoPagoSendTxXmlMensaje mensaje = new GestoPagoSendTxXmlMensaje();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        xmlResponse.setMensaje(mensaje);

        when(transactionClient.sendTxForm(eq("Bearer valid-jwt-token"), eq("test-api-key"), any(MultiValueMap.class)))
                .thenReturn(xmlResponse);

        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);

        assertNotNull(response);
        assertTrue(response.isExitoso());
        assertEquals(EstadoTransaccion.EXITOSA, response.getEstado());
        assertEquals("01", response.getCodigo());
        assertEquals("291437188", response.getIdTx());
        assertEquals("100024988", response.getNumAutorizacion());
        assertEquals("UPC_REC_001", response.getUpc());
        assertEquals(TipoOperacionGestoPago.RECARGA, response.getTipoOperacion());

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(transaccionRepository).save(captor.capture());
        GestoPagoTransaccion saved = captor.getValue();
        assertEquals("5555555551", saved.getTelefono());
        assertEquals("EXITOSA", saved.getEstado());
        assertEquals("RECARGA", saved.getTipoOperacion());
    }

    @Test
    void debeValidarYDescontarSaldoDeCuentaAlSerExitosa() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(3L);
        cuenta.setNumeroCuenta("8766090787749132");
        cuenta.setSaldo(new BigDecimal("5000.00"));
        cuenta.setEstatus("ACTIVA");

        when(cuentaRepository.findByNumeroCuenta("8766090787749132")).thenReturn(Optional.of(cuenta));

        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("10.00"))
                .numeroCuenta("8766090787749132")
                .simular(true)
                .build();

        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);

        assertNotNull(response);
        assertTrue(response.isExitoso());
        assertEquals(EstadoTransaccion.EXITOSA, response.getEstado());
        assertEquals("8766090787749132", response.getNumeroCuenta());
        assertEquals(new BigDecimal("4990.00"), response.getNuevoSaldoCuenta());
        assertEquals(new BigDecimal("4990.00"), cuenta.getSaldo());

        verify(cuentaRepository).save(cuenta);
    }

    @Test
    void debeRechazarTransaccionSiSaldoEnCuentaEsInsuficiente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(3L);
        cuenta.setNumeroCuenta("8766090787749132");
        cuenta.setSaldo(new BigDecimal("5.00")); // Saldo de 5 pesos
        cuenta.setEstatus("ACTIVA");

        when(cuentaRepository.findByNumeroCuenta("8766090787749132")).thenReturn(Optional.of(cuenta));

        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("10.00")) // Requiere 10 pesos
                .numeroCuenta("8766090787749132")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                transactionService.procesarRecarga(request));

        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        verifyNoInteractions(transactionClient);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void debeProcesarPagoServicioExitosamente() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .idProducto(205)
                .idServicio(76)
                .referencia("25720381")
                .montoPago(new BigDecimal("601.99"))
                .upc("UPC_SER_001")
                .build();

        GestoPagoSendTxXmlResponse xmlResponse = new GestoPagoSendTxXmlResponse();
        xmlResponse.setIdTx("291620941");
        xmlResponse.setNumAutorizacion("100074211");
        xmlResponse.setSaldo("154997.91");
        xmlResponse.setSaldoF("154,997.91");
        xmlResponse.setComision("6.0");
        xmlResponse.setComisionF("6");
        xmlResponse.setFecha("25/Feb/2022 18:59:34");
        xmlResponse.setMonto("601.99");

        GestoPagoSendTxXmlMensaje mensaje = new GestoPagoSendTxXmlMensaje();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        mensaje.setReferencia("25720381");
        xmlResponse.setMensaje(mensaje);

        when(transactionClient.sendTxForm(eq("Bearer valid-jwt-token"), eq("test-api-key"), any(MultiValueMap.class)))
                .thenReturn(xmlResponse);

        GestoPagoTransaccionResponse response = transactionService.procesarPagoServicio(request);

        assertNotNull(response);
        assertTrue(response.isExitoso());
        assertEquals(EstadoTransaccion.EXITOSA, response.getEstado());
        assertEquals("25720381", response.getReferencia());
        assertEquals("100074211", response.getNumAutorizacion());
        assertEquals(TipoOperacionGestoPago.PAGO_SERVICIO, response.getTipoOperacion());

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(transaccionRepository).save(captor.capture());
        GestoPagoTransaccion saved = captor.getValue();
        assertEquals("1111111111", saved.getTelefono());
        assertEquals("25720381", saved.getReferencia());
        assertEquals("PAGO_SERVICIO", saved.getTipoOperacion());
    }

    @Test
    void debeRechazarRecargaDuplicadaEnVentanaDe15Minutos() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .upc("UPC_DUPLICADO")
                .build();

        GestoPagoTransaccion txExistente = new GestoPagoTransaccion();
        txExistente.setUpc("UPC_DUPLICADO");
        txExistente.setEstado("EXITOSA");
        txExistente.setNumAutorizacion("100024988");
        when(transaccionRepository.findTopByUpcAndCreatedAtAfterOrderByCreatedAtDesc(eq("UPC_DUPLICADO"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(txExistente));

        GestoPagoIntegrationException exception = assertThrows(
                GestoPagoIntegrationException.class,
                () -> transactionService.procesarRecarga(request)
        );

        assertTrue(exception.getMessage().contains("Transacción duplicada"));
        verifyNoInteractions(transactionClient);
    }

    @Test
    void debeManejarRespuestaDeTransaccionDuplicadaDeGestoPago() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .upc("UPC_PROV_DUP")
                .build();

        GestoPagoSendTxXmlResponse xmlResponse = new GestoPagoSendTxXmlResponse();
        xmlResponse.setIdTx("291437188");
        xmlResponse.setNumAutorizacion("100024988");

        GestoPagoSendTxXmlMensaje mensaje = new GestoPagoSendTxXmlMensaje();
        mensaje.setCodigo("06");
        mensaje.setTexto("Transaccion aplicada anteriormente Intentar dentro de 15 minutos.");
        xmlResponse.setMensaje(mensaje);

        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenReturn(xmlResponse);

        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);

        assertNotNull(response);
        assertFalse(response.isExitoso());
        assertEquals(EstadoTransaccion.DUPLICADA, response.getEstado());
        assertEquals("06", response.getCodigo());
        assertTrue(response.getMensaje().contains("Transaccion aplicada anteriormente"));
    }

    @Test
    void debeManejarTimeoutDeLecturaConEstadoTimeout() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .upc("UPC_TIMEOUT")
                .build();

        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost", Collections.emptyMap(), null, null, null);
        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenThrow(new RetryableException(504, "Read timed out", Request.HttpMethod.POST, new Date(), feignRequest));

        GestoPagoTransaccionResponse response = transactionService.procesarRecarga(request);

        assertNotNull(response);
        assertFalse(response.isExitoso());
        assertEquals(EstadoTransaccion.TIMEOUT, response.getEstado());

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(transaccionRepository).save(captor.capture());
        assertEquals("TIMEOUT", captor.getValue().getEstado());
    }

    @Test
    void debeProcesarTransaccionUnificadaSegunBanderaEsServicio() {
        TransaccionGestoPagoRequest requestServicio = TransaccionGestoPagoRequest.builder()
                .esServicio(true)
                .idProducto(205)
                .idServicio(76)
                .referencia("REF-1234")
                .montoPago(new BigDecimal("150.00"))
                .upc("UPC_UNIF_01")
                .build();

        GestoPagoSendTxXmlResponse xmlResponse = new GestoPagoSendTxXmlResponse();
        GestoPagoSendTxXmlMensaje mensaje = new GestoPagoSendTxXmlMensaje();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito");
        xmlResponse.setMensaje(mensaje);
        xmlResponse.setNumAutorizacion("99999");
        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenReturn(xmlResponse);

        GestoPagoTransaccionResponse response = transactionService.procesarTransaccion(requestServicio);
        assertNotNull(response);
        assertEquals(TipoOperacionGestoPago.PAGO_SERVICIO, response.getTipoOperacion());
    }

    @Test
    void debeConsultarTransaccionPorUpc() {
        GestoPagoTransaccion entidad = new GestoPagoTransaccion();
        entidad.setUpc("UPC_CONSULTA");
        entidad.setTipoOperacion("RECARGA");
        entidad.setEstado("EXITOSA");
        entidad.setNumAutorizacion("AUTH123");
        entidad.setCodigoRespuesta("01");
        when(transaccionRepository.findByUpc("UPC_CONSULTA")).thenReturn(Optional.of(entidad));

        Optional<GestoPagoTransaccionResponse> resultado = transactionService.consultarTransaccionPorUpc("UPC_CONSULTA");
        assertTrue(resultado.isPresent());
        assertEquals("AUTH123", resultado.get().getNumAutorizacion());
        assertTrue(resultado.get().isExitoso());
    }
}
