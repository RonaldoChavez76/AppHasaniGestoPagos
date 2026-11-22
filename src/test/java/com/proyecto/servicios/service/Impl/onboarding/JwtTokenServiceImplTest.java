package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.service.onboarding.JwtTokenService;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceImplTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-which-is-at-least-32-bytes-long".getBytes());

    @Test
    void emiteYValidaTokenConIdentidadDeUsuarioYCliente() {
        JwtTokenService service = new JwtTokenServiceImpl(SECRET, 900);
        JwtTokenService.Token token = service.emit("cliente@example.com", 12L, 34L);

        JwtTokenService.AuthenticatedUser user = service.validar(token.valor()).orElseThrow();

        assertEquals(12L, user.usuarioId());
        assertEquals(34L, user.clienteId());
        assertEquals("cliente@example.com", user.correo());
        assertEquals(900, token.duracionSegundos());
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtTokenService issuer = new JwtTokenServiceImpl(SECRET, 900);
        String otherSecret = Base64.getEncoder()
                .encodeToString("another-secret-which-is-at-least-32-bytes".getBytes());
        JwtTokenService validator = new JwtTokenServiceImpl(otherSecret, 900);

        assertTrue(validator.validar(issuer.emit("cliente@example.com", 12L, 34L).valor()).isEmpty());
    }

    @Test
    void exigeUnaClaveSecretaEnFormatoBase64() {
        JwtTokenService service = new JwtTokenServiceImpl("", 900);

        assertThrows(IllegalStateException.class, () -> service.emit("cliente@example.com", 12L, 34L));
    }
}
