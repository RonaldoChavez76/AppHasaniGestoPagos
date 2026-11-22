package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;

public record BuscarClientesRequest(
        @Size(max = 50)
        @Pattern(regexp = "^[\\p{L} ]+$")
        String nombre,
        @Pattern(regexp = "(?i)^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$")
        String rfc,
        @Pattern(regexp = "(?i)^[A-Z][AEIOUX][A-Z]{2}[0-9]{6}[HM][A-Z]{5}[A-Z0-9][0-9]$")
        String curp,
        @Email @Size(max = 100)
        String correo,
        @Size(max = 20)
        String numeroCuenta,
        @Positive
        Long clienteId,
        Boolean activo,
        @PastOrPresent
        LocalDate registradoDesde,
        @PastOrPresent
        LocalDate registradoHasta,
        @Min(0)
        Integer page,
        @Min(1)
        @Max(100)
        Integer size
) {
    public BuscarClientesRequest {
        nombre = OnboardingText.optional(nombre);
        rfc = normalizarIdentificador(rfc);
        curp = normalizarIdentificador(curp);
        correo = OnboardingText.optional(correo);
        if (correo != null) {
            correo = correo.toLowerCase(Locale.ROOT);
        }
        numeroCuenta = OnboardingText.optional(numeroCuenta);
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    private static String normalizarIdentificador(String valor) {
        String normalizado = OnboardingText.optional(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }

    @AssertTrue(message = "La fecha inicial debe ser anterior o igual a la fecha final.")
    @JsonIgnore
    public boolean isRangoFechasValido() {
        return registradoDesde == null || registradoHasta == null || !registradoHasta.isBefore(registradoDesde);
    }
}
