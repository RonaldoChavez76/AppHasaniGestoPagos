package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActualizarClienteRequest(
        @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[\\p{L} ]+$")
        String primerNombre,
        @Pattern(regexp = "^$|^[\\p{L} ]{2,50}$")
        String segundoNombre,
        @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[\\p{L} ]+$")
        String apellidoPaterno,
        @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[\\p{L} ]+$")
        String apellidoMaterno,
        @NotNull LocalDate fechaNacimiento,
        @NotBlank @Size(max = 20) String sexo,
        @NotBlank @Size(max = 40) String nacionalidad,
        @NotBlank @Size(max = 30) String estadoCivil,
        @NotBlank @Email @Size(max = 100) @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        String correo,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$")
        String telefonoMovil,
        @Pattern(regexp = "^$|^[0-9]{10}$")
        String telefonoAlternativo,
        @NotBlank @Size(max = 80) String ocupacion,
        @NotBlank @Size(max = 100) String empresa,
        @NotNull @DecimalMin(value = "0.01") @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
        BigDecimal ingresoMensual,
        @NotNull @Valid DomicilioRequest domicilio
) {
    public ActualizarClienteRequest {
        primerNombre = OnboardingText.strip(primerNombre);
        segundoNombre = OnboardingText.optional(segundoNombre);
        apellidoPaterno = OnboardingText.strip(apellidoPaterno);
        apellidoMaterno = OnboardingText.strip(apellidoMaterno);
        sexo = OnboardingText.strip(sexo);
        nacionalidad = OnboardingText.strip(nacionalidad);
        estadoCivil = OnboardingText.strip(estadoCivil);
        correo = OnboardingText.strip(correo);
        telefonoMovil = OnboardingText.strip(telefonoMovil);
        telefonoAlternativo = OnboardingText.optional(telefonoAlternativo);
        ocupacion = OnboardingText.strip(ocupacion);
        empresa = OnboardingText.strip(empresa);
    }
}
