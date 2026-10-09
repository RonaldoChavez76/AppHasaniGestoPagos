package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
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
