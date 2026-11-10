package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.seguridad.SesionUsuario;
import com.proyecto.servicios.repositorys.seguridad.SesionUsuarioRepository;
import com.proyecto.servicios.service.onboarding.SesionActivaExistenteException;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SesionUsuarioServiceImpl implements SesionUsuarioService {

    private final SesionUsuarioRepository sesionRepository;

    public SesionUsuarioServiceImpl(SesionUsuarioRepository sesionRepository) {
        this.sesionRepository = sesionRepository;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public Optional<SesionUsuario> obtenerSesionActivaVigente(Long usuarioId) {
        List<SesionUsuario> activas = sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(usuarioId);
        LocalDateTime ahora = LocalDateTime.now();
        SesionUsuario vigente = null;

        for (SesionUsuario sesion : activas) {
            if (sesion.getFechaExpiracion() != null && sesion.getFechaExpiracion().isBefore(ahora)) {
                sesion.setActiva(false);
                sesion.setFechaCierre(ahora);
                sesion.setMotivoCierre("EXPIRADA_AUTOMATICAMENTE");
                sesionRepository.save(sesion);
            } else if (vigente == null) {
                vigente = sesion;
            }
        }
        return Optional.ofNullable(vigente);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void verificarDisponibilidadOCerrar(Long usuarioId, String nuevaPlataforma, boolean forzarCierreSesion) {
        Optional<SesionUsuario> sesionActivaOpt = obtenerSesionActivaVigente(usuarioId);
        if (sesionActivaOpt.isEmpty()) {
            return;
        }

        SesionUsuario sesionActiva = sesionActivaOpt.get();
        if (!forzarCierreSesion) {
            log.warn("Conflicto de sesión única detectado para usuarioId={}. Sesión activa en plataforma={}",
                    usuarioId, sesionActiva.getPlataforma());
            throw new SesionActivaExistenteException(
                    sesionActiva.getPlataforma(),
                    sesionActiva.getFechaInicio(),
                    sesionActiva.getFechaExpiracion()
            );
        }

        cerrarTodasLasSesionesActivas(usuarioId, "REEMPLAZADA_POR_" + normalizarPlataforma(nuevaPlataforma));
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public SesionUsuario registrarNuevaSesion(
            Long usuarioId,
            Long clienteId,
            String sessionId,
            String plataforma,
            long duracionSegundos,
            boolean forzarCierreSesion
    ) {
        String plataformaNormalizada = normalizarPlataforma(plataforma);
        verificarDisponibilidadOCerrar(usuarioId, plataformaNormalizada, forzarCierreSesion);

        // Asegurar que cualquier sesión previa remanente quede cerrada
        cerrarTodasLasSesionesActivas(usuarioId, "NUEVA_SESION_" + plataformaNormalizada);

        LocalDateTime expiracion = LocalDateTime.now().plusSeconds(duracionSegundos > 0 ? duracionSegundos : 900);
        SesionUsuario nueva = new SesionUsuario(
                usuarioId,
                clienteId,
                sessionId,
                plataformaNormalizada,
                expiracion
        );
        SesionUsuario guardada = sesionRepository.save(nueva);
        log.info("Nueva sesión única registrada: usuarioId={}, sessionId={}, plataforma={}, expira={}",
                usuarioId, sessionId, plataformaNormalizada, expiracion);
        return guardada;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void cerrarSesion(Long usuarioId, String sessionId) {
        LocalDateTime ahora = LocalDateTime.now();
        if (sessionId != null && !sessionId.isBlank()) {
            sesionRepository.findBySessionId(sessionId.trim()).ifPresent(sesion -> {
                if (sesion.isActiva()) {
                    sesion.setActiva(false);
                    sesion.setFechaCierre(ahora);
                    sesion.setMotivoCierre("LOGOUT_USUARIO");
                    sesionRepository.save(sesion);
                }
            });
        }
        if (usuarioId != null) {
            cerrarTodasLasSesionesActivas(usuarioId, "LOGOUT_USUARIO");
        }
        log.info("Sesión cerrada correctamente para usuarioId={}, sessionId={}", usuarioId, sessionId);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public boolean esSesionValida(Long usuarioId, String sessionId) {
        if (usuarioId == null || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        Optional<SesionUsuario> sesionOpt = sesionRepository.findBySessionId(sessionId.trim());
        if (sesionOpt.isEmpty()) {
            return false;
        }
        SesionUsuario sesion = sesionOpt.get();
        if (!usuarioId.equals(sesion.getUsuarioId()) || !sesion.isActiva()) {
            return false;
        }
        if (sesion.getFechaExpiracion() != null && sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            sesion.setActiva(false);
            sesion.setFechaCierre(LocalDateTime.now());
            sesion.setMotivoCierre("EXPIRADA");
            sesionRepository.save(sesion);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public Optional<SesionUsuario> buscarPorSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return sesionRepository.findBySessionId(sessionId.trim());
    }

    private void cerrarTodasLasSesionesActivas(Long usuarioId, String motivo) {
        List<SesionUsuario> activas = sesionRepository.findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(usuarioId);
        LocalDateTime ahora = LocalDateTime.now();
        for (SesionUsuario s : activas) {
            s.setActiva(false);
            s.setFechaCierre(ahora);
            s.setMotivoCierre(motivo);
            sesionRepository.save(s);
        }
    }

    private String normalizarPlataforma(String plataforma) {
        return (plataforma == null || plataforma.isBlank()) ? "WEB" : plataforma.trim().toUpperCase();
    }
}

