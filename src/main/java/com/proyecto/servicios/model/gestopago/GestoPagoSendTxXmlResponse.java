package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoSendTxXmlResponse {

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "SALDO_F")
    private String saldoF;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "COMISION_F")
    private String comisionF;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "REFERENCIA")
    private String referenciaDirecta;

    @XmlElement(name = "PIN")
    private String pinDirecto;

    @XmlElement(name = "legend")
    private String legendDirecto;

    @XmlElement(name = "MENSAJE")
    private GestoPagoSendTxXmlMensaje mensaje;

    public String obtenerCodigo() {
        return mensaje != null && mensaje.getCodigo() != null ? mensaje.getCodigo().trim() : null;
    }

    public String obtenerTextoMensaje() {
        return mensaje != null ? mensaje.getTexto() : null;
    }

    public String obtenerReferencia() {
        if (mensaje != null && mensaje.getReferencia() != null && !mensaje.getReferencia().isBlank()) {
            return mensaje.getReferencia().trim();
        }
        return referenciaDirecta != null ? referenciaDirecta.trim() : null;
    }

    public String obtenerPin() {
        if (mensaje != null && mensaje.getPin() != null && !mensaje.getPin().isBlank()) {
            return mensaje.getPin().trim();
        }
        return pinDirecto != null ? pinDirecto.trim() : null;
    }

    public String obtenerLegend() {
        if (mensaje != null && mensaje.getLegend() != null && !mensaje.getLegend().isBlank()) {
            return mensaje.getLegend().trim();
        }
        return legendDirecto != null ? legendDirecto.trim() : null;
    }

    public boolean esExitosa() {
        return "01".equals(obtenerCodigo()) && !"-1".equals(numAutorizacion);
    }

    public boolean esDuplicada() {
        return "06".equals(obtenerCodigo());
    }

    public boolean esTimeout() {
        return "82".equals(obtenerCodigo());
    }
}
