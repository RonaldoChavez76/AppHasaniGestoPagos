package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta del reto de segundo factor (2FA) previo a emitir el token de sesión")
public record Login2faChallengeResponse(
        @Schema(description = "Indica que las credenciales son válidas y se requiere verificar el código 2FA", example = "true")
        boolean requiere2FA,

        @Schema(description = "Correo electrónico del usuario", example = "ana.lopez.prueba@example.com")
        String correo,

        @Schema(description = "Número de cuenta activa asociada para el reto OTP", example = "4229215075268914")
        String numeroCuenta,

        @Schema(description = "Plataforma solicitante (WEB o MOVIL)", example = "MOVIL")
        String plataforma,

        @Schema(description = "Código OTP de 6 dígitos generado en el servidor", example = "482910")
        String codigoOtp,

        @Schema(description = "Tiempo de vigencia del código en segundos", example = "300")
        long expiraEnSegundos,

        @Schema(description = "Mensaje informativo para el usuario", example = "Credenciales válidas. Ingrese el código de verificación de 6 dígitos para iniciar sesión.")
        String mensaje,

        @Schema(description = "Indica si el código OTP fue enviado al correo electrónico", example = "true")
        Boolean correoEnviado,

        @Schema(description = "Correo destino enmascarado donde se envió el código OTP", example = "ch***5@gmail.com")
        String correoDestinoEnmascarado
) {
    public Login2faChallengeResponse(
            boolean requiere2FA,
            String correo,
            String numeroCuenta,
            String plataforma,
            String codigoOtp,
            long expiraEnSegundos,
            String mensaje
    ) {
        this(requiere2FA, correo, numeroCuenta, plataforma, codigoOtp, expiraEnSegundos, mensaje, false, null);
    }
}
