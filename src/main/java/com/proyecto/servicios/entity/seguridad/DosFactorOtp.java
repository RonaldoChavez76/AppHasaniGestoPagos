package com.proyecto.servicios.entity.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seguridad_dos_factor_otp")
@Getter
@Setter
@NoArgsConstructor
public class DosFactorOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @Column(name = "codigo_otp", nullable = false, length = 10)
    private String codigoOtp;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "consumido", nullable = false)
    private boolean consumido = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DosFactorOtp(String numeroCuenta, String codigoOtp, LocalDateTime expiraEn) {
        this.numeroCuenta = numeroCuenta;
        this.codigoOtp = codigoOtp;
        this.expiraEn = expiraEn;
        this.consumido = false;
        this.createdAt = LocalDateTime.now();
    }
}
