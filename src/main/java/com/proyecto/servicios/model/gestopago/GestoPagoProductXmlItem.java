package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.*;

import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductXmlItem {

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "idServicio")
    private String idServicio;

    @XmlAttribute(name = "idProducto")
    private String idProducto;

    @XmlAttribute(name = "idCatTipoServicio")
    private String idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private String tipoFront;

    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @XmlAttribute(name = "precio")
    private BigDecimal precio;

    @XmlAttribute(name = "showAyuda")
    private Boolean showAyuda;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlElement(name = "legend")
    private String legend;

    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public String getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(String idServicio) {
        this.idServicio = idServicio;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(String idProducto) {
        this.idProducto = idProducto;
    }

    public String getIdCatTipoServicio() {
        return idCatTipoServicio;
    }

    public void setIdCatTipoServicio(String idCatTipoServicio) {
        this.idCatTipoServicio = idCatTipoServicio;
    }

    public String getTipoFront() {
        return tipoFront;
    }

    public void setTipoFront(String tipoFront) {
        this.tipoFront = tipoFront;
    }

    public Boolean getHasDigitoVerificador() {
        return hasDigitoVerificador;
    }

    public void setHasDigitoVerificador(Boolean hasDigitoVerificador) {
        this.hasDigitoVerificador = hasDigitoVerificador;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Boolean getShowAyuda() {
        return showAyuda;
    }

    public void setShowAyuda(Boolean showAyuda) {
        this.showAyuda = showAyuda;
    }

    public String getTipoReferencia() {
        return tipoReferencia;
    }

    public void setTipoReferencia(String tipoReferencia) {
        this.tipoReferencia = tipoReferencia;
    }

    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }
}
