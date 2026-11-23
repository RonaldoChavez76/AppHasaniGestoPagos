package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.Login2faVerificacionRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.SesionActivaResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionRolTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private SesionUsuarioService sesionUsuarioService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @Mock
    private CuentaRepository cuentaRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AutenticacionServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AutenticacionServiceImpl(
                usuarioRepository,
                passwordEncoder,
                jwtTokenService,
                sesionUsuarioService,
                twoFactorAuthService,
                cuentaRepository
        );
    }

    @Test
    void verificarLoginCon2FADevuelveRolEjecutivoCuandoUsuarioEsEjecutivo() {
        Usuario ejecutivo = crearUsuario(RolUsuario.EJECUTIVO, "ej@example.com");
        Cuenta cuenta = crearCuenta("9941467045485002");

        when(usuarioRepository.findByCliente_Correo("ej@example.com")).thenReturn(Optional.of(ejecutivo));
        when(cuentaRepository.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(1L, "ACTIVA"))
                .thenReturn(Optional.of(cuenta));
        when(jwtTokenService.emit(eq("ej@example.com"), eq(2L), eq(1L), anyString(), eq("WEB")))
                .thenReturn(new JwtTokenService.Token("jwt-ej", Instant.now().plusSeconds(900), 900));

        LoginResponse response = service.verificarLoginCon2FA(
                new Login2faVerificacionRequest("ej@example.com", "Secreta1!", "123456", "WEB", false)
        );

        assertEquals("EJECUTIVO", response.rol());
        assertEquals(2L, response.usuarioId());
        verify(twoFactorAuthService).validarYConsumirCodigo("9941467045485002", "123456");
    }

    @Test
    void verificarLoginCon2FADevuelveRolClienteCuandoUsuarioEsCliente() {
        Usuario clienteUsuario = crearUsuario(RolUsuario.CLIENTE, "cli@example.com");
        Cuenta cuenta = crearCuenta("4229215075268914");

        when(usuarioRepository.findByCliente_Correo("cli@example.com")).thenReturn(Optional.of(clienteUsuario));
        when(cuentaRepository.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(1L, "ACTIVA"))
                .thenReturn(Optional.of(cuenta));
        when(jwtTokenService.emit(eq("cli@example.com"), eq(2L), eq(1L), anyString(), eq("MOVIL")))
                .thenReturn(new JwtTokenService.Token("jwt-cli", Instant.now().plusSeconds(900), 900));

        LoginResponse response = service.verificarLoginCon2FA(
                new Login2faVerificacionRequest("cli@example.com", "Secreta1!", "654321", "MOVIL", false)
        );

        assertEquals("CLIENTE", response.rol());
    }

    @Test
    void consultarSesionActualDevuelveRolEjecutivo() {
        Usuario ejecutivo = crearUsuario(RolUsuario.EJECUTIVO, "ej@example.com");
        Cuenta cuenta = crearCuenta("9941467045485002");

        when(jwtTokenService.validar("token-ej"))
                .thenReturn(Optional.of(new JwtTokenService.AuthenticatedUser(2L, 1L, "ej@example.com", "sid-ej", "WEB")));
        when(sesionUsuarioService.esSesionValida(2L, "sid-ej")).thenReturn(true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(ejecutivo));
        when(cuentaRepository.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(1L, "ACTIVA"))
                .thenReturn(Optional.of(cuenta));

        SesionActivaResponse response = service.consultarSesionActual("Bearer token-ej");

        assertTrue(response.activa());
        assertEquals("EJECUTIVO", response.rol());
    }

    @Test
    void consultarSesionActualDevuelveRolClienteCuandoEsCliente() {
        Usuario clienteUsuario = crearUsuario(RolUsuario.CLIENTE, "cli@example.com");
        Cuenta cuenta = crearCuenta("4229215075268914");

        when(jwtTokenService.validar("token-cli"))
                .thenReturn(Optional.of(new JwtTokenService.AuthenticatedUser(2L, 1L, "cli@example.com", "sid-cli", "MOVIL")));
        when(sesionUsuarioService.esSesionValida(2L, "sid-cli")).thenReturn(true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(clienteUsuario));
        when(cuentaRepository.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(1L, "ACTIVA"))
                .thenReturn(Optional.of(cuenta));

        SesionActivaResponse response = service.consultarSesionActual("Bearer token-cli");

        assertTrue(response.activa());
        assertEquals("CLIENTE", response.rol());
    }

    private Usuario crearUsuario(RolUsuario rol, String correo) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setPrimerNombre("Usuario");
        cliente.setApellidoPaterno("Prueba");
        cliente.setCorreo(correo);
        cliente.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setCliente(cliente);
        usuario.setRol(rol);
        usuario.setPasswordHash(passwordEncoder.encode("Secreta1!"));
        usuario.setActivo(true);
        return usuario;
    }

    private Cuenta crearCuenta(String numeroCuenta) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setEstatus("ACTIVA");
        cuenta.setSaldo(new BigDecimal("1500.00"));
        return cuenta;
    }
}
