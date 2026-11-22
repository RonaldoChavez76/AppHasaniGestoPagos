package com.proyecto.servicios.service.seguridad;

import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;

public interface TwoFactorAuthService {

    /**
     * Genera un código OTP de 6 dígitos con expiración de 5 minutos,
     * almacenándolo en Redis y asociándolo a la cuenta.
     */
    TwoFactorOtpResponse solicitarCodigo(Solicitud2faRequest request);

    /**
     * Valida el código 2FA para el número de cuenta y, si es correcto,
     * lo consume (elimina) inmediatamente para evitar reutilización (One-Time).
     *
     * @throws IllegalArgumentException si el código es nulo, inválido o ha expirado.
     */
    void validarYConsumirCodigo(String numeroCuenta, String codigo);
}
