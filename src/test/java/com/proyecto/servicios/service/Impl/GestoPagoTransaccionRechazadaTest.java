package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.gestopago.EstadoTransaccion;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlMensaje;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.RecargaTelefonicaRequest;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import feign.FeignException;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoTransaccionRechazadaTest {

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
    private GestoPagoTransaccionRegistroService registroService;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    private GestoPagoTransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("valid-jwt");
        lenient().when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        lenient().when(cryptoService.encrypt(anyString())).thenReturn("encrypted");

        lenient().when(registroService.guardarEnTransaccionIndependiente(any(GestoPagoTransaccion.class)))
                .thenAnswer(inv -> {
                    GestoPagoTransaccion ent = inv.getArgument(0);
                    ent.setId(100L);
                    ent.setCreatedAt(LocalDateTime.now());
                    return ent;
                });

        lenient().when(transaccionRepository.save(any(GestoPagoTransaccion.class)))
                .thenAnswer(inv -> {
                    GestoPagoTransaccion ent = inv.getArgument(0);
                    ent.setId(101L);
                    ent.setCreatedAt(LocalDateTime.now());
                    return ent;
                });

        service = new GestoPagoTransactionServiceImpl(
                transactionClient,
                tokenService,
                cryptoService,
                catalogProductRepository,
                transaccionRepository,
                registroService,
                cuentaRepository,
                twoFactorAuthService,
                new ObjectMapper(),
                83,
                "GPS83-TPV-17",
                "api-key",
                "083",
                false,
                true
        );
    }

    @Test
    void persisteRechazoEnTransaccionIndependienteCuandoSaldoEsInsuficiente() {
        Cuenta cuenta = cuenta("4229215075268914", "ACTIVA", new BigDecimal("20.00"));
        when(cuentaRepository.findByNumeroCuentaForUpdate("4229215075268914")).thenReturn(Optional.of(cuenta));

        RecargaTelefonicaRequest req = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("50.00"))
                .numeroCuenta("4229215075268914")
                .codigo2FA("123456")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.procesarRecarga(req));

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(registroService).guardarEnTransaccionIndependiente(captor.capture());
        GestoPagoTransaccion guardada = captor.getValue();
        assertEquals("RECHAZADA", guardada.getEstado());
        assertEquals("SALDO_INS", guardada.getCodigoRespuesta());
        assertTrue(guardada.getMotivoRechazo().contains("Saldo insuficiente"));
        assertEquals("4229215075268914", guardada.getNumeroCuenta());
        verifyNoInteractions(transactionClient);
    }

    @Test
    void persisteRechazoCuandoFaltaOFallaCodigo2fa() {
        Cuenta cuenta = cuenta("4229215075268914", "ACTIVA", new BigDecimal("500.00"));
        when(cuentaRepository.findByNumeroCuentaForUpdate("4229215075268914")).thenReturn(Optional.of(cuenta));

        PagoServicioRequest sinCodigo = PagoServicioRequest.builder()
                .idProducto(205)
                .idServicio(76)
                .referencia("987654321")
                .montoPago(new BigDecimal("100.00"))
                .numeroCuenta("4229215075268914")
                .codigo2FA("")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.procesarPagoServicio(sinCodigo));

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(registroService).guardarEnTransaccionIndependiente(captor.capture());
        assertEquals("2FA_REQ", captor.getValue().getCodigoRespuesta());
        assertEquals("RECHAZADA", captor.getValue().getEstado());
        assertTrue(captor.getValue().getMotivoRechazo().contains("2FA"));

        PagoServicioRequest codigoInvalido = PagoServicioRequest.builder()
                .idProducto(205)
                .idServicio(76)
                .referencia("987654321")
                .montoPago(new BigDecimal("100.00"))
                .numeroCuenta("4229215075268914")
                .codigo2FA("000000")
                .build();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Código 2FA incorrecto."))
                .when(twoFactorAuthService).validarYConsumirCodigo("4229215075268914", "000000");

        assertThrows(IllegalArgumentException.class, () -> service.procesarPagoServicio(codigoInvalido));
    }

    @Test
    void persisteRechazoCuandoCuentaEstaInactiva() {
        Cuenta inactiva = cuenta("4229215075268914", "INACTIVA", new BigDecimal("500.00"));
        when(cuentaRepository.findByNumeroCuentaForUpdate("4229215075268914")).thenReturn(Optional.of(inactiva));

        RecargaTelefonicaRequest req = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("20.00"))
                .numeroCuenta("4229215075268914")
                .codigo2FA("123456")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.procesarRecarga(req));

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(registroService).guardarEnTransaccionIndependiente(captor.capture());
        assertEquals("CTA_INACT", captor.getValue().getCodigoRespuesta());
        assertTrue(captor.getValue().getMotivoRechazo().contains("no se encuentra activa"));
    }

    @Test
    void persisteRechazoDuplicadoConUpcAlternativoSinColisionDeUnicidad() {
        GestoPagoTransaccion existente = new GestoPagoTransaccion();
        existente.setUpc("UPC_REP_001");
        existente.setEstado("EXITOSA");
        existente.setNumAutorizacion("AUTH999");

        when(transaccionRepository.findTopByUpcAndCreatedAtAfterOrderByCreatedAtDesc(
                eq("UPC_REP_001"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existente));
        when(transaccionRepository.existsByUpc("UPC_REP_001")).thenReturn(true);

        RecargaTelefonicaRequest req = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("50.00"))
                .upc("UPC_REP_001")
                .build();

        assertThrows(GestoPagoIntegrationException.class, () -> service.procesarRecarga(req));

        ArgumentCaptor<GestoPagoTransaccion> captor = ArgumentCaptor.forClass(GestoPagoTransaccion.class);
        verify(registroService).guardarEnTransaccionIndependiente(captor.capture());
        GestoPagoTransaccion auditada = captor.getValue();
        assertEquals("DUPLICADA", auditada.getEstado());
        assertEquals("DUP_UPC", auditada.getCodigoRespuesta());
        assertNotEquals("UPC_REP_001", auditada.getUpc());
        assertTrue(auditada.getUpc().startsWith("RJ"));
        assertTrue(auditada.getMotivoRechazo().contains("Transacción duplicada"));
    }

    @Test
    void guardaMotivoEspecificoEnRechazoXmlTimeoutYErrorHttpDeGestoPago() {
        GestoPagoSendTxXmlResponse xmlRechazo = new GestoPagoSendTxXmlResponse();
        GestoPagoSendTxXmlMensaje msg = new GestoPagoSendTxXmlMensaje();
        msg.setCodigo("05");
        msg.setTexto("Teléfono no válido para compañía");
        xmlRechazo.setMensaje(msg);

        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenReturn(xmlRechazo);

        GestoPagoTransaccionResponse respXml = service.procesarRecarga(RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .upc("UPC_XML_RECH")
                .build());

        assertFalse(respXml.isExitoso());
        assertEquals(EstadoTransaccion.RECHAZADA, respXml.getEstado());
        assertEquals(
                "Rechazo de GestoPago (código 05): Teléfono no válido para compañía",
                respXml.getMotivoRechazo());

        Request feignReq = Request.create(Request.HttpMethod.POST, "https://gestopago", Collections.emptyMap(), null, null, null);
        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenThrow(new RetryableException(504, "Read timed out", Request.HttpMethod.POST, new Date(), feignReq));

        GestoPagoTransaccionResponse respTimeout = service.procesarRecarga(RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .upc("UPC_TO_RECH")
                .build());

        assertEquals(EstadoTransaccion.TIMEOUT, respTimeout.getEstado());
        assertTrue(respTimeout.getMotivoRechazo().contains("Tiempo de espera agotado"));

        when(transactionClient.sendTxForm(anyString(), anyString(), any(MultiValueMap.class)))
                .thenThrow(new FeignException.BadGateway("Bad Gateway", feignReq, "upstream".getBytes(StandardCharsets.UTF_8), Collections.emptyMap()));

        GestoPagoTransaccionResponse respHttp = service.procesarRecarga(RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .upc("UPC_HTTP_502")
                .build());

        assertEquals(EstadoTransaccion.ERROR, respHttp.getEstado());
        assertEquals("HTTP_502", respHttp.getCodigo());
        assertTrue(respHttp.getMotivoRechazo().contains("Error HTTP 502"));
    }

    @Test
    void listaHistorialCompletoYRechazadasConSuMotivoPorCuenta() {
        GestoPagoTransaccion exitosa = new GestoPagoTransaccion();
        exitosa.setUpc("TX_EX_1");
        exitosa.setTipoOperacion("RECARGA");
        exitosa.setEstado("EXITOSA");
        exitosa.setNumeroCuenta("4229215075268914");
        exitosa.setMontoPago(new BigDecimal("50.00"));

        GestoPagoTransaccion rechazada = new GestoPagoTransaccion();
        rechazada.setUpc("RJ_001");
        rechazada.setTipoOperacion("PAGO_SERVICIO");
        rechazada.setEstado("RECHAZADA");
        rechazada.setCodigoRespuesta("SALDO_INS");
        rechazada.setMotivoRechazo("Saldo insuficiente en la cuenta 4229215075268914.");
        rechazada.setNumeroCuenta("4229215075268914");
        rechazada.setMontoPago(new BigDecimal("300.00"));

        when(transaccionRepository.findByNumeroCuentaOrderByCreatedAtDesc("4229215075268914"))
                .thenReturn(List.of(rechazada, exitosa));
        when(transaccionRepository.findByNumeroCuentaAndEstadoNotOrderByCreatedAtDesc("4229215075268914", "EXITOSA"))
                .thenReturn(List.of(rechazada));

        List<GestoPagoTransaccionResponse> completo =
                service.listarHistorialCompletoPorUsuario(null, "4229215075268914");
        assertEquals(2, completo.size());
        assertEquals("Saldo insuficiente en la cuenta 4229215075268914.", completo.get(0).getMotivoRechazo());

        List<GestoPagoTransaccionResponse> rechazadas =
                service.listarComprasRechazadasPorUsuario(null, "4229215075268914");
        assertEquals(1, rechazadas.size());
        assertEquals("RJ_001", rechazadas.get(0).getUpc());
        assertEquals("Saldo insuficiente en la cuenta 4229215075268914.", rechazadas.get(0).getMotivoRechazo());
    }

    private Cuenta cuenta(String numero, String estatus, BigDecimal saldo) {
        Cuenta c = new Cuenta();
        c.setId(1L);
        c.setNumeroCuenta(numero);
        c.setEstatus(estatus);
        c.setSaldo(saldo);
        return c;
    }
}
