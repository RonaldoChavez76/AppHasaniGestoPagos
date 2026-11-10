package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.Login2faChallengeResponse;
import com.proyecto.servicios.model.onboarding.Login2faSolicitudRequest;
import com.proyecto.servicios.model.onboarding.Login2faVerificacionRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.SesionActivaExistenteException;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private BCryptPasswordEncoder passwordEncoder;
    private AutenticacionServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AutenticacionServiceImpl(usuarioRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void autenticaUsuarioActivoYDevuelveToken() {
        Usuario usuario = usuario(true, true, "Correcta1!");
        when(usuarioRepository.findByCliente_Correo("cliente@example.com")).thenReturn(Optional.of(usuario));
        when(jwtTokenService.emit("cliente@example.com", 2L, 1L))
                .thenReturn(new JwtTokenService.Token("jwt", Instant.now().plusSeconds(900), 900));

        LoginResponse response = service.iniciarSesion(new LoginRequest(" Cliente@Example.com ", "Correcta1!"));

        assertEquals("jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
    }

    @Test
    void rechazaCredencialesIncorrectas() {
        when(usuarioRepository.findByCliente_Correo("cliente@example.com"))
                .thenReturn(Optional.of(usuario(true, true, "Correcta1!")));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.iniciarSesion(new LoginRequest("cliente@example.com", "Incorrecta1!")));
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void rechazaUsuarioInactivo() {
        when(usuarioRepository.findByCliente_Correo("cliente@example.com"))
                .thenReturn(Optional.of(usuario(false, true, "Correcta1!")));

        assertThrows(UsuarioInactivoException.class,
                () -> service.iniciarSesion(new LoginRequest("cliente@example.com", "Correcta1!")));
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void rechazaPasswordMayorAlLimiteDeBytesDeBcryptAntesDeConsultarUsuario() {
        LoginRequest request = new LoginRequest("persona@example.com", "A".repeat(73));

        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(request));
        verifyNoInteractions(usuarioRepository, jwtTokenService);
    }

    @Test
    void rechazaPasswordMultibyteQueSuperaElLimiteDeBcrypt() {
        LoginRequest request = new LoginRequest("persona@example.com", "Á".repeat(37));

        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(request));
        verifyNoInteractions(usuarioRepository, jwtTokenService);
    }

    @Test
    void rechazaClienteInactivo() {
        when(usuarioRepository.findByCliente_Correo("cliente@example.com"))
                .thenReturn(Optional.of(usuario(true, false, "Correcta1!")));

        assertThrows(UsuarioInactivoException.class,
                () -> service.iniciarSesion(new LoginRequest("cliente@example.com", "Correcta1!")));
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void solicitaReto2FAYVerificaCodigoRegistrandoSesionUnica() {
        SesionUsuarioService sesionService = mock(SesionUsuarioService.class);
        TwoFactorAuthService twoFactorService = mock(TwoFactorAuthService.class);
        CuentaRepository cuentaRepo = mock(CuentaRepository.class);

        AutenticacionServiceImpl authCon2FA = new AutenticacionServiceImpl(
                usuarioRepository, passwordEncoder, jwtTokenService, sesionService, twoFactorService, cuentaRepo
        );

        Usuario usuario = usuario(true, true, "Correcta1!");
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("4229215075268914");
        cuenta.setEstatus("ACTIVA");
        cuenta.setSaldo(new java.math.BigDecimal("5000.00"));

        when(usuarioRepository.findByCliente_Correo("cliente@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepo.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(1L, "ACTIVA"))
                .thenReturn(Optional.of(cuenta));
        when(twoFactorService.solicitarCodigo(any()))
                .thenReturn(TwoFactorOtpResponse.builder()
                        .numeroCuenta("4229215075268914")
                        .codigoOtp("123456")
                        .expiraEnSegundos(300)
                        .build());

        Login2faChallengeResponse reto = authCon2FA.solicitarLoginCon2FA(
                new Login2faSolicitudRequest("cliente@example.com", "Correcta1!", "MOVIL", false)
        );

        assertTrue(reto.requiere2FA());
        assertEquals("123456", reto.codigoOtp());
        assertEquals("MOVIL", reto.plataforma());

        when(jwtTokenService.emit(eq("cliente@example.com"), eq(2L), eq(1L), anyString(), eq("MOVIL")))
                .thenReturn(new JwtTokenService.Token("jwt-movil", Instant.now().plusSeconds(900), 900));

        LoginResponse loginFinal = authCon2FA.verificarLoginCon2FA(
                new Login2faVerificacionRequest("cliente@example.com", "Correcta1!", "123456", "MOVIL", false)
        );

        assertEquals("jwt-movil", loginFinal.accessToken());
        assertEquals("MOVIL", loginFinal.plataforma());
        assertEquals("4229215075268914", loginFinal.numeroCuenta());
        verify(twoFactorService).validarYConsumirCodigo("4229215075268914", "123456");
        verify(sesionService).registrarNuevaSesion(eq(2L), eq(1L), anyString(), eq("MOVIL"), eq(900L), eq(false));
    }

    @Test
    void detectaConflictoDeSesionActivaEnOtraPlataforma() {
        SesionUsuarioService sesionService = mock(SesionUsuarioService.class);
        TwoFactorAuthService twoFactorService = mock(TwoFactorAuthService.class);
        CuentaRepository cuentaRepo = mock(CuentaRepository.class);

        AutenticacionServiceImpl authConSesiones = new AutenticacionServiceImpl(
                usuarioRepository, passwordEncoder, jwtTokenService, sesionService, twoFactorService, cuentaRepo
        );

        Usuario usuario = usuario(true, true, "Correcta1!");
        when(usuarioRepository.findByCliente_Correo("cliente@example.com")).thenReturn(Optional.of(usuario));
        doThrow(new SesionActivaExistenteException("MOVIL", java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusMinutes(10)))
                .when(sesionService).verificarDisponibilidadOCerrar(2L, "WEB", false);

        assertThrows(SesionActivaExistenteException.class, () ->
                authConSesiones.solicitarLoginCon2FA(
                        new Login2faSolicitudRequest("cliente@example.com", "Correcta1!", "WEB", false)
                )
        );
    }

    private Usuario usuario(boolean activo, boolean clienteActivo, String password) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setCorreo("cliente@example.com");
        cliente.setActivo(clienteActivo);

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setCliente(cliente);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setActivo(activo);
        return usuario;
    }
}
