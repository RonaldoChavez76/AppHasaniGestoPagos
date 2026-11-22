package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DomicilioRequest(
        @NotBlank @Size(max = 100) String calle,
        @NotBlank @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @NotBlank @Size(max = 80) String colonia,
        @NotBlank @Size(max = 80) String municipio,
        @NotBlank @Size(max = 60) String estado,
        @NotBlank @Pattern(regexp = "^[0-9]{5}$") String codigoPostal,
        @NotBlank @Size(max = 50) String pais
) {
    public DomicilioRequest {
        calle = OnboardingText.strip(calle);
        numeroExterior = OnboardingText.strip(numeroExterior);
        numeroInterior = OnboardingText.optional(numeroInterior);
        colonia = OnboardingText.strip(colonia);
        municipio = OnboardingText.strip(municipio);
        estado = OnboardingText.strip(estado);
        codigoPostal = OnboardingText.strip(codigoPostal);
        pais = OnboardingText.strip(pais);
    }
}
