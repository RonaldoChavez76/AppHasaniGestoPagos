package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.model.onboarding.ActualizarClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.DomicilioResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.service.onboarding.ClienteConsultaService;
import com.proyecto.servicios.service.onboarding.ClienteDuplicadoException;
import com.proyecto.servicios.service.onboarding.ClienteNoEncontradoException;
import com.proyecto.servicios.service.onboarding.CuentaNoEncontradaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Period;
import java.util.Locale;

@Service
public class ClienteConsultaServiceImpl implements ClienteConsultaService {

    private final ClienteRepository clienteRepository;

    public ClienteConsultaServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public Page<ClienteResponse> listarTodos(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(this::respuesta);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public Page<ClienteResponse> listarActivos(Pageable pageable) {
        return clienteRepository.findAllByActivoTrue(pageable).map(this::respuesta);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public Page<ClienteResponse> listarRegistradosEntre(LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("El rango de fechas no es válido.");
        }
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finExclusivo = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return clienteRepository.findAllByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(
                inicio, finExclusivo, pageable).map(this::respuesta);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        return respuesta(clienteRepository.findWithDomicilioById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public ClienteResponse obtenerPorCurp(String curp) {
        return respuesta(clienteRepository.findWithDomicilioByCurp(curp.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("CURP", curp)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public ClienteResponse obtenerPorRfc(String rfc) {
        return respuesta(clienteRepository.findWithDomicilioByRfc(rfc.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("RFC", rfc)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public ClienteResponse obtenerPorCorreo(String correo) {
        return respuesta(clienteRepository.findWithDomicilioByCorreo(correo.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("correo", correo)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        return respuesta(clienteRepository.findDistinctWithDomicilioByCuentasNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta)));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public ClienteResponse actualizar(Long id, ActualizarClienteRequest request) {
        Cliente cliente = clienteRepository.findWithDomicilioById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (!correo.equals(cliente.getCorreo()) && clienteRepository.existsByCorreoAndIdNot(correo, id)) {
            throw new ClienteDuplicadoException("correo electrónico");
        }
        validarMayorEdad(request.fechaNacimiento());

        cliente.setPrimerNombre(request.primerNombre().trim());
        cliente.setSegundoNombre(limpiarOpcional(request.segundoNombre()));
        cliente.setApellidoPaterno(request.apellidoPaterno().trim());
        cliente.setApellidoMaterno(limpiarOpcional(request.apellidoMaterno()));
        cliente.setFechaNacimiento(request.fechaNacimiento());
        cliente.setSexo(request.sexo().trim());
        cliente.setNacionalidad(request.nacionalidad().trim());
        cliente.setEstadoCivil(request.estadoCivil().trim());
        cliente.setCorreo(correo);
        cliente.setTelefonoMovil(request.telefonoMovil());
        cliente.setTelefonoAlternativo(limpiarOpcional(request.telefonoAlternativo()));
        cliente.setOcupacion(limpiarOpcional(request.ocupacion()));
        cliente.setEmpresa(limpiarOpcional(request.empresa()));
        cliente.setIngresoMensual(request.ingresoMensual());
        actualizarDomicilio(cliente, request.domicilio());
        return respuesta(clienteRepository.save(cliente));
    }

    private void validarMayorEdad(LocalDate nacimiento) {
        if (nacimiento == null || nacimiento.isAfter(LocalDate.now())
                || Period.between(nacimiento, LocalDate.now()).getYears() < 18) {
            throw new IllegalArgumentException("El cliente debe tener al menos 18 años.");
        }
    }

    private void actualizarDomicilio(Cliente cliente, DomicilioRequest request) {
        Domicilio domicilio = cliente.getDomicilio();
        if (domicilio == null) {
            domicilio = new Domicilio();
            domicilio.setCliente(cliente);
        }
        domicilio.setCalle(request.calle().trim());
        domicilio.setNumeroExterior(request.numeroExterior().trim());
        domicilio.setNumeroInterior(limpiarOpcional(request.numeroInterior()));
        domicilio.setColonia(request.colonia().trim());
        domicilio.setMunicipio(request.municipio().trim());
        domicilio.setEstado(request.estado().trim());
        domicilio.setCodigoPostal(request.codigoPostal());
        domicilio.setPais(request.pais().trim());
        cliente.setDomicilio(domicilio);
    }

    private ClienteResponse respuesta(Cliente cliente) {
        Domicilio domicilio = cliente.getDomicilio();
        DomicilioResponse domicilioResponse = domicilio == null ? null : new DomicilioResponse(
                domicilio.getCalle(),
                domicilio.getNumeroExterior(),
                domicilio.getNumeroInterior(),
                domicilio.getColonia(),
                domicilio.getMunicipio(),
                domicilio.getEstado(),
                domicilio.getCodigoPostal(),
                domicilio.getPais()
        );
        return new ClienteResponse(
                cliente.getId(),
                cliente.getPrimerNombre(),
                cliente.getSegundoNombre(),
                cliente.getApellidoPaterno(),
                cliente.getApellidoMaterno(),
                cliente.getFechaNacimiento(),
                cliente.getCurp(),
                cliente.getRfc(),
                cliente.getSexo(),
                cliente.getNacionalidad(),
                cliente.getEstadoCivil(),
                cliente.getCorreo(),
                cliente.getTelefonoMovil(),
                cliente.getTelefonoAlternativo(),
                cliente.getOcupacion(),
                cliente.getEmpresa(),
                cliente.getIngresoMensual(),
                cliente.isActivo(),
                cliente.getFechaCreacion(),
                domicilioResponse
        );
    }

    private String limpiarOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
