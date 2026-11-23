package com.proyecto.servicios.service.seguridad;

import com.proyecto.servicios.entity.seguridad.SesionUsuario;

import java.util.Optional;

public interface SesionUsuarioService {

    Optional<SesionUsuario> obtenerSesionActivaVigente(Long usuarioId);

    void verificarDisponibilidadOCerrar(Long usuarioId, String nuevaPlataforma, boolean forzarCierreSesion);

    SesionUsuario registrarNuevaSesion(
            Long usuarioId,
            Long clienteId,
            String sessionId,
            String plataforma,
            long duracionSegundos,
            boolean forzarCierreSesion
    );

    void cerrarSesion(Long usuarioId, String sessionId);

    void cerrarTodasLasSesionesActivas(Long usuarioId, String motivo);

    boolean esSesionValida(Long usuarioId, String sessionId);

    Optional<SesionUsuario> buscarPorSessionId(String sessionId);
}

