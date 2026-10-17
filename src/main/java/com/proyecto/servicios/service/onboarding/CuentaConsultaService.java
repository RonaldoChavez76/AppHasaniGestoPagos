package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoCuentaResponse;
import com.proyecto.servicios.model.onboarding.BuscarCuentasRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CuentaConsultaService {

    CuentaResponse obtenerPorNumero(String numeroCuenta);

    SaldoCuentaResponse obtenerSaldo(String numeroCuenta);

    Page<CuentaResponse> listarActivas(Pageable pageable);

    Page<CuentaResponse> buscar(BuscarCuentasRequest filtros);
}
