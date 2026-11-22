package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @Test
    void registroExigeApellidoMaternoOcupacionYEmpresa() {
        RegistroClienteRequest request = new RegistroClienteRequest(
                "Nombre", null, "Apellido", null, LocalDate.now().minusYears(30),
                "GODE561231HDFRRN09", "GODE561231GR8", "M", "Mexicana", "Soltero",
                "persona@example.com", "5512345678", null, "", null,
                new BigDecimal("25000.00"),
                new DomicilioRequest("Calle Uno", "123", null, "Centro", "Municipio",
                        "Estado", "01234", "México"),
                "ClaveSegura1!"
        );

        Set<String> camposInvalidos = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("apellidoMaterno", "ocupacion", "empresa"), camposInvalidos);
    }

    @Test
    void registroValidoNoPresentaViolaciones() {
        RegistroClienteRequest request = new RegistroClienteRequest(
                "Nombre", "Segundo", "Apellido", "Materno", LocalDate.now().minusYears(30),
                "GODE561231HDFRRN09", "GODE561231GR8", "M", "Mexicana", "Soltero",
                "persona@example.com", "5512345678", null, "Ingeniera", "Empresa",
                new BigDecimal("25000.00"),
                new DomicilioRequest("Calle Uno", "123", null, "Centro", "Municipio",
                        "Estado", "01234", "México"),
                "ClaveSegura1!"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void registroRecortaEspaciosExterioresAntesDeValidarLongitudes() {
        RegistroClienteRequest request = new RegistroClienteRequest(
                "  Ronaldo  ", "   ", "  Chávez  ", "  García  ",
                LocalDate.now().minusYears(30),
                " GODE561231HDFRRN09 ", " GODE561231GR8 ", " M ", " Mexicana ", " Soltero ",
                " persona@example.com ", " 5512345678 ", "   ", " Desarrollador ", " Empresa ",
                new BigDecimal("25000.00"),
                new DomicilioRequest(" Calle Uno ", " 123 ", "   ", " Centro ", " Municipio ",
                        " Estado ", " 01234 ", " México "),
                "ClaveSegura1!"
        );

        assertEquals("Ronaldo", request.primerNombre());
        assertEquals("Chávez", request.apellidoPaterno());
        assertNull(request.segundoNombre());
        assertNull(request.telefonoAlternativo());
        assertNull(request.domicilio().numeroInterior());
        assertEquals("Calle Uno", request.domicilio().calle());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void registroRechazaCamposObligatoriosVaciosOConSoloEspacios() {
        RegistroClienteRequest request = new RegistroClienteRequest(
                "   ", "   ", "   ", "   ", null, "   ", "   ", "   ", "   ", "   ",
                "   ", "   ", "   ", "   ", "   ", null, null,
                "   "
        );

        Set<String> camposInvalidos = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(camposInvalidos.containsAll(Set.of(
                "primerNombre", "apellidoPaterno", "apellidoMaterno", "fechaNacimiento",
                "curp", "rfc", "sexo", "nacionalidad", "estadoCivil", "correo",
                "telefonoMovil", "ocupacion", "empresa", "ingresoMensual", "domicilio", "password"
        )));
    }

    @Test
    void registroRechazaNombreQueExcedeLongitudTrasRecortar() {
        RegistroClienteRequest request = new RegistroClienteRequest(
                "  " + "A".repeat(51) + "  ", null, "Apellido", "Materno",
                LocalDate.now().minusYears(30),
                "GODE561231HDFRRN09", "GODE561231GR8", "M", "Mexicana", "Soltero",
                "persona@example.com", "5512345678", null, "Desarrollador", "Empresa",
                new BigDecimal("25000.00"),
                new DomicilioRequest("Calle Uno", "123", null, "Centro", "Municipio",
                        "Estado", "01234", "México"),
                "ClaveSegura1!"
        );

        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("primerNombre")));
    }

    @Test
    void actualizacionTambienExigeApellidoMaternoOcupacionYEmpresa() {
        ActualizarClienteRequest request = new ActualizarClienteRequest(
                "Nombre", null, "Apellido", "", LocalDate.now().minusYears(30),
                "M", "Mexicana", "Soltero", "persona@example.com", "5512345678",
                null, null, "", new BigDecimal("25000.00"),
                new DomicilioRequest("Calle Uno", "123", null, "Centro", "Municipio",
                        "Estado", "01234", "México")
        );

        Set<String> camposInvalidos = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("apellidoMaterno", "ocupacion", "empresa"), camposInvalidos);
    }

    @Test
    void actualizacionRecortaNombresYTrataOpcionalesEnBlancoComoNulos() {
        ActualizarClienteRequest request = new ActualizarClienteRequest(
                "  Ronaldo  ", "  ", "  Chávez  ", "  García  ",
                LocalDate.now().minusYears(30), " M ", " Mexicana ", " Soltero ",
                " persona@example.com ", " 5512345678 ", "  ", " Desarrollador ", " Empresa ",
                new BigDecimal("25000.00"),
                new DomicilioRequest(" Calle Uno ", " 123 ", "   ", " Centro ", " Municipio ",
                        " Estado ", " 01234 ", " México ")
        );

        assertEquals("Ronaldo", request.primerNombre());
        assertNull(request.segundoNombre());
        assertNull(request.telefonoAlternativo());
        assertNull(request.domicilio().numeroInterior());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void loginRecortaEspaciosDelCorreoPeroNoModificaLaPassword() {
        LoginRequest request = new LoginRequest(" persona@example.com ", " Clave1! ");

        assertEquals("persona@example.com", request.correo());
        assertEquals(" Clave1! ", request.password());
        assertTrue(validator.validate(request).isEmpty());
    }
}
