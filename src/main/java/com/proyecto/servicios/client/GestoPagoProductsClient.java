package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProducts", url = "${gestopago.products.url}")
public interface GestoPagoProductsClient {

    @GetMapping("/sistema/service/getProductList.do")
    GestoPagoProductListResponse getProductList(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader(HttpHeaders.ACCEPT) String accept
    );
}