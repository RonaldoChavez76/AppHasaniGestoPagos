package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoSendTxXmlMensaje {

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;

    @XmlElement(name = "REFERENCIA")
    private String referencia;

    @XmlElement(name = "PIN")
    private String pin;

    @XmlElement(name = "legend")
    private String legend;
}
