package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private static final String REDIS_PREFIX = "2fa:otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final long EXPIRACION_SEGUNDOS = 300;

    private final CuentaRepository cuentaRepository;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    // Mecanismo de respaldo (fallback) en memoria por si Redis presenta latencia o caída
    private final Map<String, OtpCacheItem> fallbackCache = new ConcurrentHashMap<>();

    public TwoFactorAuthServiceImpl(
            CuentaRepository cuentaRepository,
            StringRedisTemplate redisTemplate) {
        this.cuentaRepository = cuentaRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TwoFactorOtpResponse solicitarCodigo(Solicitud2faRequest request) {
        String numeroCuenta = request.getNumeroCuenta().trim();

        // 1. Validar que la cuenta exista y se encuentre activa
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta " + numeroCuenta + " no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new IllegalArgumentException("La cuenta " + numeroCuenta + " no se encuentra activa para operar");
        }

        // 2. Generar código criptográficamente seguro de 6 dígitos numéricos (100000 - 999999)
        int codigoNumerico = 100000 + secureRandom.nextInt(900000);
        String codigoOtp = String.valueOf(codigoNumerico);
        String redisKey = REDIS_PREFIX + numeroCuenta;

        // 3. Persistir en Redis con tiempo de vida (TTL) de 5 minutos
        try {
            redisTemplate.opsForValue().set(redisKey, codigoOtp, OTP_TTL);
            log.info("Código OTP 2FA generado y guardado en Redis para cuenta: {} (expira en {} min)",
                    numeroCuenta, OTP_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("No se pudo escribir en Redis. Utilizando caché en memoria como fallback: {}", e.getMessage());
        }

        // Respaldo en memoria
        LocalDateTime expiracion = LocalDateTime.now().plus(OTP_TTL);
        fallbackCache.put(numeroCuenta, new OtpCacheItem(codigoOtp, expiracion));

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
    public void validarYConsumirCodigo(String numeroCuenta, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código 2FA de 6 dígitos es obligatorio para autorizar la operación");
        }

        String codigoLimpio = codigo.trim();
        String cuentaLimpia = numeroCuenta.trim();
        String redisKey = REDIS_PREFIX + cuentaLimpia;
        String codigoEsperado = null;

        // 1. Intentar recuperar desde Redis
        try {
            Object valor = redisTemplate.opsForValue().get(redisKey);
            if (valor != null) {
                codigoEsperado = valor.toString();
            }
        } catch (Exception e) {
            log.warn("Error consultando Redis para validar 2FA: {}", e.getMessage());
        }

        // 2. Si no se encontró en Redis, consultar caché de respaldo
        if (codigoEsperado == null) {
            OtpCacheItem item = fallbackCache.get(cuentaLimpia);
            if (item != null && item.expiracion().isAfter(LocalDateTime.now())) {
                codigoEsperado = item.codigo();
            }
        }

        // 3. Verificar si el código existe y no ha expirado
        if (codigoEsperado == null) {
            throw new IllegalArgumentException(
                    "El código 2FA ha expirado o no ha sido solicitado para la cuenta " + cuentaLimpia +
                            ". Por favor solicita un nuevo código de verificación.");
        }

        // 4. Comparar el código
        if (!codigoEsperado.equals(codigoLimpio)) {
            throw new IllegalArgumentException("Código 2FA incorrecto. Verifica el código e intenta nuevamente.");
        }

        // 5. Consumir (eliminar) inmediatamente para garantizar que sea de un solo uso (One-Time Password)
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ignored) {
        }
        fallbackCache.remove(cuentaLimpia);

        log.info("Código 2FA validado y consumido con éxito para la cuenta: {}", cuentaLimpia);
    }

    private record OtpCacheItem(String codigo, LocalDateTime expiracion) {}
}
