package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gestopago_transacciones")
@Getter
@Setter
public class GestoPagoTransaccion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upc", nullable = false, unique = true, length = 64)
    private String upc;

    @Column(name = "tipo_operacion", nullable = false, length = 30)
    private String tipoOperacion;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "monto_pago", precision = 19, scale = 4)
    private BigDecimal montoPago;

    @Column(name = "hora_local", nullable = false, length = 20)
    private String horaLocal;

    @Column(name = "unidad", nullable = false, length = 50)
    private String unidad;

    @Column(name = "id_tx", length = 100)
    private String idTx;

    @Column(name = "num_autorizacion", length = 100)
    private String numAutorizacion;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(name = "texto_respuesta", length = 500)
    private String textoRespuesta;

    @Column(name = "saldo", length = 50)
    private String saldo;

    @Column(name = "saldo_formateado", length = 50)
    private String saldoFormateado;

    @Column(name = "comision", length = 50)
    private String comision;

    @Column(name = "comision_formateada", length = 50)
    private String comisionFormateada;

    @Column(name = "fecha_transaccion", length = 50)
    private String fechaTransaccion;

    @Column(name = "pin", columnDefinition = "TEXT")
    private String pin;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado;

    @Column(name = "raw_request_payload", columnDefinition = "TEXT")
    private String rawRequestPayload;

    @Column(name = "raw_response_xml", columnDefinition = "TEXT")
    private String rawResponseXml;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
