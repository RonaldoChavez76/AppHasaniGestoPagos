package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.onboarding.CuentaNoEncontradaException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaConsultaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    private CuentaConsultaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuentaConsultaServiceImpl(cuentaRepository);
    }

    @Test
    void obtieneCuentaYSaldoPorNumero() {
        Cuenta cuenta = cuentaExistente();
        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuenta));

        var response = service.obtenerPorNumero("1234567890123456");
        var saldo = service.obtenerSaldo("1234567890123456");

        assertEquals(1L, response.clienteId());
        assertEquals(new BigDecimal("125.50"), response.saldo());
        assertEquals("1234567890123456", saldo.numeroCuenta());
        assertEquals(new BigDecimal("125.50"), saldo.saldo());
    }

    @Test
    void listaSoloCuentasActivas() {
        when(cuentaRepository.findAllByEstatus(eq("ACTIVA"), any()))
                .thenReturn(new PageImpl<>(List.of(cuentaExistente())));

        var response = service.listarActivas(PageRequest.of(0, 20));

        assertEquals(1, response.getTotalElements());
        assertEquals("ACTIVA", response.getContent().get(0).estatus());
    }

    @Test
    void cuentaInexistenteProduceNoEncontrado() {
        when(cuentaRepository.findByNumeroCuenta("desconocida")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> service.obtenerPorNumero("desconocida"));
    }

    private Cuenta cuentaExistente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Cuenta cuenta = new Cuenta();
        cuenta.setId(2L);
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta("1234567890123456");
        cuenta.setSaldo(new BigDecimal("125.50"));
        cuenta.setEstatus("ACTIVA");
        return cuenta;
    }
}
