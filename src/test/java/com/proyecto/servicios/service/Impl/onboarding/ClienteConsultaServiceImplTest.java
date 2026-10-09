package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.model.onboarding.ActualizarClienteRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import com.proyecto.servicios.service.onboarding.ClienteNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteConsultaServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    private ClienteConsultaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClienteConsultaServiceImpl(clienteRepository);
    }

    @Test
    void actualizarModificaCamposPermitidosPeroConservaCurpYRfc() {
        Cliente cliente = clienteExistente();
        Domicilio domicilio = domicilioExistente(cliente);
        cliente.setDomicilio(domicilio);
        when(clienteRepository.findWithDomicilioById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCorreoAndIdNot("nuevo@example.com", 1L)).thenReturn(false);
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        var response = service.actualizar(1L, actualizacion(" NUEVO@Example.com "));

        assertEquals("CURPORIGINAL123456", response.curp());
        assertEquals("RFCORIGINAL123", response.rfc());
        assertEquals("nuevo@example.com", response.correo());
        assertEquals("Nombre Actualizado", response.primerNombre());
        assertEquals("Nueva calle", response.domicilio().calle());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void rechazaCorreoDuplicadoSinGuardarCambios() {
        Cliente cliente = clienteExistente();
        when(clienteRepository.findWithDomicilioById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.existsByCorreoAndIdNot("nuevo@example.com", 1L)).thenReturn(true);

        assertThrows(ClienteDuplicadoException.class, () -> service.actualizar(1L, actualizacion("nuevo@example.com")));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void rechazaActualizacionQueDejaAlClienteMenorDeEdad() {
        Cliente cliente = clienteExistente();
        when(clienteRepository.findWithDomicilioById(1L)).thenReturn(Optional.of(cliente));

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizar(1L, actualizacion("cliente@example.com", LocalDate.now().minusYears(17))));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void obtenerPorIdInexistenteProduceNoEncontrado() {
        when(clienteRepository.findWithDomicilioById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    private Cliente clienteExistente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setPrimerNombre("Nombre Original");
        cliente.setApellidoPaterno("Apellido");
        cliente.setFechaNacimiento(LocalDate.now().minusYears(30));
        cliente.setCurp("CURPORIGINAL123456");
        cliente.setRfc("RFCORIGINAL123");
        cliente.setCorreo("cliente@example.com");
        cliente.setIngresoMensual(new BigDecimal("10000.00"));
        cliente.setActivo(true);
        return cliente;
    }

    private Domicilio domicilioExistente(Cliente cliente) {
        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle("Calle anterior");
        domicilio.setNumeroExterior("1");
        domicilio.setColonia("Centro");
        domicilio.setMunicipio("Municipio");
        domicilio.setEstado("Estado");
        domicilio.setCodigoPostal("01234");
        domicilio.setPais("México");
        return domicilio;
    }

    private ActualizarClienteRequest actualizacion(String correo) {
        return actualizacion(correo, LocalDate.now().minusYears(30));
    }

    private ActualizarClienteRequest actualizacion(String correo, LocalDate fechaNacimiento) {
        return new ActualizarClienteRequest(
                "Nombre Actualizado",
                null,
                "Apellido",
                null,
                fechaNacimiento,
                "M",
                "Mexicana",
                "Soltero",
                correo,
                "5512345678",
                null,
                "Ocupación",
                "Empresa",
                new BigDecimal("20000.00"),
                new DomicilioRequest("Nueva calle", "10", null, "Colonia", "Municipio", "Estado", "54321", "México")
        );
    }
}
