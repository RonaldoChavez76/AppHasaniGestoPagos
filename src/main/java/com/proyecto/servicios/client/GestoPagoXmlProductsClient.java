package com.proyecto.servicios.client;

import com.proyecto.servicios.config.FeignXmlConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoXmlProducts", url = "${gestopago.products.url}", configuration = FeignXmlConfig.class)
public interface GestoPagoXmlProductsClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoProductXmlResponse getProductListXml(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader(HttpHeaders.ACCEPT) String accept
    );
}
