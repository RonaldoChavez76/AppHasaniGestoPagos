package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private RegistroClienteServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new RegistroClienteServiceImpl(
                clienteRepository,
                domicilioRepository,
                usuarioRepository,
                cuentaRepository,
                passwordEncoder
        );
    }

    @Test
    void registraClienteDomicilioUsuarioYCuentaEnConjunto() {
        when(clienteRepository.existsByCurp(any())).thenReturn(false);
        when(clienteRepository.existsByRfc(any())).thenReturn(false);
        when(clienteRepository.existsByCorreo(any())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(clienteRepository.save(any())).thenAnswer(invocation -> {
            var cliente = invocation.<com.proyecto.servicios.entity.onboarding.Cliente>getArgument(0);
            cliente.setId(1L);
            return cliente;
        });
        when(usuarioRepository.save(any())).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(2L);
            return usuario;
        });
        when(cuentaRepository.save(any())).thenAnswer(invocation -> {
            Cuenta cuenta = invocation.getArgument(0);
            cuenta.setId(3L);
            return cuenta;
        });

        RegistroClienteResponse response = service.registrar(request(LocalDate.now().minusYears(30)));

        assertEquals(1L, response.clienteId());
        assertEquals(2L, response.usuarioId());
        assertEquals(3L, response.cuentaId());
        assertEquals("persona@example.com", response.correo());
        assertTrue(response.numeroCuenta().matches("^[1-9][0-9]{15}$"));

        var clienteCaptor = org.mockito.ArgumentCaptor.forClass(
                com.proyecto.servicios.entity.onboarding.Cliente.class);
        verify(clienteRepository).save(clienteCaptor.capture());
        assertEquals("GODE561231HDFRRN09", clienteCaptor.getValue().getCurp());
        assertEquals("persona@example.com", clienteCaptor.getValue().getCorreo());

        var usuarioCaptor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals("persona@example.com", usuarioCaptor.getValue().getCliente().getCorreo());
        assertEquals(com.proyecto.servicios.entity.onboarding.RolUsuario.CLIENTE, usuarioCaptor.getValue().getRol());
        assertTrue(passwordEncoder.matches("ClaveSegura1!", usuarioCaptor.getValue().getPasswordHash()));
        assertNotEquals("ClaveSegura1!", usuarioCaptor.getValue().getPasswordHash());

        var cuentaCaptor = org.mockito.ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuentaCaptor.capture());
        assertEquals(BigDecimal.ZERO, cuentaCaptor.getValue().getSaldo());
        assertEquals("ACTIVA", cuentaCaptor.getValue().getEstatus());
        verify(domicilioRepository).save(any());
    }

    @Test
    void rechazaMenorDeEdadAntesDeGuardarDatos() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar(request(LocalDate.now().minusYears(17))));

        verifyNoInteractions(clienteRepository, domicilioRepository, usuarioRepository, cuentaRepository);
    }

    @Test
    void rechazaCurpDuplicadaAntesDeCrearRegistros() {
        when(clienteRepository.existsByCurp("GODE561231HDFRRN09")).thenReturn(true);

        assertThrows(ClienteDuplicadoException.class,
                () -> service.registrar(request(LocalDate.now().minusYears(30))));

        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(domicilioRepository, usuarioRepository, cuentaRepository);
    }

    private RegistroClienteRequest request(LocalDate fechaNacimiento) {
        return new RegistroClienteRequest(
                "Nombre",
                null,
                "Apellido",
                "ApellidoMaterno",
                fechaNacimiento,
                "GODE561231HDFRRN09",
                "GODE561231GR8",
                "M",
                "Mexicana",
                "Soltero",
                " Persona@Example.COM ",
                "5512345678",
                null,
                "Ingeniera",
                "Empresa",
                new BigDecimal("25000.00"),
                new DomicilioRequest(
                        "Calle Uno", "123", null, "Centro", "Municipio", "Estado", "01234", "México"),
                "ClaveSegura1!"
        );
    }
}
