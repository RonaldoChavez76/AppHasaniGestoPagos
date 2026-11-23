package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.seguridad.EmailService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoEmailServiceImplTest {

    @Test
    void estaHabilitadoDebeValidarConfiguracionCompleta() {
        BrevoEmailServiceImpl completo = new BrevoEmailServiceImpl(
                RestClient.builder().build(),
                true,
                "https://api.brevo.com/v3/smtp/email",
                "xkeysib-123",
                "chavezronaldo675@gmail.com",
                "GestoPago"
        );
        assertTrue(completo.estaHabilitado());

        BrevoEmailServiceImpl deshabilitado = new BrevoEmailServiceImpl(
                RestClient.builder().build(),
                false,
                "https://api.brevo.com/v3/smtp/email",
                "xkeysib-123",
                "chavezronaldo675@gmail.com",
                "GestoPago"
        );
        assertFalse(deshabilitado.estaHabilitado());

        BrevoEmailServiceImpl sinClave = new BrevoEmailServiceImpl(
                RestClient.builder().build(),
                true,
                "https://api.brevo.com/v3/smtp/email",
                "  ",
                "chavezronaldo675@gmail.com",
                "GestoPago"
        );
        assertFalse(sinClave.estaHabilitado());
    }

    @Test
    void enmascararCorreoDebeOcultarParteLocalConservandoDominio() {
        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                RestClient.builder().build(),
                false,
                "https://api.brevo.com/v3/smtp/email",
                "",
                "no-reply@gestopago.local",
                "GestoPago"
        );

        assertEquals("ch***5@gmail.com", service.enmascararCorreo("chavezronaldo675@gmail.com"));
        assertEquals("a***@x.com", service.enmascararCorreo("ab@x.com"));
        assertEquals("", service.enmascararCorreo("correo-invalido"));
    }

    @Test
    void enviarCodigo2FADebeResponderModoLocalCuandoEstaDeshabilitado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                builder.build(),
                false,
                "https://api.brevo.com/v3/smtp/email",
                "",
                "no-reply@gestopago.local",
                "GestoPago"
        );

        EmailService.ResultadoEnvioCorreo resultado = service.enviarCodigo2FA(
                "cliente@correo.com",
                "Ana Prueba",
                "482910",
                "4229215075268914",
                "LOGIN",
                5
        );

        assertFalse(resultado.enviado());
        assertEquals("cl***e@correo.com", resultado.correoDestinoEnmascarado());
        server.verify();
    }

    @Test
    void enviarCodigo2FADebeInvocarApiBrevoConApiKeyYContenidoOtp() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                builder.build(),
                true,
                "https://api.brevo.com/v3/smtp/email",
                "xkeysib-test-123",
                "chavezronaldo675@gmail.com",
                "GestoPago"
        );

        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test-123"))
                .andExpect(content().string(Matchers.containsString("482910")))
                .andExpect(content().string(Matchers.containsString("chavezronaldo675@gmail.com")))
                .andRespond(withSuccess("{\"messageId\":\"<msg-1>\"}", MediaType.APPLICATION_JSON));

        EmailService.ResultadoEnvioCorreo resultado = service.enviarCodigo2FA(
                "cliente@correo.com",
                "Ana Prueba",
                "482910",
                "4229215075268914",
                "COMPRA",
                5
        );

        assertTrue(resultado.enviado());
        assertEquals("cl***e@correo.com", resultado.correoDestinoEnmascarado());
        server.verify();
    }

    @Test
    void enviarCodigo2FADebeManejarErrorDeBrevoSinExplotar() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailServiceImpl service = new BrevoEmailServiceImpl(
                builder.build(),
                true,
                "https://api.brevo.com/v3/smtp/email",
                "xkeysib-test-123",
                "chavezronaldo675@gmail.com",
                "GestoPago"
        );

        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        EmailService.ResultadoEnvioCorreo resultado = service.enviarCodigo2FA(
                "cliente@correo.com",
                "Ana Prueba",
                "482910",
                "4229215075268914",
                "LOGIN",
                5
        );

        assertFalse(resultado.enviado());
        assertEquals("cl***e@correo.com", resultado.correoDestinoEnmascarado());
        server.verify();
    }
}
