package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroClienteRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String primerNombre,
        @Pattern(regexp = "^$|^(?=.{3,50}$)[\\p{L}]+(?: [\\p{L}]+)*$")
        String segundoNombre,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String apellidoPaterno,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String apellidoMaterno,
        @NotNull @Past
        LocalDate fechaNacimiento,
        @NotBlank @Pattern(regexp = "(?i)^[A-Z][AEIOUX][A-Z]{2}[0-9]{6}[HM][A-Z]{5}[A-Z0-9][0-9]$")
        String curp,
        @NotBlank @Pattern(regexp = "(?i)^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$")
        String rfc,
        @NotBlank @Size(min = 1, max = 20) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String sexo,
        @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String nacionalidad,
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[\\p{L}]+(?: [\\p{L}]+)*$")
        String estadoCivil,
        @NotBlank @Email @Size(min = 6, max = 100) @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        String correo,
        @NotBlank @Pattern(regexp = "^[0-9]{10}$")
        String telefonoMovil,
        @Pattern(regexp = "^$|^[0-9]{10}$")
        String telefonoAlternativo,
        @NotBlank @Size(min = 3, max = 80) @Pattern(regexp = "^[\\p{L}.\\-/]+(?: [\\p{L}.\\-/]+)*$")
        String ocupacion,
        @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = "^[\\p{L}0-9.,&\\-/]+(?: [\\p{L}0-9.,&\\-/]+)*$")
        String empresa,
        @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "99999999.99") @Digits(integer = 10, fraction = 2)
        BigDecimal ingresoMensual,
        @NotNull @Valid
        DomicilioRequest domicilio,
        @NotBlank @Size(min = 8, max = 12)
        @Pattern(regexp = "^(?!.*\\s)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,12}$")
        String password
) {
    public RegistroClienteRequest {
        primerNombre = OnboardingText.strip(primerNombre);
        segundoNombre = OnboardingText.optional(segundoNombre);
        apellidoPaterno = OnboardingText.strip(apellidoPaterno);
        apellidoMaterno = OnboardingText.strip(apellidoMaterno);
        curp = OnboardingText.strip(curp);
        rfc = OnboardingText.strip(rfc);
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
