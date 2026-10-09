package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    @Override
    @EntityGraph(attributePaths = {"domicilio"})
    Page<Cliente> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"domicilio"})
    Optional<Cliente> findWithDomicilioById(Long id);

    @EntityGraph(attributePaths = {"domicilio"})
    Optional<Cliente> findWithDomicilioByCurp(String curp);

    @EntityGraph(attributePaths = {"domicilio"})
    Optional<Cliente> findWithDomicilioByRfc(String rfc);

    @EntityGraph(attributePaths = {"domicilio"})
    Optional<Cliente> findWithDomicilioByCorreo(String correo);

    @EntityGraph(attributePaths = {"domicilio"})
    Optional<Cliente> findDistinctWithDomicilioByCuentasNumeroCuenta(String numeroCuenta);

    @EntityGraph(attributePaths = {"domicilio"})
    Page<Cliente> findAllByActivoTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"domicilio"})
    Page<Cliente> findAllByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(
            OffsetDateTime desde, OffsetDateTime hastaExclusivo, Pageable pageable);

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);
}
