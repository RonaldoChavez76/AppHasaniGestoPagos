package com.proyecto.servicios.service.onboarding;

import java.time.Instant;
import java.util.Optional;

public interface JwtTokenService {

    Token emit(String correo, Long usuarioId, Long clienteId);

    Optional<AuthenticatedUser> validar(String token);

    record Token(String valor, Instant expiraEn, long duracionSegundos) {
    }

    record AuthenticatedUser(Long usuarioId, Long clienteId, String correo) {
    }
}
