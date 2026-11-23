package com.proyecto.servicios.service.Impl.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.controller.ClienteOnboardingController;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.ActualizarClienteRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.Login2faVerificacionRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.ReactivarClienteRequest;
import com.proyecto.servicios.model.onboarding.SesionActivaResponse;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.repositorys.seguridad.DosFactorOtpRepository;
import com.proyecto.servicios.service.Impl.TwoFactorAuthServiceImpl;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReactivacionCuentaTest {

    @Test
    void reactivarRestauraClienteUsuarioYCuentasInactivas() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        DomicilioRepository domicilioRepo = mock(DomicilioRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        CuentaRepository cuentaRepo = mock(CuentaRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setActivo(false);

        Cuenta cuentaInactiva = new Cuenta();
        cuentaInactiva.setNumeroCuenta("4229215075268914");
        cuentaInactiva.setEstatus("INACTIVA");

        Cuenta cuentaBloqueada = new Cuenta();
        cuentaBloqueada.setNumeroCuenta("4229215075269999");
        cuentaBloqueada.setEstatus("BLOQUEADA");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setActivo(false);
        cliente.setUsuario(usuario);
        cliente.setCuentas(List.of(cuentaInactiva, cuentaBloqueada));

        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));

        RegistroClienteServiceImpl service = new RegistroClienteServiceImpl(
                clienteRepo,
                domicilioRepo,
                usuarioRepo,
                cuentaRepo,
                encoder
        );

        service.reactivar(1L);

        assertTrue(cliente.isActivo());
        assertTrue(usuario.isActivo());
        assertEquals("ACTIVA", cuentaInactiva.getEstatus());
        assertEquals("BLOQUEADA", cuentaBloqueada.getEstatus());
    }

    @Test
    void rechazaActualizarClienteCuandoEstaInactivo() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        Cliente clienteInactivo = new Cliente();
        clienteInactivo.setId(5L);
        clienteInactivo.setActivo(false);

        when(clienteRepo.findWithDomicilioById(5L)).thenReturn(Optional.of(clienteInactivo));

        ClienteConsultaServiceImpl consultaService = new ClienteConsultaServiceImpl(clienteRepo);
        ActualizarClienteRequest request = new ActualizarClienteRequest(
                "Ana", null, "Lopez", "Garcia", LocalDate.of(1995, 5, 10),
                "FEMENINO", "Mexicana", "Soltera", "ana@example.com", "5512345678",
                null, "Ingeniera", "Tech", new BigDecimal("25000.00"),
                new DomicilioRequest("Reforma", "10", null, "Centro", "Cuauhtemoc", "CDMX", "06000", "Mexico")
        );

        assertThrows(UsuarioInactivoException.class, () -> consultaService.actualizar(5L, request));
    }

    @Test
    void twoFactorPermiteLoginEnCuentaInactivaYBloqueaCompra() {
        CuentaRepository cuentaRepo = mock(CuentaRepository.class);
        DosFactorOtpRepository otpRepo = mock(DosFactorOtpRepository.class);

        Cuenta cuentaInactiva = new Cuenta();
        cuentaInactiva.setNumeroCuenta("9941467045485002");
        cuentaInactiva.setEstatus("INACTIVA");

        when(cuentaRepo.findByNumeroCuenta("9941467045485002")).thenReturn(Optional.of(cuentaInactiva));

        TwoFactorAuthServiceImpl service = new TwoFactorAuthServiceImpl(cuentaRepo, otpRepo, null);

        TwoFactorOtpResponse otpLogin = service.solicitarCodigo(
                Solicitud2faRequest.builder().numeroCuenta("9941467045485002").motivo("LOGIN").build()
        );
        assertNotNull(otpLogin.getCodigoOtp());

        assertThrows(IllegalArgumentException.class, () -> service.solicitarCodigo(
                Solicitud2faRequest.builder().numeroCuenta("9941467045485002").motivo("COMPRA").build()
        ));
    }

    @Test
    void interceptorPermiteReactivarAUsuarioInactivoConRolDegradadoYBloqueaOtrasRutas() throws Exception {
        JwtTokenService tokenService = mock(JwtTokenService.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        JwtAuthenticationInterceptor interceptor = new JwtAuthenticationInterceptor(
                tokenService, usuarioRepo, new ObjectMapper().findAndRegisterModules()
        );

        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "inactivo@example.com");
        when(tokenService.validar("jwt-inactivo")).thenReturn(Optional.of(user));
        when(usuarioRepo.findRolActivaById(2L)).thenReturn(Optional.empty());
        when(usuarioRepo.findRolById(2L)).thenReturn(Optional.of(RolUsuario.EJECUTIVO));

        MockHttpServletRequest putReactivar = new MockHttpServletRequest("PUT", "/clientes/reactivar");
        putReactivar.addHeader("Authorization", "Bearer jwt-inactivo");
        MockHttpServletResponse respReactivar = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(putReactivar, respReactivar, new Object()));
        assertEquals(RolUsuario.CLIENTE, putReactivar.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE));

        MockHttpServletRequest putActualizar = new MockHttpServletRequest("PUT", "/clientes/actualizar");
        putActualizar.addHeader("Authorization", "Bearer jwt-inactivo");
        MockHttpServletResponse respActualizar = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(putActualizar, respActualizar, new Object()));
        assertEquals(403, respActualizar.getStatus());
    }

    @Test
    void controladorPermiteReactivarPropioClienteYEjecutivoPuedeReactivarAOtro() {
        RegistroClienteService registroService = mock(RegistroClienteService.class);
        ClienteConsultaService consultaService = mock(ClienteConsultaService.class);
        ClienteOnboardingController controller = new ClienteOnboardingController(registroService, consultaService);

        MockHttpServletRequest reqCliente = new MockHttpServletRequest("PUT", "/clientes/reactivar");
        reqCliente.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.CLIENTE);
        reqCliente.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(2L, 10L, "cliente@example.com"));

        var resPropio = controller.reactivar(new ReactivarClienteRequest(10L), reqCliente);
        assertEquals(200, resPropio.getStatusCode().value());
        verify(registroService).reactivar(10L);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.reactivar(new ReactivarClienteRequest(99L), reqCliente));

        MockHttpServletRequest reqEjecutivo = new MockHttpServletRequest("PUT", "/clientes/reactivar");
        reqEjecutivo.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.EJECUTIVO);
        reqEjecutivo.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(1L, 1L, "ejecutivo@example.com"));

        var resEjecutivo = controller.reactivar(new ReactivarClienteRequest(99L), reqEjecutivo);
        assertEquals(200, resEjecutivo.getStatusCode().value());
        verify(registroService).reactivar(99L);
    }

    @Test
    void autenticacionDevuelveCuentaActivaFalseCuandoClienteEstaInactivo() {
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        JwtTokenService tokenService = mock(JwtTokenService.class);
        SesionUsuarioService sesionService = mock(SesionUsuarioService.class);
        TwoFactorAuthService twoFactorService = mock(TwoFactorAuthService.class);
        CuentaRepository cuentaRepo = mock(CuentaRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setPrimerNombre("Ana");
        cliente.setApellidoPaterno("Prueba");
        cliente.setCorreo("ana.inactiva@example.com");
        cliente.setActivo(false);

        Usuario usuario = new Usuario();
        usuario.setId(14L);
        usuario.setCliente(cliente);
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setPasswordHash(encoder.encode("Secreta1!"));
        usuario.setActivo(false);

        Cuenta cuentaInactiva = new Cuenta();
        cuentaInactiva.setNumeroCuenta("4229215075268914");
        cuentaInactiva.setEstatus("INACTIVA");
        cuentaInactiva.setSaldo(new BigDecimal("1200.00"));

        when(usuarioRepo.findByCliente_Correo("ana.inactiva@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepo.findById(14L)).thenReturn(Optional.of(usuario));
        when(cuentaRepo.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(7L, "ACTIVA"))
                .thenReturn(Optional.empty());
        when(cuentaRepo.findByClienteIdOrderByIdAsc(7L)).thenReturn(List.of(cuentaInactiva));
        when(tokenService.emit(eq("ana.inactiva@example.com"), eq(14L), eq(7L), anyString(), eq("WEB")))
                .thenReturn(new JwtTokenService.Token("jwt-inactivo", Instant.now().plusSeconds(900), 900));
        when(tokenService.validar("jwt-inactivo"))
                .thenReturn(Optional.of(new JwtTokenService.AuthenticatedUser(14L, 7L, "ana.inactiva@example.com", "sid-inact", "WEB")));
        when(sesionService.esSesionValida(14L, "sid-inact")).thenReturn(true);

        AutenticacionServiceImpl service = new AutenticacionServiceImpl(
                usuarioRepo, encoder, tokenService, sesionService, twoFactorService, cuentaRepo
        );

        LoginResponse login = service.verificarLoginCon2FA(
                new Login2faVerificacionRequest("ana.inactiva@example.com", "Secreta1!", "123456", "WEB", false)
        );
        assertEquals(Boolean.FALSE, login.cuentaActiva());
        assertEquals("4229215075268914", login.numeroCuenta());

        SesionActivaResponse sesion = service.consultarSesionActual("Bearer jwt-inactivo");
        assertTrue(sesion.activa());
        assertEquals(Boolean.FALSE, sesion.cuentaActiva());
    }
}
