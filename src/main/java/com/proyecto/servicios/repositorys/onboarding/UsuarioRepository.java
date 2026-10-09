package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.onboarding.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCliente_Correo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    @EntityGraph(attributePaths = "cliente")
    Optional<Usuario> findWithClienteById(Long id);

    @Query("""
            select u.rol
            from Usuario u
            where u.id = :id and u.activo = true and u.cliente.activo = true
            """)
    Optional<RolUsuario> findRolActivaById(@Param("id") Long id);
}
