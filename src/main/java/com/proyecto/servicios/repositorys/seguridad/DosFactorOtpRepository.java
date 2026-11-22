package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.DosFactorOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DosFactorOtpRepository extends JpaRepository<DosFactorOtp, Long> {

    @Query("SELECT o FROM DosFactorOtp o WHERE o.numeroCuenta = :numeroCuenta AND o.consumido = false ORDER BY o.id DESC LIMIT 1")
    Optional<DosFactorOtp> findTopVigentePorCuenta(@Param("numeroCuenta") String numeroCuenta);
}
