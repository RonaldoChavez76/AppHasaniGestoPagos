package com.proyecto.servicios.service.onboarding;

import java.time.Instant;
import java.util.Optional;

public interface JwtTokenService {

    Token emit(String correo, Long usuarioId, Long clienteId);

    default Token emit(String correo, Long usuarioId, Long clienteId, String sessionId, String plataforma) {
        return emit(correo, usuarioId, clienteId);
    }

    Optional<AuthenticatedUser> validar(String token);

    record Token(String valor, Instant expiraEn, long duracionSegundos) {
    }

    record AuthenticatedUser(
            Long usuarioId,
            Long clienteId,
            String correo,
            String sessionId,
            String plataforma
    ) {
        public AuthenticatedUser(Long usuarioId, Long clienteId, String correo) {
            this(usuarioId, clienteId, correo, null, null);
        }
    }
}
