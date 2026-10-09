package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.*;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteRequest;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.repositorys.onboarding.*;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import com.proyecto.servicios.service.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.service.onboarding.RegistroClienteService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RegistroClienteServiceImpl implements RegistroClienteService {

    private static final int MAYORIA_DE_EDAD = 18;
    private static final int MAX_INTENTOS_NUMERO_CUENTA = 5;
    private static final int LONGITUD_MAXIMA_BCRYPT_BYTES = 72;
    private static final BigDecimal SALDO_INICIAL = BigDecimal.ZERO;

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroClienteServiceImpl(
            ClienteRepository clienteRepository,
            DomicilioRepository domicilioRepository,
            UsuarioRepository usuarioRepository,
            CuentaRepository cuentaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public RegistroClienteResponse registrar(RegistroClienteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del registro son obligatorios.");
        }

        validarMayorEdad(request.fechaNacimiento());
        validarLongitudPassword(request.password());

        String curp = request.curp().trim().toUpperCase(Locale.ROOT);
        String rfc = request.rfc().trim().toUpperCase(Locale.ROOT);
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        validarUnicidad(curp, rfc, correo);

        Cliente cliente = crearCliente(request, curp, rfc, correo);
        Cliente clienteGuardado = clienteRepository.save(cliente);

        Domicilio domicilio = crearDomicilio(request.domicilio(), clienteGuardado);
        domicilioRepository.save(domicilio);

        Usuario usuario = new Usuario();
        usuario.setCliente(clienteGuardado);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setActivo(true);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(clienteGuardado);
        cuenta.setNumeroCuenta(generarNumeroCuentaDisponible());
        cuenta.setSaldo(SALDO_INICIAL);
        cuenta.setEstatus("ACTIVA");
        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        return new RegistroClienteResponse(
                clienteGuardado.getId(),
                usuarioGuardado.getId(),
                cuentaGuardada.getId(),
                cuentaGuardada.getNumeroCuenta(),
                correo
        );
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void darDeBaja(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(clienteId));
        cliente.setActivo(false);

        Usuario usuario = cliente.getUsuario();
        if (usuario != null) {
            usuario.setActivo(false);
        }

        for (Cuenta cuenta : cliente.getCuentas()) {
            if ("ACTIVA".equals(cuenta.getEstatus())) {
                cuenta.setEstatus("INACTIVA");
            }
        }
    }

    private void validarMayorEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null
                || fechaNacimiento.isAfter(LocalDate.now())
                || Period.between(fechaNacimiento, LocalDate.now()).getYears() < MAYORIA_DE_EDAD) {
            throw new IllegalArgumentException("El cliente debe tener al menos 18 años.");
        }
    }

    private void validarLongitudPassword(String password) {
        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT_BYTES) {
            throw new IllegalArgumentException("La contraseña excede el máximo admitido por BCrypt (72 bytes UTF-8).");
        }
    }

    private void validarUnicidad(String curp, String rfc, String correo) {
        if (clienteRepository.existsByCurp(curp)) {
            throw new ClienteDuplicadoException("CURP");
        }
        if (clienteRepository.existsByRfc(rfc)) {
            throw new ClienteDuplicadoException("RFC");
        }
        if (clienteRepository.existsByCorreo(correo)) {
            throw new ClienteDuplicadoException("correo electrónico");
        }
    }

    private Cliente crearCliente(RegistroClienteRequest request, String curp, String rfc, String correo) {
        Cliente cliente = new Cliente();
        cliente.setPrimerNombre(request.primerNombre().trim());
        cliente.setSegundoNombre(limpiarOpcional(request.segundoNombre()));
        cliente.setApellidoPaterno(request.apellidoPaterno().trim());
        cliente.setApellidoMaterno(limpiarOpcional(request.apellidoMaterno()));
        cliente.setFechaNacimiento(request.fechaNacimiento());
        cliente.setCurp(curp);
        cliente.setRfc(rfc);
        cliente.setSexo(request.sexo().trim());
        cliente.setNacionalidad(request.nacionalidad().trim());
        cliente.setEstadoCivil(request.estadoCivil().trim());
        cliente.setCorreo(correo);
        cliente.setTelefonoMovil(request.telefonoMovil());
        cliente.setTelefonoAlternativo(limpiarOpcional(request.telefonoAlternativo()));
        cliente.setOcupacion(limpiarOpcional(request.ocupacion()));
        cliente.setEmpresa(limpiarOpcional(request.empresa()));
        cliente.setIngresoMensual(request.ingresoMensual());
        cliente.setActivo(true);
        return cliente;
    }

    private Domicilio crearDomicilio(DomicilioRequest request, Cliente cliente) {
        Domicilio domicilio = new Domicilio();
        domicilio.setCliente(cliente);
        domicilio.setCalle(request.calle().trim());
        domicilio.setNumeroExterior(request.numeroExterior().trim());
        domicilio.setNumeroInterior(limpiarOpcional(request.numeroInterior()));
        domicilio.setColonia(request.colonia().trim());
        domicilio.setMunicipio(request.municipio().trim());
        domicilio.setEstado(request.estado().trim());
        domicilio.setCodigoPostal(request.codigoPostal());
        domicilio.setPais(request.pais().trim());
        return domicilio;
    }

    private String generarNumeroCuentaDisponible() {
        for (int intento = 0; intento < MAX_INTENTOS_NUMERO_CUENTA; intento++) {
            String numeroCuenta = ThreadLocalRandom.current().nextInt(1, 10)
                    + String.format("%015d", ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L));
            if (!cuentaRepository.existsByNumeroCuenta(numeroCuenta)) {
                return numeroCuenta;
            }
        }
        throw new IllegalStateException("No fue posible generar un número de cuenta disponible.");
    }

    private String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
