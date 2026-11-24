package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestoPagoTransaccionRegistroService {

    private final GestoPagoTransaccionRepository transaccionRepository;

    public GestoPagoTransaccionRegistroService(GestoPagoTransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional(transactionManager = "sfTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public GestoPagoTransaccion guardarEnTransaccionIndependiente(GestoPagoTransaccion entidad) {
        return transaccionRepository.saveAndFlush(entidad);
    }
}
