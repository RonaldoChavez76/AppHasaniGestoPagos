package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoCatalogServiceImplTest {

    @Test
    void deberiaRetornarDatosLocalesCuandoElServicioExternoFalla() {
        GestoPagoXmlProductsClient productClient = Mockito.mock(GestoPagoXmlProductsClient.class);
        GestoPagoCatalogProductRepository productRepository = Mockito.mock(GestoPagoCatalogProductRepository.class);
        GestoPagoTokenService tokenService = Mockito.mock(GestoPagoTokenService.class);

        GestoPagoCatalogProduct productoLocal = new GestoPagoCatalogProduct();
        productoLocal.setProductId("P-100");
        productoLocal.setCode("100");
        productoLocal.setName("Café");
        productoLocal.setPrice(new BigDecimal("120.00"));
        productoLocal.setActive(true);

        Mockito.when(productRepository.findAll()).thenReturn(List.of(productoLocal));

        GestoPagoCatalogServiceImpl service = new GestoPagoCatalogServiceImpl(
                productClient,
                productRepository,
                tokenService,
                "secret-key",
                83,
                "GPS83-TPV-17"
        );

        ResponseDTO<List<GestoPagoCatalogProduct>> response = service.consultarCatalogoDesdeBaseLocal();

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
        assertEquals("Respuesta desde caché local por fallo en servicio externo", response.getMensaje());
        assertFalse(response.getData().isEmpty());
    }

    @Test
    void deberiaDeserializarXmlRealDeGestoPago() throws Exception {
        String xml = "<RESPONSE>"
                + "<MENSAJE><CODIGO>0</CODIGO><TEXTO>OK</TEXTO></MENSAJE>"
                + "<PRODUCTOS>"
                + "<producto servicio=\"Mensual\" producto=\"Recarga\" idServicio=\"1\" idProducto=\"100\" idCatTipoServicio=\"10\" tipoFront=\"1\" hasDigitoVerificador=\"true\" precio=\"120.50\" showAyuda=\"false\" tipoReferencia=\"N\">"
                + "<legend><![CDATA[Texto de ayuda 1]]></legend>"
                + "</producto>"
                + "<producto servicio=\"Mensual\" producto=\"Pago\" idServicio=\"2\" idProducto=\"200\" idCatTipoServicio=\"11\" tipoFront=\"2\" hasDigitoVerificador=\"false\" precio=\"89.90\" showAyuda=\"true\" tipoReferencia=\"X\">"
                + "<legend><![CDATA[Texto de ayuda 2]]></legend>"
                + "</producto>"
                + "</PRODUCTOS>"
                + "</RESPONSE>";

        JAXBContext context = JAXBContext.newInstance(GestoPagoProductXmlResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        GestoPagoProductXmlResponse response = (GestoPagoProductXmlResponse) unmarshaller.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );

        assertNotNull(response);
        assertNotNull(response.getMensaje());
        assertEquals("0", response.getMensaje().getCodigo());
        assertEquals(2, response.getItems().size());
        assertEquals("Recarga", response.getItems().get(0).getProducto());
        assertEquals("Texto de ayuda 2", response.getItems().get(1).getLegend());
    }
}
