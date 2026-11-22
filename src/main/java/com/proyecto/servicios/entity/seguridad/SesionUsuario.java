package com.proyecto.servicios.entity.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_usuario")
@Getter
@Setter
@NoArgsConstructor
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "plataforma", nullable = false, length = 20)
    private String plataforma = "WEB";

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "motivo_cierre", length = 100)
    private String motivoCierre;

    public SesionUsuario(
            Long usuarioId,
            Long clienteId,
            String sessionId,
            String plataforma,
            LocalDateTime fechaExpiracion
    ) {
        this.usuarioId = usuarioId;
        this.clienteId = clienteId;
        this.sessionId = sessionId;
        this.plataforma = (plataforma == null || plataforma.isBlank()) ? "WEB" : plataforma.trim().toUpperCase();
        this.activa = true;
        this.fechaInicio = LocalDateTime.now();
        this.fechaExpiracion = fechaExpiracion;
    }

    public boolean estaVigente() {
        return activa && fechaExpiracion != null && fechaExpiracion.isAfter(LocalDateTime.now());
    }
}

