package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.CambiarPasswordUsuarioRequest;
import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.model.onboarding.UsuarioIdRequest;
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

        assertEquals(usuario, controller.consultar(new UsuarioIdRequest(5L), request).data());
        verify(usuarioService).obtenerPorId(5L);
    }

    @Test
    void impideConsultarOtroUsuario() {
        AccesoNoAutorizadoException exception = assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.consultar(new UsuarioIdRequest(6L), solicitudAutenticada(5L)));

        assertTrue(exception.getMessage().contains("propio usuario"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void ejecutivoPuedeConsultarOtroUsuario() {
        MockHttpServletRequest request = solicitudAutenticada(5L);
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.EJECUTIVO);
        UsuarioResponse usuario = new UsuarioResponse(
                6L, 10L, "otro@example.com", true, OffsetDateTime.now(), OffsetDateTime.now());
        when(usuarioService.obtenerPorId(6L)).thenReturn(usuario);

        assertEquals(usuario, controller.consultar(new UsuarioIdRequest(6L), request).data());

        verify(usuarioService).obtenerPorId(6L);
    }

    @Test
    void niegaConsultaSiElRolNoEstaPresente() {
        MockHttpServletRequest request = solicitudAutenticada(5L);
        request.removeAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.consultar(new UsuarioIdRequest(5L), request));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void permiteCambiarPasswordDelUsuarioAutenticado() {
        var cambio = new CambiarPasswordRequest("Actual1!", "Nueva2!");

        var response = controller.cambiarPassword(
                new CambiarPasswordUsuarioRequest(5L, cambio), solicitudAutenticada(5L));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, response.getBody().status());
        assertNull(response.getBody().data());
        verify(usuarioService).cambiarPassword(5L, cambio);
    }

    @Test
    void impideCambiarPasswordDeOtroUsuario() {
        AccesoNoAutorizadoException exception = assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.cambiarPassword(
                        new CambiarPasswordUsuarioRequest(
                                6L, new CambiarPasswordRequest("Actual1!", "Nueva2!")),
                        solicitudAutenticada(5L)));

        assertTrue(exception.getMessage().contains("Solo puedes cambiar la contraseña"));
        verifyNoInteractions(usuarioService);
    }

    @Test
    void ejecutivoNoPuedeCambiarPasswordDeOtroUsuario() {
        MockHttpServletRequest request = solicitudAutenticada(5L);
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.EJECUTIVO);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.cambiarPassword(
                        new CambiarPasswordUsuarioRequest(
                                6L, new CambiarPasswordRequest("Actual1!", "Nueva2!")),
                        request));

        verifyNoInteractions(usuarioService);
    }

    private MockHttpServletRequest solicitudAutenticada(Long usuarioId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.CLIENTE);
        request.setAttribute(
                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(usuarioId, 9L, "usuario@example.com")
        );
        return request;
    }
}
