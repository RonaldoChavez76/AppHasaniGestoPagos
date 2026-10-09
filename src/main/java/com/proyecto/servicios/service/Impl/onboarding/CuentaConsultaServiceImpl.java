package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.model.onboarding.SaldoCuentaResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.onboarding.CuentaConsultaService;
import com.proyecto.servicios.service.onboarding.CuentaNoEncontradaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaConsultaServiceImpl implements CuentaConsultaService {

    private final CuentaRepository cuentaRepository;

    public CuentaConsultaServiceImpl(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public CuentaResponse obtenerPorNumero(String numeroCuenta) {
        return respuesta(cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public SaldoCuentaResponse obtenerSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        return new SaldoCuentaResponse(cuenta.getNumeroCuenta(), cuenta.getSaldo());
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public Page<CuentaResponse> listarActivas(Pageable pageable) {
        return cuentaRepository.findAllByEstatus("ACTIVA", pageable).map(this::respuesta);
    }

    private CuentaResponse respuesta(Cuenta cuenta) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getNumeroCuenta(),
                cuenta.getCliente().getId(),
                cuenta.getSaldo(),
                cuenta.getEstatus(),
                cuenta.getFechaApertura()
        );
    }
}
