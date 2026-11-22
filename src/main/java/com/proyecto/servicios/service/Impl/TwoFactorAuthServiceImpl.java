package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.seguridad.DosFactorOtp;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.seguridad.DosFactorOtpRepository;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private static final String REDIS_PREFIX = "2fa:otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final long EXPIRACION_SEGUNDOS = 300;

    private final CuentaRepository cuentaRepository;
    private final DosFactorOtpRepository otpRepository;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorAuthServiceImpl(
            CuentaRepository cuentaRepository,
            DosFactorOtpRepository otpRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.cuentaRepository = cuentaRepository;
        this.otpRepository = otpRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public TwoFactorOtpResponse solicitarCodigo(Solicitud2faRequest request) {
        String numeroCuenta = request.getNumeroCuenta().trim();

        // 1. Validar que la cuenta exista y se encuentre activa en PostgreSQL
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta " + numeroCuenta + " no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new IllegalArgumentException("La cuenta " + numeroCuenta + " no se encuentra activa para operar");
        }

        // 2. Generar código criptográficamente seguro de 6 dígitos numéricos (100000 - 999999)
        int codigoNumerico = 100000 + secureRandom.nextInt(900000);
        String codigoOtp = String.valueOf(codigoNumerico);
        LocalDateTime expiracion = LocalDateTime.now().plus(OTP_TTL);

        // 3. Persistir en PostgreSQL (Garantiza funcionamiento en Producción sin Redis)
        DosFactorOtp otpEntidad = new DosFactorOtp(numeroCuenta, codigoOtp, expiracion);
        otpRepository.save(otpEntidad);
        log.info("Código OTP 2FA persistido en PostgreSQL para cuenta: {}", numeroCuenta);

        // 4. Si Redis está disponible, almacenar como acelerador de lectura con TTL
        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_PREFIX + numeroCuenta;
                redisTemplate.opsForValue().set(redisKey, codigoOtp, OTP_TTL);
                log.info("Código OTP 2FA cacheado en Redis para cuenta: {} (TTL {} min)", numeroCuenta, OTP_TTL.toMinutes());
            } catch (Exception e) {
                log.warn("Redis no disponible en este entorno, operando únicamente con PostgreSQL: {}", e.getMessage());
            }
        }

        log.info("=== [2FA BANCARIO] CÓDIGO GENERADO PARA CUENTA {} ===> {} ===", numeroCuenta, codigoOtp);

        return TwoFactorOtpResponse.builder()
                .numeroCuenta(numeroCuenta)
                .mensaje("Código de verificación OTP generado exitosamente. Válido por 5 minutos.")
                .expiraEnSegundos(EXPIRACION_SEGUNDOS)
                .codigoOtp(codigoOtp)
                .fechaExpiracion(expiracion.toString())
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

        // 1. Buscar el último código activo no consumido en PostgreSQL
        Optional<DosFactorOtp> otpOpt = otpRepository.findTopVigentePorCuenta(cuentaLimpia);

        if (otpOpt.isEmpty() || otpOpt.get().getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "El código 2FA ha expirado o no ha sido solicitado para la cuenta " + cuentaLimpia +
                            ". Por favor solicita un nuevo código de verificación.");
        }

        DosFactorOtp otp = otpOpt.get();

        // 2. Validar que el código coincida
        if (!otp.getCodigoOtp().equals(codigoLimpio)) {
            throw new IllegalArgumentException("Código 2FA incorrecto. Verifica el código e intenta nuevamente.");
        }

        // 3. Marcar como consumido en PostgreSQL (One-Time Password)
        otp.setConsumido(true);
        otpRepository.save(otp);

        // 4. Limpiar en Redis si está disponible
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_PREFIX + cuentaLimpia);
            } catch (Exception ignored) {
            }
        }

        log.info("Código 2FA validado y consumido con éxito en PostgreSQL para la cuenta: {}", cuentaLimpia);
    }
}
