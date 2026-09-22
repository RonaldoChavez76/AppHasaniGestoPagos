package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoCatalogServiceImpl implements GestoPagoCatalogService {

    private static final String REDIS_KEY = "catalogoGestopago::catalogo-v2";

    private final GestoPagoXmlProductsClient productsClient;
    private final GestoPagoCatalogProductRepository catalogRepository;
    private final GestoPagoTokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String apiKey;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;

    public GestoPagoCatalogServiceImpl(
            GestoPagoXmlProductsClient productsClient,
            GestoPagoCatalogProductRepository catalogRepository,
            GestoPagoTokenService tokenService,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${gestopago.auth.api-key}") String apiKey,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo
    ) {
        this.productsClient = productsClient;
        this.catalogRepository = catalogRepository;
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
        this.apiKey = apiKey;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
    }

    @Override
    @Transactional
    public void refreshCatalogFromExternalSource() {
        log.info("Iniciando actualización programada del catálogo GestoPago");

        try {
            String token = obtenerTokenActual();
            GestoPagoProductXmlResponse response = productsClient.getProductListXml(
                    "Bearer " + token,
                    apiKey,
                    "application/xml"
            );

            List<GestoPagoProductXmlItem> items = response != null ? response.getItems() : List.of();
            if (items.isEmpty()) {
                log.error("El catálogo externo llegó vacío. No se actualizará PostgreSQL ni Redis.");
                throw new IllegalStateException("Catalogo externo vacío");
            }

            List<GestoPagoCatalogProduct> productos = mapXmlToEntity(items);
            int sizeActual = catalogRepository.findAll().size();

            if (productos.size() < sizeActual) {
                log.warn("El tamaño del catálogo externo es menor al existente en BD. Se registrará la alerta y se actualizará la información.");
            }

            catalogRepository.deleteAll();
            catalogRepository.saveAll(productos);

            log.info("Catálogo GestoPago actualizado correctamente en PostgreSQL y Redis. Total: {}", productos.size());
        } catch (IllegalStateException ex) {
            log.error("Error de validación del catálogo externo: {}", ex.getMessage(), ex);
            throw ex;
        } catch (RetryableException ex) {
            log.error("Timeout al consultar el catálogo externo. Se conserva la información local actual.", ex);
        } catch (FeignException ex) {
            log.error("Error HTTP {} al consultar catálogo externo. Se conserva la información local actual.", ex.status(), ex);
        } catch (Exception ex) {
            log.error("Se produjo un error inesperado al serializar o persistir el catálogo. Los datos locales no se borran.", ex);
        }
    }

    @Override
    public ResponseDTO<List<GestoPagoCatalogProduct>> consultarCatalogo() {
        Object cachedValue = redisTemplate.opsForValue().get(REDIS_KEY);
        if (cachedValue instanceof ResponseDTO<?> cachedResponse
                && cachedResponse.getData() instanceof List<?> data
                && !data.isEmpty()) {
            log.info("Cache hit de Redis para la clave {}", REDIS_KEY);
            @SuppressWarnings("unchecked")
            ResponseDTO<List<GestoPagoCatalogProduct>> typedResponse =
                    (ResponseDTO<List<GestoPagoCatalogProduct>>) (ResponseDTO<?>) cachedResponse;
            return typedResponse;
        }

        log.info("Cache miss de Redis para la clave {}; consultando PostgreSQL o GestoPago", REDIS_KEY);
        List<GestoPagoCatalogProduct> cache = catalogRepository.findAll();
        if (!cache.isEmpty()) {
            return guardarEnCache(ResponseDTO.success(0, "Catálogo cargado desde PostgreSQL", cache));
        }

        try {
            String token = obtenerTokenActual();
            GestoPagoProductXmlResponse response = productsClient.getProductListXml(
                    "Bearer " + token,
                    apiKey,
                    "application/xml"
            );
            List<GestoPagoProductXmlItem> items = response != null ? response.getItems() : List.of();
            if (items.isEmpty()) {
                return ResponseDTO.failure(1, "Sin catálogo disponible en GestoPago");
            }
            List<GestoPagoCatalogProduct> productos = mapXmlToEntity(items);
            catalogRepository.saveAll(productos);
            return guardarEnCache(ResponseDTO.success(0, "Catálogo cargado desde GestoPago", productos));
        } catch (RetryableException ex) {
            log.error("Timeout externo al consultar catálogo. Se usa fallback local.", ex);
            List<GestoPagoCatalogProduct> local = catalogRepository.findAll();
            return guardarEnCache(ResponseDTO.success(0, "Catálogo cargado desde caché local por fallo en servicio externo", local));
        } catch (FeignException ex) {
            log.error("Fallo externo al consultar catálogo. Se usa fallback local.", ex);
            List<GestoPagoCatalogProduct> local = catalogRepository.findAll();
            return guardarEnCache(ResponseDTO.success(0, "Catálogo cargado desde caché local por fallo en servicio externo", local));
        }
    }

    private ResponseDTO<List<GestoPagoCatalogProduct>> guardarEnCache(
            ResponseDTO<List<GestoPagoCatalogProduct>> response) {
        if (response.getData() != null && !response.getData().isEmpty()) {
            redisTemplate.opsForValue().set(REDIS_KEY, response, Duration.ofMinutes(10));
            log.info("Catálogo guardado en Redis con la clave {}", REDIS_KEY);
        }
        return response;
    }

    @Override
    public ResponseDTO<List<GestoPagoCatalogProduct>> consultarCatalogoDesdeBaseLocal() {
        List<GestoPagoCatalogProduct> local = catalogRepository.findAll();
        if (local.isEmpty()) {
            return ResponseDTO.failure(1, "No hay información en la base local");
        }
        return ResponseDTO.success(0, "Respuesta desde caché local por fallo en servicio externo", local);
    }

    private List<GestoPagoCatalogProduct> mapXmlToEntity(List<GestoPagoProductXmlItem> items) {
        List<GestoPagoCatalogProduct> productos = new ArrayList<>();
        for (GestoPagoProductXmlItem item : items) {
            GestoPagoCatalogProduct entity = new GestoPagoCatalogProduct();
            entity.setProductId(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            entity.setCode(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            entity.setName(item.getProducto() != null ? item.getProducto() : item.getServicio());
            entity.setPrice(item.getPrecio());
            boolean activo = Boolean.TRUE.equals(item.getHasDigitoVerificador())
                    || Boolean.TRUE.equals(item.getShowAyuda())
                    || (item.getHasDigitoVerificador() == null && item.getShowAyuda() == null);
            entity.setActive(activo);
            productos.add(entity);
        }
        return productos;
    }

    private String obtenerTokenActual() {
        GestoPagoToken token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    tokenService.renovarToken();
                    return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo).orElse(null);
                });

        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new GestoPagoIntegrationException(
                    "No fue posible obtener el token de GestoPago para el catálogo", HttpStatus.UNAUTHORIZED, null);
        }
        return token.getToken();
    }
}
