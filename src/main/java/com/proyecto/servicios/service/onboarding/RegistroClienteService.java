package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;

public interface RegistroClienteService {

    RegistroClienteResponse registrar(RegistroClienteRequest request);

    void darDeBaja(Long clienteId);
}
