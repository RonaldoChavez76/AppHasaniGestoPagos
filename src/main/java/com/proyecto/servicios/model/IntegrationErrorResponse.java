package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class IntegrationErrorResponse {

    private final int status;
    private final String message;
    private final Instant timestamp;
}