package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteRequestContractTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void crearValidador() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void cerrarValidador() throws Exception {
        validatorFactory.close();
    }

    @Test
    void busquedaAplicaValoresPredeterminadosYNormalizaFiltros() {
        var request = new BuscarClientesRequest("  r  ", " abcd010101abc ", null,
                null, null, null, null, null, null, null, null);

        assertEquals("r", request.nombre());
        assertEquals("ABCD010101ABC", request.rfc());
        assertEquals(0, request.page());
        assertEquals(20, request.size());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void busquedaAceptaFiltrosVaciosParaListarPaginado() {
        var request = new BuscarClientesRequest(null, null, null,
                null, null, null, null, null, null, 0, 20);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void busquedaNoExponeElResultadoInternoDeValidacionEnElJson() throws Exception {
        var request = new BuscarClientesRequest(null, null, null,
                null, null, null, null, null, null, 0, 20);

        String json = new ObjectMapper().writeValueAsString(request);

        assertFalse(json.contains("rangoFechasValido"));
    }

    @Test
    void busquedaRechazaFiltrosYValoresDePaginaInvalidos() {
        var request = new BuscarClientesRequest("R1", "RFC-INVALIDO", null,
                null, null, null, null, null, null, -1, 101);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void actualizacionValidaIdentificadorYDatosAnidados() {
        var request = new ActualizarClienteConIdRequest(0L, null);

        assertEquals(2, validator.validate(request).size());
    }

    @Test
    void consultaYBajaRequierenIdentificadoresPositivos() {
        assertTrue(validator.validate(new ConsultarClienteRequest(5L)).isEmpty());
        assertTrue(validator.validate(new DarDeBajaClienteRequest(5L)).isEmpty());
        assertEquals(1, validator.validate(new ConsultarClienteRequest(-1L)).size());
        assertEquals(1, validator.validate(new DarDeBajaClienteRequest(null)).size());
    }
}
