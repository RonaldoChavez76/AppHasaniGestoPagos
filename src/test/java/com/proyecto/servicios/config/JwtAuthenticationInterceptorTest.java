package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationInterceptorTest {

    private final JwtTokenService tokenService = mock(JwtTokenService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final CuentaRepository cuentaRepository = mock(CuentaRepository.class);
    private final JwtAuthenticationInterceptor interceptor =
            new JwtAuthenticationInterceptor(
                    tokenService, usuarioRepository, cuentaRepository, new ObjectMapper().findAndRegisterModules());

    @Test
    void permiteRegistroPublicoDeCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clientes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verifyNoInteractions(tokenService);
    }

    @Test
    void bloqueaConsultaSinToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        verifyNoInteractions(tokenService, usuarioRepository);
    }

    @Test
    void validaTokenYAdjuntaLaIdentidadAlRequest() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        when(cuentaRepository.existsByNumeroCuentaAndClienteId("123", 1L)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cuentas/123");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(user, request.getAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE));
        verify(tokenService).validar("jwt");
        verify(usuarioRepository).findRolActivaById(2L);
        verify(cuentaRepository).existsByNumeroCuentaAndClienteId("123", 1L);
    }

    @Test
    void revocaTokenCuandoUsuarioOClienteYaNoEstanActivos() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/5");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void permiteAlClienteConsultarSoloSuPropioPerfil() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/1");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void deniegaAlClienteLaListaCompletaDeClientes() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void deniegaAlClienteConsultarElPerfilDeOtraPersona() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes/99");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void permiteAlEjecutivoConsultarLaListaDeClientes() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "ejecutivo@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.EJECUTIVO));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/clientes");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void deniegaAlClienteElSaldoDeUnaCuentaAjena() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        when(cuentaRepository.existsByNumeroCuentaAndClienteId("otra-cuenta", 1L)).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cuentas/otra-cuenta/saldo");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void permiteAlClienteDarseDeBajaElMismo() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/clientes/1");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void deniegaAlClienteDarseDeBajaOtroCliente() throws Exception {
        JwtTokenService.AuthenticatedUser user =
                new JwtTokenService.AuthenticatedUser(2L, 1L, "cliente@example.com");
        when(tokenService.validar("jwt")).thenReturn(Optional.of(user));
        when(usuarioRepository.findRolActivaById(2L)).thenReturn(Optional.of(RolUsuario.CLIENTE));
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/clientes/99");
        request.addHeader("Authorization", "Bearer jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }
}
