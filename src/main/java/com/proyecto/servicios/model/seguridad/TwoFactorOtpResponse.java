package com.proyecto.servicios.model.seguridad;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con la información del código OTP 2FA generado")
public class TwoFactorOtpResponse {

    @Schema(description = "Número de cuenta asociada", example = "4229215075268914")
    private String numeroCuenta;

    @Schema(description = "Mensaje informativo para el usuario", example = "Código de verificación OTP generado exitosamente. Válido por 5 minutos.")
    private String mensaje;

    @Schema(description = "Tiempo de validez en segundos", example = "300")
    private long expiraEnSegundos;

    @Schema(description = "Código de 6 dígitos (disponible en entorno local/pruebas)", example = "482910")
    private String codigoOtp;

    @Schema(description = "Fecha y hora de expiración en formato ISO-8601", example = "2026-10-26T19:20:00")
    private String fechaExpiracion;

    @Schema(description = "Indica si el código OTP fue enviado por correo electrónico", example = "true")
    private boolean correoEnviado;

    @Schema(description = "Correo destino enmascarado donde se envió el OTP", example = "ch***5@gmail.com")
    private String correoDestinoEnmascarado;
}
