package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.onboarding.BuscarCuentasRequest;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class CuentaSpecifications {

    private CuentaSpecifications() {
    }

    public static Specification<Cuenta> conFiltros(BuscarCuentasRequest filtros) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtros.numeroCuenta() != null) {
                predicates.add(builder.equal(root.get("numeroCuenta"), filtros.numeroCuenta()));
            }
            if (filtros.clienteId() != null) {
                predicates.add(builder.equal(root.get("cliente").get("id"), filtros.clienteId()));
            }
            if (Boolean.TRUE.equals(filtros.soloActivas())) {
                predicates.add(builder.equal(root.get("estatus"), "ACTIVA"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
