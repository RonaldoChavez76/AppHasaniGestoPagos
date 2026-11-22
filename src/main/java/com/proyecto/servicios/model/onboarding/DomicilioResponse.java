package com.proyecto.servicios.model.onboarding;

public record DomicilioResponse(
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String municipio,
        String estado,
        String codigoPostal,
        String pais
) {
}
