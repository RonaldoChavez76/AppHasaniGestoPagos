package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;

public interface AutenticacionService {

    LoginResponse iniciarSesion(LoginRequest request);
}
