package com.proyecto.servicios.model.seguridad;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con la información del código OTP 2FA generado")
public class TwoFactorOtpResponse {

    @Schema(description = "Número de cuenta asociada", example = "8766090787749132")
    private String numeroCuenta;

    @Schema(description = "Mensaje informativo para el usuario", example = "Código de verificación OTP generado exitosamente. Válido por 5 minutos.")
    private String mensaje;

    @Schema(description = "Tiempo de validez en segundos", example = "300")
    private long expiraEnSegundos;

    @Schema(description = "Código de 6 dígitos (disponible en entorno local/pruebas)", example = "482910")
    private String codigoOtp;

    @Schema(description = "Fecha y hora de expiración en formato ISO-8601", example = "2026-10-26T19:20:00")
    private String fechaExpiracion;
}
