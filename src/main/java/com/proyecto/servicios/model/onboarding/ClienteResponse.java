package com.proyecto.servicios.model.onboarding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ClienteResponse(
        Long id,
        String primerNombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno,
        LocalDate fechaNacimiento,
        String curp,
        String rfc,
        String sexo,
        String nacionalidad,
        String estadoCivil,
        String correo,
        String telefonoMovil,
        String telefonoAlternativo,
        String ocupacion,
        String empresa,
        BigDecimal ingresoMensual,
        boolean activo,
        OffsetDateTime fechaCreacion,
        DomicilioResponse domicilio
) {
}
