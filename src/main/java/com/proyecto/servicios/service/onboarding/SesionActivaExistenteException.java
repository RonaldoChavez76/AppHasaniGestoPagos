package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.ConflictoSesionResponse;

import java.time.LocalDateTime;

public class SesionActivaExistenteException extends RuntimeException {

    private final String plataformaActiva;
    private final LocalDateTime fechaInicio;
    private final LocalDateTime fechaExpiracion;

    public SesionActivaExistenteException(
            String plataformaActiva,
            LocalDateTime fechaInicio,
            LocalDateTime fechaExpiracion
    ) {
        super("Ya existe una sesión activa en la plataforma " + plataformaActiva
                + ". Debe finalizar la sesión anterior antes de iniciar una nueva.");
        this.plataformaActiva = plataformaActiva;
        this.fechaInicio = fechaInicio;
        this.fechaExpiracion = fechaExpiracion;
    }

    public String getPlataformaActiva() {
        return plataformaActiva;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public ConflictoSesionResponse toResponse() {
        return new ConflictoSesionResponse(
                true,
                plataformaActiva,
                fechaInicio,
                fechaExpiracion,
                "Se detectó una sesión activa en " + plataformaActiva
                        + ". Confirme el cierre de la sesión anterior enviando forzarCierreSesion=true."
        );
    }
}

