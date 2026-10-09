package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuentaAndClienteId(String numeroCuenta, Long clienteId);

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Page<Cuenta> findAllByEstatus(String estatus, Pageable pageable);

    Page<Cuenta> findAllByClienteId(Long clienteId, Pageable pageable);
}
