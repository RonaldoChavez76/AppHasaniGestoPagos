package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    List<SesionUsuario> findByUsuarioIdAndActivaTrueOrderByFechaInicioDesc(Long usuarioId);

    Optional<SesionUsuario> findBySessionId(String sessionId);

    Optional<SesionUsuario> findByUsuarioIdAndSessionIdAndActivaTrue(Long usuarioId, String sessionId);
}

