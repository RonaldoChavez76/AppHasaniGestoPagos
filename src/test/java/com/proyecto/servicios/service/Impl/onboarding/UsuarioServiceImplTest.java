package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.onboarding.UsuarioNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UsuarioServiceImpl(usuarioRepository, passwordEncoder);
    }

    @Test
    void obtieneUsuarioSinExponerPasswordHash() {
        Usuario usuario = usuario(true, true, "ClaveActual1!");
        when(usuarioRepository.findWithClienteById(1L)).thenReturn(Optional.of(usuario));

        var response = service.obtenerPorId(1L);

        assertEquals(1L, response.id());
        assertEquals(2L, response.clienteId());
        assertEquals("cliente@example.com", response.correo());
        assertFalse(response.toString().contains(usuario.getPasswordHash()));
    }

    @Test
    void cambiaPasswordCuandoLaActualEsCorrecta() {
        Usuario usuario = usuario(true, true, "ClaveActual1!");
        when(usuarioRepository.findWithClienteById(1L)).thenReturn(Optional.of(usuario));

        service.cambiarPassword(1L, new CambiarPasswordRequest("ClaveActual1!", "ClaveNueva2!"));

        assertTrue(passwordEncoder.matches("ClaveNueva2!", usuario.getPasswordHash()));
        assertFalse(passwordEncoder.matches("ClaveActual1!", usuario.getPasswordHash()));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rechazaPasswordActualIncorrectaSinGuardar() {
        Usuario usuario = usuario(true, true, "ClaveActual1!");
        when(usuarioRepository.findWithClienteById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class,
                () -> service.cambiarPassword(1L, new CambiarPasswordRequest("Incorrecta1!", "ClaveNueva2!")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaCambioSiUsuarioOClienteEstaInactivo() {
        Usuario usuario = usuario(false, true, "ClaveActual1!");
        when(usuarioRepository.findWithClienteById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class,
                () -> service.cambiarPassword(1L, new CambiarPasswordRequest("ClaveActual1!", "ClaveNueva2!")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaPasswordNuevaIgualALaActual() {
        Usuario usuario = usuario(true, true, "ClaveActual1!");
        when(usuarioRepository.findWithClienteById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarPassword(1L, new CambiarPasswordRequest("ClaveActual1!", "ClaveActual1!")));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteProduceNotFound() {
        when(usuarioRepository.findWithClienteById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    private Usuario usuario(boolean usuarioActivo, boolean clienteActivo, String password) {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setCorreo("cliente@example.com");
        cliente.setActivo(clienteActivo);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCliente(cliente);
        usuario.setActivo(usuarioActivo);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        return usuario;
    }
}
