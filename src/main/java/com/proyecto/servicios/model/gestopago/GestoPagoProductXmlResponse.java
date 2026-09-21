package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProductXmlResponse {

    @XmlElement(name = "MENSAJE")
    private GestoPagoMensajeXml mensaje;

    @XmlElement(name = "PRODUCTOS")
    private GestoPagoProductosXml productos;

    public GestoPagoMensajeXml getMensaje() {
        return mensaje;
    }

    public void setMensaje(GestoPagoMensajeXml mensaje) {
        this.mensaje = mensaje;
    }

    public GestoPagoProductosXml getProductos() {
        return productos;
    }

    public void setProductos(GestoPagoProductosXml productos) {
        this.productos = productos;
    }

    public List<GestoPagoProductXmlItem> getItems() {
        if (productos == null) {
            return new ArrayList<>();
        }
        return productos.getProductos();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GestoPagoMensajeXml {

        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getTexto() {
            return texto;
        }

        public void setTexto(String texto) {
            this.texto = texto;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GestoPagoProductosXml {

        @XmlElement(name = "producto")
        private List<GestoPagoProductXmlItem> productos = new ArrayList<>();

        public List<GestoPagoProductXmlItem> getProductos() {
            return productos;
        }

        public void setProductos(List<GestoPagoProductXmlItem> productos) {
            this.productos = productos;
        }
    }
}
