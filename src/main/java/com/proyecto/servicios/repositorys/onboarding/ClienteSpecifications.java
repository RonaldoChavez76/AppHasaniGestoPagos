package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.model.onboarding.BuscarClientesRequest;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ClienteSpecifications {

    private static final List<String> CAMPOS_NOMBRE = List.of(
            "primerNombre", "segundoNombre", "apellidoPaterno", "apellidoMaterno");

    private ClienteSpecifications() {
    }

    public static Specification<Cliente> conFiltros(BuscarClientesRequest filtros) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtros.nombre() != null) {
                String[] terminos = filtros.nombre().toLowerCase(Locale.ROOT).split("\\s+");
                for (String termino : terminos) {
                    String patron = "%" + termino + "%";
                    Predicate coincideConAlgunCampo = CAMPOS_NOMBRE.stream()
                            .map(campo -> builder.like(
                                    builder.lower(builder.coalesce(root.get(campo), "")),
                                    patron))
                            .reduce(builder::or)
                            .orElseThrow();
                    predicates.add(coincideConAlgunCampo);
                }
            }
            if (filtros.rfc() != null) {
                predicates.add(builder.equal(root.get("rfc"), filtros.rfc()));
            }
            if (filtros.curp() != null) {
                predicates.add(builder.equal(root.get("curp"), filtros.curp()));
            }
            if (filtros.correo() != null) {
                predicates.add(builder.equal(root.get("correo"), filtros.correo()));
            }
            if (filtros.numeroCuenta() != null) {
                var cuentas = root.join("cuentas");
                predicates.add(builder.equal(cuentas.get("numeroCuenta"), filtros.numeroCuenta()));
                query.distinct(true);
            }
            if (filtros.clienteId() != null) {
                predicates.add(builder.equal(root.get("id"), filtros.clienteId()));
            }
            if (filtros.activo() != null) {
                predicates.add(builder.equal(root.get("activo"), filtros.activo()));
            }
            if (filtros.registradoDesde() != null) {
                OffsetDateTime inicio = filtros.registradoDesde().atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(builder.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio));
            }
            if (filtros.registradoHasta() != null) {
                OffsetDateTime finExclusivo = filtros.registradoHasta().plusDays(1)
                        .atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(builder.lessThan(root.get("fechaCreacion"), finExclusivo));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
