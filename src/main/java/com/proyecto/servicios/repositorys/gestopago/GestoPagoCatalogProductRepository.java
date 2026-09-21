package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GestoPagoCatalogProductRepository extends JpaRepository<GestoPagoCatalogProduct, Long> {

    Optional<GestoPagoCatalogProduct> findByProductId(String productId);
    boolean existsByProductId(String productId);
}
