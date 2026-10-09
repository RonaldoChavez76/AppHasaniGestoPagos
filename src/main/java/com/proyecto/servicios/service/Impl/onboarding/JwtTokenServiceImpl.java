package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.service.onboarding.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final String secret;
    private final long duracionSegundos;

    public JwtTokenServiceImpl(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-seconds:900}") long duracionSegundos
    ) {
        this.secret = secret;
        this.duracionSegundos = duracionSegundos;
        if (duracionSegundos <= 0) {
            throw new IllegalArgumentException("La duración del token JWT debe ser positiva.");
        }
    }

    @Override
    public Token emit(String correo, Long usuarioId, Long clienteId) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plusSeconds(duracionSegundos);
        String valor = Jwts.builder()
                .subject(correo)
                .claim("usuarioId", usuarioId)
                .claim("clienteId", clienteId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiraEn))
                .signWith(clave())
                .compact();
        return new Token(valor, expiraEn, duracionSegundos);
    }

    @Override
    public Optional<AuthenticatedUser> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    claims.get("usuarioId", Long.class),
                    claims.get("clienteId", Long.class),
                    claims.getSubject()
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private SecretKey clave() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Debe configurarse JWT_SECRET para habilitar la autenticación JWT.");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (io.jsonwebtoken.io.DecodingException exception) {
            throw new IllegalStateException("JWT_SECRET debe ser una clave Base64 válida de al menos 256 bits.",
                    exception);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 256 bits.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
