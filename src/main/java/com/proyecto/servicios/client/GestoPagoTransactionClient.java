package com.proyecto.servicios.client;

import com.proyecto.servicios.config.FeignXmlConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxXmlResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoTransaction", url = "${gestopago.products.url}", configuration = FeignXmlConfig.class)
public interface GestoPagoTransactionClient {

    /**
     * Consume el endpoint sendTx.do enviando random y signed como cuerpo form-urlencoded.
     */
    @PostMapping(value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoSendTxXmlResponse sendTxForm(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody MultiValueMap<String, String> formParams
    );

    /**
     * Consume el endpoint sendTx.do enviando random y signed como parámetros de petición.
     */
    @PostMapping(value = "/sistema/service/sendTx.do",
            produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoSendTxXmlResponse sendTxParams(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed
    );
}
