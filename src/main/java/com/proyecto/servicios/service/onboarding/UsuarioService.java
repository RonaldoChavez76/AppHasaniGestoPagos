package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;

public interface UsuarioService {

    UsuarioResponse obtenerPorId(Long id);

    void cambiarPassword(Long id, CambiarPasswordRequest request);
}
