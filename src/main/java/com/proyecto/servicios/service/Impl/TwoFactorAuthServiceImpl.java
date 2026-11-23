package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.seguridad.DosFactorOtp;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.DosFactorOtpRepository;
import com.proyecto.servicios.service.seguridad.EmailService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private static final String REDIS_PREFIX = "2fa:otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final long EXPIRACION_SEGUNDOS = 300;

    private final CuentaRepository cuentaRepository;
    private final DosFactorOtpRepository otpRepository;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final boolean exponerOtpEnRespuesta;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public TwoFactorAuthServiceImpl(
            CuentaRepository cuentaRepository,
            DosFactorOtpRepository otpRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) EmailService emailService,
            @Value("${app.2fa.exponer-otp-en-respuesta:true}") boolean exponerOtpEnRespuesta
    ) {
        this.cuentaRepository = cuentaRepository;
        this.otpRepository = otpRepository;
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.exponerOtpEnRespuesta = exponerOtpEnRespuesta;
    }

    public TwoFactorAuthServiceImpl(
            CuentaRepository cuentaRepository,
            DosFactorOtpRepository otpRepository,
            StringRedisTemplate redisTemplate
    ) {
        this(cuentaRepository, otpRepository, redisTemplate, null, true);
    }

    @Override
    @Transactional
    public TwoFactorOtpResponse solicitarCodigo(Solicitud2faRequest request) {
        String numeroCuenta = request.getNumeroCuenta().trim();

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta " + numeroCuenta + " no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new IllegalArgumentException("La cuenta " + numeroCuenta + " no se encuentra activa para operar");
        }

        int codigoNumerico = 100000 + secureRandom.nextInt(900000);
        String codigoOtp = String.valueOf(codigoNumerico);
        LocalDateTime expiracion = LocalDateTime.now().plus(OTP_TTL);

        DosFactorOtp otpEntidad = new DosFactorOtp(numeroCuenta, codigoOtp, expiracion);
        otpRepository.save(otpEntidad);
        log.info("Código OTP 2FA persistido en PostgreSQL para cuenta: {}", numeroCuenta);

        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_PREFIX + numeroCuenta;
                redisTemplate.opsForValue().set(redisKey, codigoOtp, OTP_TTL);
                log.info("Código OTP 2FA cacheado en Redis para cuenta: {} (TTL {} min)", numeroCuenta, OTP_TTL.toMinutes());
            } catch (Exception e) {
                log.warn("Redis no disponible en este entorno, operando únicamente con PostgreSQL: {}", e.getMessage());
            }
        }

        boolean correoEnviado = false;
        String correoEnmascarado = null;
        Cliente cliente = cuenta.getCliente();
        if (emailService != null && cliente != null && cliente.getCorreo() != null) {
            String nombreCliente = construirNombre(cliente);
            EmailService.ResultadoEnvioCorreo resultado = emailService.enviarCodigo2FA(
                    cliente.getCorreo(),
                    nombreCliente,
                    codigoOtp,
                    numeroCuenta,
                    request.getMotivo(),
                    OTP_TTL.toMinutes()
            );
            correoEnviado = resultado.enviado();
            correoEnmascarado = resultado.correoDestinoEnmascarado();

            if (emailService.estaHabilitado() && !correoEnviado && !exponerOtpEnRespuesta) {
                throw new IllegalStateException(
                        "No fue posible enviar el código 2FA a tu correo electrónico. Verifica la configuración de Brevo."
                );
            }
        }

        String codigoRespuesta = (exponerOtpEnRespuesta || emailService == null || !emailService.estaHabilitado())
                ? codigoOtp
                : null;

        String mensajeUsuario = correoEnviado
                ? "Código de verificación OTP enviado a tu correo " + correoEnmascarado + ". Válido por 5 minutos."
                : "Código de verificación OTP generado exitosamente. Válido por 5 minutos.";

        log.info("=== [2FA BANCARIO] CÓDIGO GENERADO PARA CUENTA {} (correoEnviado={}) ===", numeroCuenta, correoEnviado);

        return TwoFactorOtpResponse.builder()
                .numeroCuenta(numeroCuenta)
                .mensaje(mensajeUsuario)
                .expiraEnSegundos(EXPIRACION_SEGUNDOS)
                .codigoOtp(codigoRespuesta)
                .fechaExpiracion(expiracion.toString())
                .correoEnviado(correoEnviado)
                .correoDestinoEnmascarado(correoEnmascarado)
                .build();
    }

    @Override
    @Transactional
    public void validarYConsumirCodigo(String numeroCuenta, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código 2FA de 6 dígitos es obligatorio para autorizar la operación");
        }

        String codigoLimpio = codigo.trim();
        String cuentaLimpia = numeroCuenta.trim();

        Optional<DosFactorOtp> otpOpt = otpRepository.findTopVigentePorCuenta(cuentaLimpia);

        if (otpOpt.isEmpty() || otpOpt.get().getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "El código 2FA ha expirado o no ha sido solicitado para la cuenta " + cuentaLimpia +
                            ". Por favor solicita un nuevo código de verificación.");
        }

        DosFactorOtp otp = otpOpt.get();

        if (!otp.getCodigoOtp().equals(codigoLimpio)) {
            throw new IllegalArgumentException("Código 2FA incorrecto. Verifica el código e intenta nuevamente.");
        }

        otp.setConsumido(true);
        otpRepository.save(otp);

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_PREFIX + cuentaLimpia);
            } catch (Exception ignored) {
            }
        }

        log.info("Código 2FA validado y consumido con éxito en PostgreSQL para la cuenta: {}", cuentaLimpia);
    }

    private String construirNombre(Cliente cliente) {
        return Stream.of(
                        cliente.getPrimerNombre(),
                        cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(),
                        cliente.getApellidoMaterno()
                )
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" "));
    }
}
