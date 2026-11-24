package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DomicilioRequest(
        @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "^[\\p{L}0-9.,#\\-/]+(?: [\\p{L}0-9.,#\\-/]+)*$") String calle,
        @NotBlank @Size(min = 1, max = 20) @Pattern(regexp = "^[A-Za-z0-9\\-/]+$") String numeroExterior,
        @Pattern(regexp = "^$|^[A-Za-z0-9\\-/]{1,20}$") String numeroInterior,
        @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = "^[\\p{L}0-9.\\-]+(?: [\\p{L}0-9.\\-]+)*$") String colonia,
        @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = "^[\\p{L}.\\-]+(?: [\\p{L}.\\-]+)*$") String municipio,
        @NotBlank @Size(min = 2, max = 60) @Pattern(regexp = "^[\\p{L}.\\-]+(?: [\\p{L}.\\-]+)*$") String estado,
        @NotBlank @Pattern(regexp = "^[0-9]{5}$") String codigoPostal,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$") String pais
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
