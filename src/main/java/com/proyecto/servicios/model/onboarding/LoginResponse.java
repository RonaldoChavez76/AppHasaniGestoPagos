package com.proyecto.servicios.model.onboarding;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt
) {
}
