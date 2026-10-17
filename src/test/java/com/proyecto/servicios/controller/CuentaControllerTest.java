package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.model.onboarding.BuscarCuentasRequest;
import com.proyecto.servicios.service.onboarding.AccesoNoAutorizadoException;
import com.proyecto.servicios.service.onboarding.CuentaConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CuentaControllerTest {

    private final CuentaConsultaService consultaService = mock(CuentaConsultaService.class);
    private final CuentaController controller = new CuentaController(consultaService);

    @Test
    void clienteNecesitaIdentidadAsociadaYNumeroDeCuenta() {
        var sinNumero = new BuscarCuentasRequest(null, null, null, 0, 20);
        var conNumero = new BuscarCuentasRequest("123", null, null, 0, 20);

        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(sinNumero, solicitudCliente(7L, 15L)));
        assertThrows(AccesoNoAutorizadoException.class,
                () -> controller.buscar(conNumero, solicitudCliente(7L, null)));
        verifyNoInteractions(consultaService);
    }

    @Test
    void identidadClienteNoPuedeElegirClienteIdEnLaBusqueda() {
        when(consultaService.buscar(any())).thenReturn(Page.empty());
        var filtros = new BuscarCuentasRequest("123", 999L, false, 0, 20);

        controller.buscar(filtros, solicitudCliente(7L, 15L));

        var captor = org.mockito.ArgumentCaptor.forClass(BuscarCuentasRequest.class);
        verify(consultaService).buscar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(15L, captor.getValue().clienteId());
    }

    private MockHttpServletRequest solicitudCliente(Long usuarioId, Long clienteId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.CLIENTE);
        request.setAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                new JwtTokenService.AuthenticatedUser(usuarioId, clienteId, "cliente@example.com"));
        return request;
    }
}
