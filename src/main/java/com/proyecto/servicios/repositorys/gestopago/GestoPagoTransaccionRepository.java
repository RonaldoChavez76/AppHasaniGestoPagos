package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GestoPagoTransaccionRepository extends JpaRepository<GestoPagoTransaccion, Long> {

    Optional<GestoPagoTransaccion> findByUpc(String upc);

    boolean existsByUpc(String upc);

    Optional<GestoPagoTransaccion> findTopByIdServicioAndMontoPagoAndCreatedAtAfterOrderByCreatedAtDesc(
            Integer idServicio, BigDecimal montoPago, LocalDateTime after);

    Optional<GestoPagoTransaccion> findTopByUpcAndCreatedAtAfterOrderByCreatedAtDesc(
            String upc, LocalDateTime after);

    List<GestoPagoTransaccion> findTop50ByOrderByCreatedAtDesc();
}
