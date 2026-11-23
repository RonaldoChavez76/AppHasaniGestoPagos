package com.proyecto.servicios.service.seguridad;

public interface EmailService {

    record ResultadoEnvioCorreo(
            boolean enviado,
            String correoDestinoEnmascarado,
            String mensaje
    ) {
    }

    boolean estaHabilitado();

    ResultadoEnvioCorreo enviarCodigo2FA(
            String correoDestino,
            String nombreCliente,
            String codigoOtp,
            String numeroCuenta,
            String motivo,
            long minutosVigencia
    );

    String enmascararCorreo(String correo);
}
