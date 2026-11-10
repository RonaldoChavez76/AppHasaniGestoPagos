package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.*;

public interface AutenticacionService {

    LoginResponse iniciarSesion(LoginRequest request);

    Login2faChallengeResponse solicitarLoginCon2FA(Login2faSolicitudRequest request);

    LoginResponse verificarLoginCon2FA(Login2faVerificacionRequest request);

    void cerrarSesion(String authorizationHeader);

    SesionActivaResponse consultarSesionActual(String authorizationHeader);
}
