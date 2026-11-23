package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.seguridad.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BrevoEmailServiceImpl implements EmailService {

    private final RestClient restClient;
    private final boolean mailEnabled;
    private final String apiUrl;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    @Autowired
    public BrevoEmailServiceImpl(
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.brevo.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.from:no-reply@gestopago.local}") String fromEmail,
            @Value("${app.mail.from-name:GestoPago Digital}") String fromName
    ) {
        this(
                crearRestClientPorDefecto(),
                mailEnabled,
                apiUrl,
                apiKey,
                fromEmail,
                fromName
        );
    }

    public BrevoEmailServiceImpl(
            RestClient restClient,
            boolean mailEnabled,
            String apiUrl,
            String apiKey,
            String fromEmail,
            String fromName
    ) {
        this.restClient = restClient;
        this.mailEnabled = mailEnabled;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.fromEmail = fromEmail != null ? fromEmail.trim() : "";
        this.fromName = (fromName == null || fromName.isBlank()) ? "GestoPago Digital" : fromName.trim();
    }

    private static RestClient crearRestClientPorDefecto() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean estaHabilitado() {
        return mailEnabled && !apiKey.isBlank() && !fromEmail.isBlank();
    }

    @Override
    public ResultadoEnvioCorreo enviarCodigo2FA(
            String correoDestino,
            String nombreCliente,
            String codigoOtp,
            String numeroCuenta,
            String motivo,
            long minutosVigencia
    ) {
        String destinoLimpio = correoDestino != null ? correoDestino.trim() : "";
        String enmascarado = enmascararCorreo(destinoLimpio);

        if (destinoLimpio.isEmpty() || !destinoLimpio.contains("@")) {
            return new ResultadoEnvioCorreo(false, enmascarado, "El cliente no tiene un correo electrónico válido.");
        }

        if (!mailEnabled || apiKey.isBlank() || fromEmail.isBlank()) {
            log.info("Envío de correo 2FA en modo local (MAIL_ENABLED={}) para destino={}", mailEnabled, enmascarado);
            return new ResultadoEnvioCorreo(false, enmascarado, "Servicio de correo en modo local.");
        }

        String nombreMostrar = (nombreCliente == null || nombreCliente.isBlank()) ? "Cliente GestoPago" : nombreCliente.trim();
        boolean esCompra = "COMPRA".equalsIgnoreCase(motivo);
        String asunto = esCompra
                ? "Autoriza tu operación en GestoPago — Código 2FA"
                : "Tu código de verificación 2FA — GestoPago";
        String descripcionOperacion = esCompra
                ? "autorizar una compra o pago de servicio"
                : "iniciar sesión en la plataforma";

        String html = "<div style=\"font-family:Segoe UI,Arial,sans-serif;max-width:520px;margin:0 auto;padding:24px;border:1px solid #d9e2e7;border-radius:16px;background:#ffffff;\">"
                + "<h2 style=\"color:#176B87;margin:0 0 12px 0;\">GestoPago Digital</h2>"
                + "<p style=\"color:#334155;font-size:15px;margin:0 0 16px 0;\">Hola <strong>" + escaparHtml(nombreMostrar) + "</strong>,</p>"
                + "<p style=\"color:#475569;font-size:14px;line-height:1.5;margin:0 0 18px 0;\">Utiliza el siguiente código temporal de seguridad para " + descripcionOperacion + " con tu cuenta <strong>" + escaparHtml(enmascararCuenta(numeroCuenta)) + "</strong>:</p>"
                + "<div style=\"background:#E6F2F6;border:1px solid #176B87;border-radius:12px;padding:18px;text-align:center;margin:0 0 18px 0;\">"
                + "<span style=\"font-size:30px;font-weight:800;letter-spacing:8px;color:#0F4C61;\">" + escaparHtml(codigoOtp) + "</span>"
                + "</div>"
                + "<p style=\"color:#64748b;font-size:13px;margin:0;\">Este código expira en <strong>" + minutosVigencia + " minutos</strong>. Si no solicitaste esta operación, ignora este mensaje y cambia tu contraseña.</p>"
                + "</div>";

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", destinoLimpio, "name", nombreMostrar)),
                "subject", asunto,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri(apiUrl)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Código OTP 2FA enviado exitosamente vía Brevo hacia {}", enmascarado);
            return new ResultadoEnvioCorreo(true, enmascarado, "Código enviado a tu correo electrónico.");
        } catch (Exception ex) {
            log.error("Error al enviar correo OTP 2FA vía Brevo hacia {}: {}", enmascarado, ex.getMessage());
            return new ResultadoEnvioCorreo(false, enmascarado, "No fue posible enviar el correo vía Brevo.");
        }
    }

    @Override
    public String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            return "";
        }
        String limpio = correo.trim();
        int arroba = limpio.indexOf('@');
        String local = limpio.substring(0, arroba);
        String dominio = limpio.substring(arroba);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + dominio;
        }
        return local.substring(0, 2) + "***" + local.charAt(local.length() - 1) + dominio;
    }

    private String enmascararCuenta(String cuenta) {
        if (cuenta == null || cuenta.length() <= 4) {
            return cuenta == null ? "" : cuenta;
        }
        return "****" + cuenta.substring(cuenta.length() - 4);
    }

    private String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
