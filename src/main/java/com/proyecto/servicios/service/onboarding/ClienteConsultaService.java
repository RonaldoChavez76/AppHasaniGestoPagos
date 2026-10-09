package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.onboarding.ActualizarClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

public interface ClienteConsultaService {

    Page<ClienteResponse> listarTodos(Pageable pageable);

    Page<ClienteResponse> listarActivos(Pageable pageable);

    Page<ClienteResponse> listarRegistradosEntre(LocalDate desde, LocalDate hasta, Pageable pageable);

    ClienteResponse obtenerPorId(Long id);

    ClienteResponse obtenerPorCurp(String curp);

    ClienteResponse obtenerPorRfc(String rfc);

    ClienteResponse obtenerPorCorreo(String correo);

    ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta);

    ClienteResponse actualizar(Long id, ActualizarClienteRequest request);
}
