package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioControllerTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final UsuarioController controller = new UsuarioController(usuarioService);

    @Test
    void consultaSoloElUsuarioAutenticado() {
        MockHttpServletRequest request = solicitudAutenticada(5L);
        UsuarioResponse usuario = new UsuarioResponse(
                5L, 9L, "usuario@example.com", true, OffsetDateTime.now(), OffsetDateTime.now());
        when(usuarioService.obtenerPorId(5L)).thenReturn(usuario);

        assertEquals(usuario, controller.obtenerPorId(5L, request));
        verify(usuarioService).obtenerPorId(5L);
    }

    @Test
    void impideConsultarOtroUsuario() {
        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.obtenerPorId(6L, solicitudAutenticada(5L)));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void permiteCambiarPasswordDelUsuarioAutenticado() {
        var cambio = new CambiarPasswordRequest("Actual1!", "Nueva2!");

        var response = controller.cambiarPassword(5L, cambio, solicitudAutenticada(5L));

        assertEquals(204, response.getStatusCode().value());
        verify(usuarioService).cambiarPassword(5L, cambio);
    }

    @Test
    void impideCambiarPasswordDeOtroUsuario() {
        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.cambiarPassword(
                        6L,
                        new CambiarPasswordRequest("Actual1!", "Nueva2!"),
                        solicitudAutenticada(5L)));

        verifyNoInteractions(usuarioService);
    }

    private MockHttpServletRequest solicitudAutenticada(Long usuarioId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(usuarioId, 9L, "usuario@example.com")
        );
        return request;
    }
}
