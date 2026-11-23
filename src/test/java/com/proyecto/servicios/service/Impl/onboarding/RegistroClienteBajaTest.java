package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistroClienteBajaTest {

    @Test
    void darDeBajaDebeInactivarEntidadesYCerrarTodasLasSesiones() {
        ClienteRepository clienteRepo = Mockito.mock(ClienteRepository.class);
        DomicilioRepository domicilioRepo = Mockito.mock(DomicilioRepository.class);
        UsuarioRepository usuarioRepo = Mockito.mock(UsuarioRepository.class);
        CuentaRepository cuentaRepo = Mockito.mock(CuentaRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        SesionUsuarioService sesionService = Mockito.mock(SesionUsuarioService.class);

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setActivo(true);

        Cuenta cuentaActiva = new Cuenta();
        cuentaActiva.setNumeroCuenta("4229215075268914");
        cuentaActiva.setEstatus("ACTIVA");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setActivo(true);
        cliente.setUsuario(usuario);
        cliente.setCuentas(List.of(cuentaActiva));

        Mockito.when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));

        RegistroClienteServiceImpl service = new RegistroClienteServiceImpl(
                clienteRepo,
                domicilioRepo,
                usuarioRepo,
                cuentaRepo,
                encoder,
                sesionService
        );

        service.darDeBaja(1L);

        assertFalse(cliente.isActivo());
        assertFalse(usuario.isActivo());
        assertEquals("INACTIVA", cuentaActiva.getEstatus());
        Mockito.verify(sesionService).cerrarTodasLasSesionesActivas(20L, "BAJA_CLIENTE");
    }
}
