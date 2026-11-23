package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.gestopago.EstadoTransaccion;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.gestopago.PagoServicioRequest;
import com.proyecto.servicios.model.gestopago.RecargaTelefonicaRequest;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTransaction2faMandatoryTest {

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

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    private GestoPagoTransactionServiceImpl service;
    private Cuenta cuentaActiva;

    @BeforeEach
    void setUp() {
        cuentaActiva = new Cuenta();
        cuentaActiva.setNumeroCuenta("4229215075268914");
        cuentaActiva.setEstatus("ACTIVA");
        cuentaActiva.setSaldo(new BigDecimal("1000.00"));

        lenient().when(cuentaRepository.findByNumeroCuentaForUpdate("4229215075268914"))
                .thenReturn(Optional.of(cuentaActiva));
        lenient().when(transaccionRepository.save(any(GestoPagoTransaccion.class)))
                .thenAnswer(inv -> {
                    GestoPagoTransaccion entidad = inv.getArgument(0);
                    entidad.setId(1L);
                    entidad.setCreatedAt(LocalDateTime.now());
                    return entidad;
                });

        service = new GestoPagoTransactionServiceImpl(
                transactionClient,
                tokenService,
                cryptoService,
                catalogProductRepository,
                transaccionRepository,
                cuentaRepository,
                twoFactorAuthService,
                new ObjectMapper(),
                83,
                "GPS83-TPV-17",
                "test-api-key",
                "083",
                false,
                true
        );
    }

    @Test
    void procesarRecargaDebeRechazarCuandoFaltaCodigo2FA() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("50.00"))
                .numeroCuenta("4229215075268914")
                .simular(true)
                .codigo2FA(null)
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.procesarRecarga(request)
        );

        assertTrue(ex.getMessage().contains("El código de verificación 2FA de 6 dígitos es obligatorio"));
        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(transactionClient, twoFactorAuthService);
    }

    @Test
    void procesarPagoServicioDebeRechazarCuandoCodigo2FAEstaVacio() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .idProducto(205)
                .idServicio(76)
                .referencia("987654321")
                .montoPago(new BigDecimal("100.00"))
                .numeroCuenta("4229215075268914")
                .simular(true)
                .codigo2FA("   ")
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.procesarPagoServicio(request)
        );

        assertTrue(ex.getMessage().contains("El código de verificación 2FA de 6 dígitos es obligatorio"));
        verify(cuentaRepository, never()).save(any());
        verifyNoInteractions(transactionClient, twoFactorAuthService);
    }

    @Test
    void procesarRecargaSimuladaConCodigo2FAValidoDebeConsumirOtpYDebitarSaldo() {
        RecargaTelefonicaRequest request = RecargaTelefonicaRequest.builder()
                .idProducto(582)
                .idServicio(133)
                .telefono("5512345678")
                .monto(new BigDecimal("50.00"))
                .numeroCuenta("4229215075268914")
                .simular(true)
                .codigo2FA("482910")
                .build();

        GestoPagoTransaccionResponse response = service.procesarRecarga(request);

        assertTrue(response.isExitoso());
        assertEquals(EstadoTransaccion.EXITOSA, response.getEstado());
        assertEquals(new BigDecimal("950.00"), cuentaActiva.getSaldo());
        verify(twoFactorAuthService).validarYConsumirCodigo("4229215075268914", "482910");
        verify(cuentaRepository).save(cuentaActiva);
    }
}
