package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.seguridad.DosFactorOtp;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.DosFactorOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private DosFactorOtpRepository otpRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TwoFactorAuthServiceImpl twoFactorService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        twoFactorService = new TwoFactorAuthServiceImpl(cuentaRepository, otpRepository, redisTemplate);
    }

    @Test
    void debeGenerarCodigoOtpExitosamente() {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(3L);
        cuenta.setNumeroCuenta("8766090787749132");
        cuenta.setEstatus("ACTIVA");

        when(cuentaRepository.findByNumeroCuenta("8766090787749132")).thenReturn(Optional.of(cuenta));

        Solicitud2faRequest request = Solicitud2faRequest.builder()
                .numeroCuenta("8766090787749132")
                .build();

        TwoFactorOtpResponse response = twoFactorService.solicitarCodigo(request);

        assertNotNull(response);
        assertEquals("8766090787749132", response.getNumeroCuenta());
        assertEquals(300, response.getExpiraEnSegundos());
        assertNotNull(response.getCodigoOtp());
        assertEquals(6, response.getCodigoOtp().length());
        assertTrue(response.getCodigoOtp().matches("^[0-9]{6}$"));

        verify(otpRepository).save(any(DosFactorOtp.class));
    }

    @Test
    void debeRechazarGeneracionSiCuentaNoExiste() {
        when(cuentaRepository.findByNumeroCuenta("9999999999999999")).thenReturn(Optional.empty());

        Solicitud2faRequest request = Solicitud2faRequest.builder()
                .numeroCuenta("9999999999999999")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> twoFactorService.solicitarCodigo(request));

        assertTrue(ex.getMessage().contains("no existe"));
    }

    @Test
    void debeRechazarGeneracionSiCuentaInactiva() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("8766090787749132");
        cuenta.setEstatus("BLOQUEADA");

        when(cuentaRepository.findByNumeroCuenta("8766090787749132")).thenReturn(Optional.of(cuenta));

        Solicitud2faRequest request = Solicitud2faRequest.builder()
                .numeroCuenta("8766090787749132")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> twoFactorService.solicitarCodigo(request));

        assertTrue(ex.getMessage().contains("no se encuentra activa"));
    }

    @Test
    void debeValidarYConsumirCodigoExitosamenteEnPostgres() {
        DosFactorOtp otp = new DosFactorOtp("8766090787749132", "654321", LocalDateTime.now().plusMinutes(5));
        when(otpRepository.findTopVigentePorCuenta("8766090787749132")).thenReturn(Optional.of(otp));

        assertDoesNotThrow(() -> twoFactorService.validarYConsumirCodigo("8766090787749132", "654321"));

        assertTrue(otp.isConsumido());
        verify(otpRepository).save(otp);
        verify(redisTemplate).delete("2fa:otp:8766090787749132");
    }

    @Test
    void debeRechazarCodigoSiEsIncorrecto() {
        DosFactorOtp otp = new DosFactorOtp("8766090787749132", "654321", LocalDateTime.now().plusMinutes(5));
        when(otpRepository.findTopVigentePorCuenta("8766090787749132")).thenReturn(Optional.of(otp));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> twoFactorService.validarYConsumirCodigo("8766090787749132", "111111"));

        assertTrue(ex.getMessage().contains("Código 2FA incorrecto"));
        assertFalse(otp.isConsumido());
        verify(otpRepository, never()).save(otp);
    }

    @Test
    void debeRechazarCodigoSiExpiroONoExiste() {
        when(otpRepository.findTopVigentePorCuenta("8766090787749132")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> twoFactorService.validarYConsumirCodigo("8766090787749132", "654321"));

        assertTrue(ex.getMessage().contains("ha expirado o no ha sido solicitado"));
    }
}
