package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.seguridad.SesionUsuario;
import com.proyecto.servicios.repositorys.seguridad.SesionUsuarioRepository;
import com.proyecto.servicios.service.onboarding.SesionActivaExistenteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionUsuarioServiceImplTest {

    @Mock
    private SesionUsuarioRepository sesionRepository;

    private SesionUsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SesionUsuarioServiceImpl(sesionRepository);
    }

    @Test
    void registraNuevaSesionCuandoNoExisteSesionPrevia() {
        when(sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(2L))
                .thenReturn(List.of());
        when(sesionRepository.save(any(SesionUsuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SesionUsuario sesion = service.registrarNuevaSesion(2L, 3L, "sess-1", "MOVIL", 900, false);

        assertTrue(sesion.isActiva());
        assertEquals("MOVIL", sesion.getPlataforma());
        assertEquals("sess-1", sesion.getSessionId());
    }

    @Test
    void lanzaExcepcionDeConflictoCuandoYaExisteSesionActivaYNoSeFuerzaCierre() {
        SesionUsuario activaEnMovil = new SesionUsuario(
                2L, 3L, "sess-movil", "MOVIL", LocalDateTime.now().plusMinutes(10)
        );
        when(sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(2L))
                .thenReturn(List.of(activaEnMovil));

        SesionActivaExistenteException ex = assertThrows(
                SesionActivaExistenteException.class,
                () -> service.registrarNuevaSesion(2L, 3L, "sess-web", "WEB", 900, false)
        );

        assertEquals("MOVIL", ex.getPlataformaActiva());
        assertTrue(ex.toResponse().sesionActivaExistente());
    }

    @Test
    void cierraSesionAnteriorYRegistraNuevaCuandoForzarCierreEsTrue() {
        SesionUsuario activaEnMovil = new SesionUsuario(
                2L, 3L, "sess-movil", "MOVIL", LocalDateTime.now().plusMinutes(10)
        );
        when(sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(2L))
                .thenReturn(List.of(activaEnMovil));
        when(sesionRepository.save(any(SesionUsuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SesionUsuario nuevaEnWeb = service.registrarNuevaSesion(2L, 3L, "sess-web", "WEB", 900, true);

        assertFalse(activaEnMovil.isActiva());
        assertNotNull(activaEnMovil.getFechaCierre());
        assertTrue(nuevaEnWeb.isActiva());
        assertEquals("WEB", nuevaEnWeb.getPlataforma());
        assertEquals("sess-web", nuevaEnWeb.getSessionId());
    }

    @Test
    void invalidaSesionAlCerrarSesionExplicitamente() {
        SesionUsuario sesion = new SesionUsuario(
                2L, 3L, "sess-1", "MOVIL", LocalDateTime.now().plusMinutes(10)
        );
        when(sesionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(sesion));
        when(sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(2L))
                .thenReturn(List.of(sesion));

        service.cerrarSesion(2L, "sess-1");

        assertFalse(sesion.isActiva());
        assertEquals("LOGOUT_USUARIO", sesion.getMotivoCierre());
    }

    @Test
    void validaCorrectamenteSiLaSesionSigueVigente() {
        SesionUsuario sesion = new SesionUsuario(
                2L, 3L, "sess-1", "WEB", LocalDateTime.now().plusMinutes(10)
        );
        when(sesionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(sesion));

        assertTrue(service.esSesionValida(2L, "sess-1"));

        sesion.setActiva(false);
        assertFalse(service.esSesionValida(2L, "sess-1"));
    }
}

