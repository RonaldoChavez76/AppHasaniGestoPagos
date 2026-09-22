package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDto;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GestoPagoProductService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GestoPagoProductServiceImpl implements GestoPagoProductService {

    private final GestoPagoXmlProductsClient productsClient;
    private final GestoPagoTokenService tokenService;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String apiKey;

    public GestoPagoProductServiceImpl(
            GestoPagoXmlProductsClient productsClient,
            GestoPagoTokenService tokenService,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.auth.api-key}") String apiKey) {
        this.productsClient = productsClient;
        this.tokenService = tokenService;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.apiKey = apiKey;
    }

    @Override
    public GestoPagoProductListResponse obtenerProductos() {
        log.info("Iniciando consulta de productos en GestoPago");
        try {
            String token = obtenerToken();
                GestoPagoProductXmlResponse xmlResponse = productsClient.getProductListXml(
                    "Bearer " + token, apiKey, "application/xml");
                GestoPagoProductListResponse response = mapXmlResponse(xmlResponse);
            log.info("Consulta de productos en GestoPago finalizada correctamente");
            return response;
        } catch (RetryableException exception) {
            log.error("Timeout o error de comunicación al consultar productos en GestoPago");
            throw new GestoPagoIntegrationException("No fue posible comunicarse con GestoPago", HttpStatus.GATEWAY_TIMEOUT, exception);
        } catch (FeignException exception) {
            if (exception.status() == HttpStatus.UNAUTHORIZED.value()) {
            log.error("Error de autenticación al consultar productos en GestoPago");
                throw new GestoPagoIntegrationException("La autenticación con GestoPago fue rechazada", HttpStatus.UNAUTHORIZED, exception);
            }
            log.error("GestoPago respondió con estado HTTP {} al consultar productos", exception.status(), exception);
            throw new GestoPagoIntegrationException("GestoPago rechazó la consulta de productos", exception);
        } catch (Exception exception) {
            log.error("Error inesperado al consultar productos en GestoPago", exception);
            throw new GestoPagoIntegrationException("Error al consultar productos en GestoPago", exception);
        }
    }

    private String obtenerToken() {
        GestoPagoToken token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    tokenService.renovarToken();
                    return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo).orElse(null);
                });

        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new GestoPagoIntegrationException(
                    "No fue posible obtener el token de GestoPago", HttpStatus.UNAUTHORIZED, null);
        }
        return token.getToken();
    }

    private GestoPagoProductListResponse mapXmlResponse(GestoPagoProductXmlResponse xmlResponse) {
        GestoPagoProductListResponse response = new GestoPagoProductListResponse();
        if (xmlResponse == null) {
            return response;
        }

        if (xmlResponse.getMensaje() != null) {
            response.setCode(parseCode(xmlResponse.getMensaje().getCodigo()));
            response.setMessage(xmlResponse.getMensaje().getTexto());
        }

        ArrayNode products = JsonNodeFactory.instance.arrayNode();
        for (GestoPagoProductXmlItem item : xmlResponse.getItems()) {
            GestoPagoProductDto product = new GestoPagoProductDto();
            product.setId(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            product.setCode(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            product.setName(item.getProducto() != null ? item.getProducto() : item.getServicio());
            if (item.getPrecio() != null) {
                product.setPrice(JsonNodeFactory.instance.numberNode(item.getPrecio()));
            }
            products.add(JsonNodeFactory.instance.pojoNode(product));
        }
        response.setData(products);
        return response;
    }

    private Integer parseCode(String code) {
        try {
            return code == null ? null : Integer.valueOf(code);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}