package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primer_nombre", nullable = false, length = 50)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 50)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 18, updatable = false)
    private String curp;

    @Column(nullable = false, length = 13, updatable = false)
    private String rfc;

    @Column(nullable = false, length = 20)
    private String sexo;

    @Column(nullable = false, length = 40)
    private String nacionalidad;

    @Column(name = "estado_civil", nullable = false, length = 30)
    private String estadoCivil;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(name = "telefono_movil", nullable = false, length = 10)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", length = 10)
    private String telefonoAlternativo;

    @Column(length = 80)
    private String ocupacion;

    @Column(length = 100)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @OneToOne(mappedBy = "cliente", fetch = FetchType.LAZY)
    private Domicilio domicilio;

    @OneToOne(mappedBy = "cliente", fetch = FetchType.LAZY)
    private Usuario usuario;

    @OneToMany(mappedBy = "cliente")
    private List<Cuenta> cuentas = new ArrayList<>();

    @PrePersist
    void alCrear() {
        OffsetDateTime ahora = OffsetDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
