package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.BuscarClientesRequest;
import com.proyecto.servicios.model.onboarding.ConsultarClienteRequest;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteOnboardingControllerTest {

    private final RegistroClienteService registroService = mock(RegistroClienteService.class);
    private final ClienteConsultaService consultaService = mock(ClienteConsultaService.class);
    private final ClienteOnboardingController controller =
            new ClienteOnboardingController(registroService, consultaService);

    @Test
    void consultaDeClienteSoloPermiteSuPropioId() {
        when(consultaService.obtenerPorId(15L)).thenReturn(null);

        var response = controller.consultar(
                new ConsultarClienteRequest(15L), solicitudCliente(7L, 15L));

        org.junit.jupiter.api.Assertions.assertEquals(200, response.status());
        org.junit.jupiter.api.Assertions.assertNotNull(response.timestamp());
        verify(consultaService).obtenerPorId(15L);
        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.consultar(new ConsultarClienteRequest(8L), solicitudCliente(7L, 15L)));
        verifyNoMoreInteractions(consultaService);
    }

    @Test
    void ejecutivoPuedeConsultarCualquierCliente() {
        var request = solicitudCliente(7L, 15L);
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.EJECUTIVO);
        when(consultaService.obtenerPorId(1L)).thenReturn(null);

        var response = controller.consultar(new ConsultarClienteRequest(1L), request);

        assertEquals(200, response.status());
        verify(consultaService).obtenerPorId(1L);
    }

    @Test
    void explicaCuandoUnClienteIntentaConsultarOtroCliente() {
        AccesoNoAutorizadoException exception = assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.consultar(new ConsultarClienteRequest(1L), solicitudCliente(7L, 15L)));

        assertTrue(exception.getMessage().contains("rol CLIENTE"));
        assertTrue(exception.getMessage().contains("EJECUTIVO"));
        assertTrue(exception.getMessage().contains("cliente 1"));
        verifyNoInteractions(consultaService);
    }

    @Test
    void busquedaDeClienteRequiereNumeroDeCuentaYNoPermiteOtrosFiltros() {
        var sinFiltros = new BuscarClientesRequest(null, null, null,
                null, null, null, null, null, null, 0, 20);
        var filtroPorNombre = new BuscarClientesRequest("ana", null, null,
                null, "123", null, null, null, null, 0, 20);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(sinFiltros, solicitudCliente(7L, 15L)));
        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(filtroPorNombre, solicitudCliente(7L, 15L)));
        verifyNoInteractions(consultaService);
    }

    @Test
    void indicaQueLaBusquedaGlobalRequiereEjecutivo() {
        var sinFiltros = new BuscarClientesRequest(null, null, null,
                null, null, null, null, null, null, 0, 20);

        AccesoNoAutorizadoException exception = assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(sinFiltros, solicitudCliente(7L, 15L)));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("EJECUTIVO"));
    }

    @Test
    void busquedaDeClienteNoPermiteIdentidadSinClienteAsociado() {
        var filtroCuenta = new BuscarClientesRequest(null, null, null,
                null, "123", null, null, null, null, 0, 20);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(filtroCuenta, solicitudCliente(7L, null)));
        verify(consultaService, never()).buscar(any());
    }

    private MockHttpServletRequest solicitudCliente(Long usuarioId, Long clienteId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.CLIENTE);
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(usuarioId, clienteId, "cliente@example.com"));
        return request;
    }
}
