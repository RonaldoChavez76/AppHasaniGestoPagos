package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.config.JwtAuthenticationInterceptor;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void devuelveTodosLosErroresDeValidacionAgrupadosPorCampo() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request", "primerNombre", "", false, new String[]{"NotBlank"}, null, "no debe estar vacío"));
        bindingResult.addError(new FieldError(
                "request", "primerNombre", "", false, new String[]{"Pattern"}, null, "regex interno"));
        bindingResult.addError(new FieldError(
                "request", "correo", "invalido", false, new String[]{"Email"}, null, "regex interno"));
        bindingResult.addError(new FieldError(
                "request", "correo", "invalido", false, new String[]{"Size"}, null, "regex interno"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleRequestInvalido(exception);

        assertEquals(400, response.getStatusCode().value());
        ApiResponse<Void> body = response.getBody();
        assertEquals(
                "Datos no válidos: primerNombre: es obligatorio.; "
                        + "correo: debe tener un formato de correo válido. "
                        + "no cumple con la longitud permitida.",
                body.message());
        assertEquals(400, body.status());
        assertEquals(null, body.data());
        org.junit.jupiter.api.Assertions.assertFalse(body.message().contains("regex"));
        org.junit.jupiter.api.Assertions.assertFalse(body.message().contains("["));
    }

    @Test
    void usaMensajeGenericoCuandoNoHayErroresDeCampo() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleRequestInvalido(exception);

        assertEquals("La solicitud contiene datos inválidos.", response.getBody().message());
        assertEquals(400, response.getBody().status());
        assertEquals(null, response.getBody().data());
    }

    @Test
    void diferenciaUnaSolicitudNoAutorizadaDeUnErrorDeJson() throws Exception {
        ClienteOnboardingController controller = new ClienteOnboardingController(
                mock(RegistroClienteService.class), mock(ClienteConsultaService.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(post("/clientes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.CLIENTE)
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new JwtTokenService.AuthenticatedUser(1L, 1L, "cliente@example.com"))
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Los clientes solo pueden buscar usando el número de su propia cuenta. "
                                + "Los demás filtros requieren un usuario EJECUTIVO."));
    }

    @Test
    void devuelveMensajeAmigableCuandoElJsonEstaMalformado() throws Exception {
        ClienteOnboardingController controller = new ClienteOnboardingController(
                mock(RegistroClienteService.class), mock(ClienteConsultaService.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(post("/clientes/busqueda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_ROLE_ATTRIBUTE, RolUsuario.EJECUTIVO)
                        .requestAttr(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new JwtTokenService.AuthenticatedUser(1L, 1L, "ejecutivo@example.com"))
                        .content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No se pudo leer la solicitud. Verifica que el JSON tenga comillas, llaves y comas correctas "
                                + "y que sus campos correspondan a este endpoint."));
    }
}
