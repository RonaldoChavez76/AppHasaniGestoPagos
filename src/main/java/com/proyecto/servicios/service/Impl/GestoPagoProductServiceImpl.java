package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
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

    private final GestoPagoProductsClient productsClient;
    private final GestoPagoTokenService tokenService;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String apiKey;

    public GestoPagoProductServiceImpl(
            GestoPagoProductsClient productsClient,
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
            GestoPagoProductListResponse response = productsClient.getProductList(
                    "Bearer " + token, apiKey, "application/json");
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
}