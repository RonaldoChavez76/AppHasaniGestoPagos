package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoXmlProductsClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.ResponseDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoCatalogProductResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoLogoUrl;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlItem;
import com.proyecto.servicios.model.gestopago.GestoPagoProductXmlResponse;
import com.proyecto.servicios.model.onboarding.PageResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.Normalizer;
import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GestoPagoCatalogServiceImpl implements GestoPagoCatalogService {

    private static final String REDIS_KEY = "catalogoGestopago::catalogo-v4";

    private final GestoPagoXmlProductsClient productsClient;
    private final GestoPagoCatalogProductRepository catalogRepository;
    private final GestoPagoTokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String apiKey;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String logoBaseUrl;

    public GestoPagoCatalogServiceImpl(
            GestoPagoXmlProductsClient productsClient,
            GestoPagoCatalogProductRepository catalogRepository,
            GestoPagoTokenService tokenService,
            RedisTemplate<String, Object> redisTemplate,
            String apiKey,
            Integer idDistribuidor,
            String codigoDispositivo
    ) {
        this(
                productsClient,
                catalogRepository,
                tokenService,
                redisTemplate,
                apiKey,
                idDistribuidor,
                codigoDispositivo,
                GestoPagoLogoUrl.BASE_URL_DEFECTO
        );
    }

    @Autowired
    public GestoPagoCatalogServiceImpl(
            GestoPagoXmlProductsClient productsClient,
            GestoPagoCatalogProductRepository catalogRepository,
            GestoPagoTokenService tokenService,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${gestopago.auth.api-key}") String apiKey,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.products.logo-base-url:" + GestoPagoLogoUrl.BASE_URL_DEFECTO + "}") String logoBaseUrl
    ) {
        this.productsClient = productsClient;
        this.catalogRepository = catalogRepository;
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
        this.apiKey = apiKey;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.logoBaseUrl = logoBaseUrl;
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
            long sizeActual = catalogRepository.count();

            if (productos.size() < sizeActual) {
                log.warn("El tamaño del catálogo externo es menor al existente en BD. Se registrará la alerta y se actualizará la información.");
            }

            catalogRepository.deleteAllInBatch();
            catalogRepository.saveAll(productos);

            // Sincronizar inmediatamente en Redis para mantener los datos al día
            ResponseDTO<List<GestoPagoCatalogProduct>> cacheDto =
                    ResponseDTO.success(0, "Catálogo sincronizado desde GestoPago", productos);
            guardarEnCache(cacheDto);

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
    public ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogo() {
        return mapearRespuesta(consultarCatalogoDesdeCacheBaseOExterno());
    }

    @Override
    public ResponseDTO<PageResponse<GestoPagoCatalogProductResponse>> consultarCatalogoPaginado(
            int page,
            int size,
            String busqueda,
            String categoria,
            Boolean soloDisponibles
    ) {
        ResponseDTO<List<GestoPagoCatalogProductResponse>> base = consultarCatalogo();
        if (!Integer.valueOf(0).equals(base.getCodigo()) || base.getData() == null) {
            return ResponseDTO.failure(base.getCodigo(), base.getMensaje());
        }

        int paginaSolicitada = Math.max(0, page);
        int tamanoNormalizado = Math.max(1, Math.min(size, 60));
        String busquedaNorm = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        String categoriaNorm = normalizarTexto(categoria);
        boolean filtrarDisponibles = Boolean.TRUE.equals(soloDisponibles);

        List<GestoPagoCatalogProductResponse> filtrados = base.getData().stream()
                .filter(item -> !filtrarDisponibles || !Boolean.FALSE.equals(item.active()))
                .filter(item -> {
                    if (categoriaNorm.isEmpty() || "todos".equals(categoriaNorm)) {
                        return true;
                    }
                    return normalizarTexto(clasificarCategoria(item)).equals(categoriaNorm);
                })
                .filter(item -> {
                    if (busquedaNorm.isEmpty()) {
                        return true;
                    }
                    return contiene(item.name(), busquedaNorm)
                            || contiene(item.servicio(), busquedaNorm)
                            || contiene(item.productId(), busquedaNorm)
                            || contiene(item.code(), busquedaNorm)
                            || contiene(item.legend(), busquedaNorm)
                            || contiene(clasificarCategoria(item), busquedaNorm);
                })
                .toList();

        long totalElements = filtrados.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / tamanoNormalizado);
        int paginaAjustada = totalPages == 0 ? 0 : Math.min(paginaSolicitada, totalPages - 1);
        int fromIndex = paginaAjustada * tamanoNormalizado;
        int toIndex = Math.min(fromIndex + tamanoNormalizado, filtrados.size());
        List<GestoPagoCatalogProductResponse> contenido =
                fromIndex >= toIndex ? List.of() : List.copyOf(filtrados.subList(fromIndex, toIndex));

        PageResponse<GestoPagoCatalogProductResponse> pageResponse = new PageResponse<>(
                contenido,
                paginaAjustada,
                tamanoNormalizado,
                totalElements,
                totalPages,
                paginaAjustada == 0,
                totalPages == 0 || paginaAjustada >= totalPages - 1
        );
        return ResponseDTO.success(base.getCodigo(), base.getMensaje(), pageResponse);
    }

    private ResponseDTO<List<GestoPagoCatalogProduct>> consultarCatalogoDesdeCacheBaseOExterno() {
        // 1. NIVEL 1: Consultar Redis (Caché en memoria)
        ResponseDTO<List<GestoPagoCatalogProduct>> cached = consultarCacheRedis();
        if (cached != null) {
            log.info("Flujo [1/3] Cache HIT en Redis (clave: {}). Retornando {} productos desde memoria.",
                    REDIS_KEY, cached.getData().size());
            return cached;
        }
        log.info("Flujo [1/3] Cache MISS en Redis (clave: {}). Continuando con Nivel 2: PostgreSQL...", REDIS_KEY);

        // 2. NIVEL 2: Consultar PostgreSQL (Persistencia local)
        log.info("Flujo [2/3] Consultando base de datos PostgreSQL...");
        List<GestoPagoCatalogProduct> localProducts = catalogRepository.findAll();
        if (!localProducts.isEmpty()) {
            boolean requiereEnriquecimiento = localProducts.stream()
                    .noneMatch(p -> p.getIdServicio() != null && !p.getIdServicio().isBlank());
            if (requiereEnriquecimiento) {
                Optional<List<GestoPagoCatalogProduct>> enriquecidos = intentarEnriquecerDesdeExterno();
                if (enriquecidos.isPresent()) {
                    ResponseDTO<List<GestoPagoCatalogProduct>> updated =
                            ResponseDTO.success(0, "Catálogo enriquecido desde GestoPago", enriquecidos.get());
                    return guardarEnCache(updated);
                }
            }
            log.info("Flujo [2/3] Encontrados {} productos en PostgreSQL. Repoblando Redis y retornando.",
                    localProducts.size());
            ResponseDTO<List<GestoPagoCatalogProduct>> response =
                    ResponseDTO.success(0, "Catálogo cargado desde PostgreSQL", localProducts);
            return guardarEnCache(response);
        }
        log.info("Flujo [2/3] PostgreSQL vacío. Continuando con Nivel 3: API GestoPago...");

        // 3. NIVEL 3: Consultar API externa de GestoPago
        log.info("Flujo [3/3] Consultando API externa de GestoPago...");
        try {
            String token = obtenerTokenActual();
            GestoPagoProductXmlResponse response = productsClient.getProductListXml(
                    "Bearer " + token,
                    apiKey,
                    "application/xml"
            );
            List<GestoPagoProductXmlItem> items = response != null ? response.getItems() : List.of();
            if (items.isEmpty()) {
                log.warn("Flujo [3/3] La API de GestoPago no retornó productos.");
                return ResponseDTO.failure(1, "Sin catálogo disponible en GestoPago");
            }
            List<GestoPagoCatalogProduct> productos = mapXmlToEntity(items);
            catalogRepository.saveAll(productos);

            ResponseDTO<List<GestoPagoCatalogProduct>> successResponse =
                    ResponseDTO.success(0, "Catálogo cargado desde GestoPago", productos);
            log.info("Flujo [3/3] Éxito en API GestoPago. {} productos persistidos en PostgreSQL y guardados en Redis.",
                    productos.size());
            return guardarEnCache(successResponse);
        } catch (RetryableException ex) {
            log.error("Flujo [3/3] Timeout al consultar API de GestoPago. Activando fallback a base local...", ex);
            return responderConFallbackLocal("Catálogo cargado desde caché local por fallo en servicio externo");
        } catch (FeignException ex) {
            log.error("Flujo [3/3] Error HTTP {} al consultar API de GestoPago. Activando fallback a base local...",
                    ex.status(), ex);
            return responderConFallbackLocal("Catálogo cargado desde caché local por fallo en servicio externo");
        }
    }

    private ResponseDTO<List<GestoPagoCatalogProduct>> consultarCacheRedis() {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cachedValue instanceof ResponseDTO<?> cachedResponse
                    && cachedResponse.getData() instanceof List<?> data
                    && !data.isEmpty()) {
                @SuppressWarnings("unchecked")
                ResponseDTO<List<GestoPagoCatalogProduct>> typedResponse =
                        (ResponseDTO<List<GestoPagoCatalogProduct>>) (ResponseDTO<?>) cachedResponse;
                return typedResponse;
            }
        } catch (Exception ex) {
            log.warn("Flujo [1/3] Error de comunicación con Redis: '{}'. Se continúa resilentemente con PostgreSQL.",
                    ex.getMessage());
        }
        return null;
    }

    private ResponseDTO<List<GestoPagoCatalogProduct>> responderConFallbackLocal(String mensaje) {
        List<GestoPagoCatalogProduct> local = catalogRepository.findAll();
        if (local.isEmpty()) {
            log.error("Fallback fallido: no hay información disponible en la base de datos local.");
            return ResponseDTO.failure(1, "No hay información disponible localmente ni en servicio externo");
        }
        ResponseDTO<List<GestoPagoCatalogProduct>> fallbackResponse = ResponseDTO.success(0, mensaje, local);
        return guardarEnCache(fallbackResponse);
    }

    private ResponseDTO<List<GestoPagoCatalogProduct>> guardarEnCache(
            ResponseDTO<List<GestoPagoCatalogProduct>> response) {
        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            try {
                redisTemplate.opsForValue().set(REDIS_KEY, response, Duration.ofMinutes(10));
                log.info("Catálogo guardado en Redis con la clave {} (TTL: 10m)", REDIS_KEY);
            } catch (Exception ex) {
                log.warn("No fue posible guardar en Redis: '{}'. La respuesta continuará normalmente.",
                        ex.getMessage());
            }
        }
        return response;
    }

    @Override
    public ResponseDTO<List<GestoPagoCatalogProductResponse>> consultarCatalogoDesdeBaseLocal() {
        List<GestoPagoCatalogProduct> local = catalogRepository.findAll();
        if (local.isEmpty()) {
            return ResponseDTO.failure(1, "No hay información en la base local");
        }
        return mapearRespuesta(ResponseDTO.success(
                0, "Respuesta desde caché local por fallo en servicio externo", local));
    }

    private ResponseDTO<List<GestoPagoCatalogProductResponse>> mapearRespuesta(
            ResponseDTO<List<GestoPagoCatalogProduct>> response
    ) {
        if (response.getData() == null) {
            return ResponseDTO.failure(response.getCodigo(), response.getMensaje());
        }
        List<GestoPagoCatalogProductResponse> productos = response.getData().stream()
                .map(this::toResponse)
                .toList();
        return ResponseDTO.success(response.getCodigo(), response.getMensaje(), productos);
    }

    private GestoPagoCatalogProductResponse toResponse(GestoPagoCatalogProduct producto) {
        boolean activo = producto.getActive() == null || Boolean.TRUE.equals(producto.getActive());
        return new GestoPagoCatalogProductResponse(
                producto.getId(),
                producto.getProductId(),
                producto.getCode(),
                producto.getName(),
                producto.getPrice(),
                activo,
                producto.getCreatedAt(),
                producto.getUpdatedAt(),
                producto.getServicio(),
                producto.getIdServicio(),
                producto.getTipoFront(),
                producto.getLegend(),
                GestoPagoLogoUrl.construir(logoBaseUrl, producto.getIdServicio())
        );
    }

    private Optional<List<GestoPagoCatalogProduct>> intentarEnriquecerDesdeExterno() {
        try {
            String token = obtenerTokenActual();
            GestoPagoProductXmlResponse response = productsClient.getProductListXml(
                    "Bearer " + token,
                    apiKey,
                    "application/xml"
            );
            List<GestoPagoProductXmlItem> items = response != null ? response.getItems() : List.of();
            if (items.isEmpty()) {
                return Optional.empty();
            }
            List<GestoPagoCatalogProduct> productos = mapXmlToEntity(items);
            catalogRepository.deleteAllInBatch();
            catalogRepository.saveAll(productos);
            log.info("Catálogo local enriquecido automáticamente con {} productos desde GestoPago.", productos.size());
            return Optional.of(productos);
        } catch (Exception ex) {
            log.warn("No fue posible enriquecer el catálogo local desde GestoPago: '{}'. Se mantiene caché local.",
                    ex.getMessage());
            return Optional.empty();
        }
    }

    private String clasificarCategoria(GestoPagoCatalogProductResponse item) {
        String nombre = item.name() != null ? item.name() : "";
        String servicio = item.servicio() != null ? item.servicio() : "";
        String upper = (nombre + " " + servicio).toUpperCase(Locale.ROOT);
        if (upper.contains("NETFLIX") || upper.contains("SPOTIFY") || upper.contains("XBOX")
                || upper.contains("PLAYSTATION") || upper.contains("STEAM") || upper.contains("PIN")
                || upper.contains("CINEPOLIS") || upper.contains("AMAZON") || upper.contains("GOOGLE")) {
            return "Entretenimiento y Pines";
        }
        if ("1".equals(item.tipoFront())
                || upper.contains("RECARGA") || upper.contains("TIEMPO AIRE") || upper.contains("PAQUETE")
                || upper.contains("AMIGO") || upper.contains("TELCEL") || upper.contains("MOVISTAR")
                || upper.contains("AT&T") || upper.contains("UNEFON") || upper.contains("BAIT")
                || upper.contains("VIRGIN")) {
            return "Recargas Telefónicas";
        }
        return "Pago de Servicios";
    }

    private boolean contiene(String campo, String busquedaNorm) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(busquedaNorm);
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String descompuesto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private List<GestoPagoCatalogProduct> mapXmlToEntity(List<GestoPagoProductXmlItem> items) {
        List<GestoPagoCatalogProduct> productos = new ArrayList<>();
        for (GestoPagoProductXmlItem item : items) {
            GestoPagoCatalogProduct entity = new GestoPagoCatalogProduct();
            entity.setProductId(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            entity.setCode(item.getIdProducto() != null ? item.getIdProducto() : item.getIdServicio());
            entity.setName(item.getProducto() != null ? item.getProducto() : item.getServicio());
            entity.setPrice(item.getPrecio());
            entity.setActive(true);
            entity.setServicio(item.getServicio());
            entity.setIdServicio(item.getIdServicio());
            entity.setTipoFront(item.getTipoFront());
            entity.setLegend(item.getLegend() != null ? item.getLegend().trim() : null);
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
