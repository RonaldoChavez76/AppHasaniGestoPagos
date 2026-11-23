package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.seguridad.DosFactorOtp;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.DosFactorOtpRepository;
import com.proyecto.servicios.service.seguridad.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TwoFactorAuthEmailTest {

    @Test
    void solicitarCodigoDebeOcultarOtpCuandoCorreoSeEnviaYExposicionEstaDesactivada() {
        CuentaRepository cuentaRepo = Mockito.mock(CuentaRepository.class);
        DosFactorOtpRepository otpRepo = Mockito.mock(DosFactorOtpRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);

        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Ana");
        cliente.setApellidoPaterno("Prueba");
        cliente.setCorreo("ana.prueba@correo.com");

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("4229215075268914");
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(cliente);

        Mockito.when(cuentaRepo.findByNumeroCuenta("4229215075268914")).thenReturn(Optional.of(cuenta));
        Mockito.when(otpRepo.save(Mockito.any(DosFactorOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(emailService.estaHabilitado()).thenReturn(true);
        Mockito.when(emailService.enviarCodigo2FA(
                Mockito.eq("ana.prueba@correo.com"),
                Mockito.eq("Ana Prueba"),
                Mockito.anyString(),
                Mockito.eq("4229215075268914"),
                Mockito.eq("COMPRA"),
                Mockito.eq(5L)
        )).thenReturn(new EmailService.ResultadoEnvioCorreo(true, "an***a@correo.com", "Enviado"));

        TwoFactorAuthServiceImpl service = new TwoFactorAuthServiceImpl(
                cuentaRepo,
                otpRepo,
                null,
                emailService,
                false
        );

        Solicitud2faRequest req = new Solicitud2faRequest("4229215075268914", "COMPRA");
        TwoFactorOtpResponse resp = service.solicitarCodigo(req);

        assertNull(resp.getCodigoOtp());
        assertTrue(resp.isCorreoEnviado());
        assertEquals("an***a@correo.com", resp.getCorreoDestinoEnmascarado());
    }

    @Test
    void solicitarCodigoDebeFallarSiCorreoEstaHabilitadoYFallaEnvioSinExponerOtp() {
        CuentaRepository cuentaRepo = Mockito.mock(CuentaRepository.class);
        DosFactorOtpRepository otpRepo = Mockito.mock(DosFactorOtpRepository.class);
        EmailService emailService = Mockito.mock(EmailService.class);

        Cliente cliente = new Cliente();
        cliente.setPrimerNombre("Ana");
        cliente.setCorreo("ana.prueba@correo.com");

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("4229215075268914");
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(cliente);

        Mockito.when(cuentaRepo.findByNumeroCuenta("4229215075268914")).thenReturn(Optional.of(cuenta));
        Mockito.when(otpRepo.save(Mockito.any(DosFactorOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(emailService.estaHabilitado()).thenReturn(true);
        Mockito.when(emailService.enviarCodigo2FA(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyLong()
        )).thenReturn(new EmailService.ResultadoEnvioCorreo(false, "an***a@correo.com", "Error Brevo"));

        TwoFactorAuthServiceImpl service = new TwoFactorAuthServiceImpl(
                cuentaRepo,
                otpRepo,
                null,
                emailService,
                false
        );

        Solicitud2faRequest req = new Solicitud2faRequest("4229215075268914", "LOGIN");
        assertThrows(IllegalStateException.class, () -> service.solicitarCodigo(req));
    }
}
