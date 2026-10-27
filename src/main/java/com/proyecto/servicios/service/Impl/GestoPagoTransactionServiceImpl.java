package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransactionClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoCatalogProduct;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.gestopago.GestoPagoTransaccion;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.gestopago.*;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoCatalogProductRepository;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTransaccionRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.GestoPagoCryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GestoPagoTransactionService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class GestoPagoTransactionServiceImpl implements GestoPagoTransactionService {

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TELEFONO_FIJO_SERVICIO = "1111111111";

    private final GestoPagoTransactionClient transactionClient;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoCryptoService cryptoService;
    private final GestoPagoCatalogProductRepository catalogProductRepository;
    private final GestoPagoTransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final ObjectMapper objectMapper;

    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String apiKey;
    private final String defaultUnidad;
    private final boolean mockModeGlobal;

    public GestoPagoTransactionServiceImpl(
            GestoPagoTransactionClient transactionClient,
            GestoPagoTokenService tokenService,
            GestoPagoCryptoService cryptoService,
            GestoPagoCatalogProductRepository catalogProductRepository,
            GestoPagoTransaccionRepository transaccionRepository,
            CuentaRepository cuentaRepository,
            ObjectMapper objectMapper,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.auth.api-key}") String apiKey,
            @Value("${gestopago.unidad:083}") String defaultUnidad,
            @Value("${gestopago.mock-mode:false}") boolean mockModeGlobal) {
        this.transactionClient = transactionClient;
        this.tokenService = tokenService;
        this.cryptoService = cryptoService;
        this.catalogProductRepository = catalogProductRepository;
        this.transaccionRepository = transaccionRepository;
        this.cuentaRepository = cuentaRepository;
        this.objectMapper = objectMapper;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.apiKey = apiKey;
        this.defaultUnidad = defaultUnidad;
        this.mockModeGlobal = mockModeGlobal;
    }

    @Override
    @Transactional
    public GestoPagoTransaccionResponse procesarRecarga(RecargaTelefonicaRequest request) {
        log.info("Iniciando procesamiento de recarga telefónica: idProducto={}, idServicio={}, telefono={}",
                request.getIdProducto(), request.getIdServicio(), request.getTelefono());

        // 1. Verificación del producto en la base de datos PostgreSQL
        GestoPagoCatalogProduct productoBd = verificarProductoEnBaseDeDatos(request.getIdProducto());

        // Determinar monto: si no viene en request, tomar precio del catálogo
        BigDecimal montoTransaccion = request.getMonto();
        if (montoTransaccion == null && productoBd != null && productoBd.getPrice() != null) {
            montoTransaccion = productoBd.getPrice();
        }
        if (montoTransaccion == null) {
            montoTransaccion = BigDecimal.ZERO;
        }

        // 2. Validación de saldo en cuenta si se especificó numeroCuenta
        Cuenta cuentaADebitar = validarYObtenerCuenta(request.getNumeroCuenta(), montoTransaccion);

        // 3. Preparación y validación de parámetros de la recarga (tipoFront = 1)
        String upc = sanitizarOGenerarUpc(request.getUpc());
        validarReglaDuplicadoRecarga(upc);

        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank())
                ? request.getUnidad().trim()
                : defaultUnidad;

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(request.getTelefono().trim())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        return ejecutarTransaccion(TipoOperacionGestoPago.RECARGA, payload, montoTransaccion,
                cuentaADebitar, request.getSimular());
    }

    @Override
    @Transactional
    public GestoPagoTransaccionResponse procesarPagoServicio(PagoServicioRequest request) {
        log.info("Iniciando procesamiento de pago de servicio: idProducto={}, idServicio={}, referencia={}, monto={}",
                request.getIdProducto(), request.getIdServicio(), request.getReferencia(), request.getMontoPago());

        // 1. Verificación del producto/servicio en la base de datos PostgreSQL
        verificarProductoEnBaseDeDatos(request.getIdProducto());

        // 2. Validación de saldo en cuenta si se especificó numeroCuenta
        Cuenta cuentaADebitar = validarYObtenerCuenta(request.getNumeroCuenta(), request.getMontoPago());

        // 3. Preparación y validación de parámetros (tipoFront = 2 o gift card)
        String upc = sanitizarOGenerarUpc(request.getUpc());
        validarReglaDuplicadoServicio(upc, request.getIdServicio(), request.getMontoPago());

        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank())
                ? request.getUnidad().trim()
                : defaultUnidad;

        // Regla GestoPago: para servicios el teléfono se fija a "1111111111" a menos que venga especificado
        String telefono = (request.getTelefono() != null && !request.getTelefono().isBlank())
                ? request.getTelefono().trim()
                : TELEFONO_FIJO_SERVICIO;

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(telefono)
                .referencia(request.getReferencia().trim())
                .montoPago(request.getMontoPago())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        return ejecutarTransaccion(TipoOperacionGestoPago.PAGO_SERVICIO, payload, request.getMontoPago(),
                cuentaADebitar, request.getSimular());
    }

    @Override
    @Transactional
    public GestoPagoTransaccionResponse procesarTransaccion(TransaccionGestoPagoRequest request) {
        TipoOperacionGestoPago tipo = determinarTipoOperacion(request);

        if (tipo == TipoOperacionGestoPago.PAGO_SERVICIO) {
            if (request.getReferencia() == null || request.getReferencia().isBlank()) {
                throw new IllegalArgumentException("La referencia es requerida para pagos de servicios");
            }
            if (request.getMontoPago() == null || request.getMontoPago().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El montoPago debe ser mayor a 0 para pagos de servicios");
            }
            PagoServicioRequest servicioReq = PagoServicioRequest.builder()
                    .idProducto(request.getIdProducto())
                    .idServicio(request.getIdServicio())
                    .referencia(request.getReferencia())
                    .montoPago(request.getMontoPago())
                    .telefono(request.getTelefono())
                    .upc(request.getUpc())
                    .unidad(request.getUnidad())
                    .numeroCuenta(request.getNumeroCuenta())
                    .simular(request.getSimular())
                    .build();
            return procesarPagoServicio(servicioReq);
        } else {
            if (request.getTelefono() == null || !request.getTelefono().matches("^[0-9]{10}$")) {
                throw new IllegalArgumentException("El teléfono debe contener 10 dígitos numéricos para recargas");
            }
            RecargaTelefonicaRequest recargaReq = RecargaTelefonicaRequest.builder()
                    .idProducto(request.getIdProducto())
                    .idServicio(request.getIdServicio())
                    .telefono(request.getTelefono())
                    .monto(request.getMontoPago())
                    .upc(request.getUpc())
                    .unidad(request.getUnidad())
                    .numeroCuenta(request.getNumeroCuenta())
                    .simular(request.getSimular())
                    .build();
            return procesarRecarga(recargaReq);
        }
    }

    @Override
    public Optional<GestoPagoTransaccionResponse> consultarTransaccionPorUpc(String upc) {
        if (upc == null || upc.isBlank()) {
            return Optional.empty();
        }
        return transaccionRepository.findByUpc(upc.trim()).map(this::mapearEntidadARespuesta);
    }

    @Override
    public List<GestoPagoTransaccionResponse> listarUltimasTransacciones() {
        return transaccionRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::mapearEntidadARespuesta)
                .toList();
    }

    private GestoPagoTransaccionResponse ejecutarTransaccion(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            BigDecimal montoOriginal,
            Cuenta cuentaADebitar,
            Boolean simularFlag) {

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializando payload para sendTx: {}", e.getMessage());
            throw new GestoPagoIntegrationException("Error serializando transacción", e);
        }

        GestoPagoSendTxXmlResponse xmlResponse = null;
        EstadoTransaccion estado = EstadoTransaccion.ERROR;
        String rawXml = null;

        // Evaluar si opera en modo simulación (mock) para pruebas locales
        boolean usarMock = mockModeGlobal || Boolean.TRUE.equals(simularFlag);

        if (usarMock) {
            log.info("Modo simulación (mock) activo para UPC={}. Generando respuesta simulada aprobada.", payload.getUpc());
            xmlResponse = generarMockResponse(tipoOperacion, payload, montoOriginal);
            estado = EstadoTransaccion.EXITOSA;
        } else {
            String token = obtenerTokenValido();
            String encryptedSigned = cryptoService.encrypt(jsonPayload);
            String random = System.currentTimeMillis() + "_rand";

            try {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("random", random);
                form.add("signed", encryptedSigned);

                log.info("Enviando petición sendTx.do a GestoPago para UPC={}, tipoOperacion={}",
                        payload.getUpc(), tipoOperacion);

                xmlResponse = transactionClient.sendTxForm("Bearer " + token, apiKey, form);

                if (xmlResponse != null) {
                    if (xmlResponse.esExitosa()) {
                        estado = EstadoTransaccion.EXITOSA;
                    } else if (xmlResponse.esDuplicada()) {
                        estado = EstadoTransaccion.DUPLICADA;
                    } else if (xmlResponse.esTimeout()) {
                        estado = EstadoTransaccion.TIMEOUT;
                    } else {
                        estado = EstadoTransaccion.RECHAZADA;
                    }
                }
            } catch (RetryableException ex) {
                log.error("Timeout de lectura (62s) en sendTx.do para UPC={}: {}", payload.getUpc(), ex.getMessage());
                estado = EstadoTransaccion.TIMEOUT;
            } catch (FeignException ex) {
                if (ex.status() == HttpStatus.FORBIDDEN.value() || ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                    log.warn("Token de GestoPago expirado o rechazado (HTTP {}). Renovando token y reintentando sendTx...", ex.status());
                    tokenService.renovarToken();
                    String nuevoToken = obtenerTokenValido();

                    MultiValueMap<String, String> retryForm = new LinkedMultiValueMap<>();
                    retryForm.add("random", System.currentTimeMillis() + "_rand");
                    retryForm.add("signed", encryptedSigned);

                    try {
                        xmlResponse = transactionClient.sendTxForm("Bearer " + nuevoToken, apiKey, retryForm);
                        if (xmlResponse != null && xmlResponse.esExitosa()) {
                            estado = EstadoTransaccion.EXITOSA;
                        } else if (xmlResponse != null && xmlResponse.esDuplicada()) {
                            estado = EstadoTransaccion.DUPLICADA;
                        } else {
                            estado = EstadoTransaccion.RECHAZADA;
                        }
                    } catch (Exception retryEx) {
                        log.error("Error en reintento tras renovación de token: {}", retryEx.getMessage(), retryEx);
                        estado = EstadoTransaccion.ERROR;
                    }
                } else {
                    log.error("Error HTTP {} de GestoPago en sendTx: {}", ex.status(), ex.getMessage());
                    estado = EstadoTransaccion.ERROR;
                }
            } catch (Exception ex) {
                log.error("Error inesperado consumiendo sendTx.do en GestoPago: {}", ex.getMessage(), ex);
                estado = EstadoTransaccion.ERROR;
            }
        }

        // 4. Débito de saldo en PostgreSQL si la transacción fue EXITOSA y se vinculó una cuenta
        BigDecimal nuevoSaldoCuenta = null;
        String numeroCuentaVinculada = null;

        if (cuentaADebitar != null) {
            numeroCuentaVinculada = cuentaADebitar.getNumeroCuenta();
            if (estado == EstadoTransaccion.EXITOSA) {
                BigDecimal saldoPrevio = cuentaADebitar.getSaldo() != null ? cuentaADebitar.getSaldo() : BigDecimal.ZERO;
                nuevoSaldoCuenta = saldoPrevio.subtract(montoOriginal);
                cuentaADebitar.setSaldo(nuevoSaldoCuenta);
                cuentaRepository.save(cuentaADebitar);
                log.info("Saldo debitado exitosamente de la cuenta {}. Saldo previo: ${}, Nuevo saldo: ${}",
                        numeroCuentaVinculada, saldoPrevio, nuevoSaldoCuenta);
            } else {
                nuevoSaldoCuenta = cuentaADebitar.getSaldo();
                log.warn("La transacción no fue exitosa (estado={}). NO se descontó saldo de la cuenta {}. Saldo conservado: ${}",
                        estado, numeroCuentaVinculada, nuevoSaldoCuenta);
            }
        }

        // 5. Persistir la transacción en la base de datos PostgreSQL (Log evidence mandatorio por GestoPago)
        GestoPagoTransaccion entidad = guardarTransaccionEnBaseDeDatos(
                tipoOperacion, payload, xmlResponse, estado, jsonPayload, rawXml, numeroCuentaVinculada, montoOriginal);

        GestoPagoTransaccionResponse respuesta = construirRespuesta(entidad, xmlResponse, montoOriginal, nuevoSaldoCuenta);
        log.info("Transacción UPC={} procesada con estado={}, codigo={}",
                entidad.getUpc(), respuesta.getEstado(), respuesta.getCodigo());

        return respuesta;
    }

    private Cuenta validarYObtenerCuenta(String numeroCuenta, BigDecimal montoRequerido) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return null;
        }
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new IllegalArgumentException("La cuenta " + numeroCuenta + " no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new IllegalArgumentException("La cuenta " + numeroCuenta + " no se encuentra activa");
        }

        BigDecimal saldoActual = cuenta.getSaldo() != null ? cuenta.getSaldo() : BigDecimal.ZERO;
        if (montoRequerido != null && saldoActual.compareTo(montoRequerido) < 0) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente en la cuenta " + numeroCuenta +
                            ". Saldo disponible: $" + saldoActual +
                            ", Monto requerido: $" + montoRequerido);
        }

        log.info("Cuenta {} validada con saldo suficiente: ${} (Monto requerido: ${})",
                numeroCuenta, saldoActual, montoRequerido);
        return cuenta;
    }

    private GestoPagoSendTxXmlResponse generarMockResponse(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            BigDecimal montoOriginal) {

        GestoPagoSendTxXmlResponse xml = new GestoPagoSendTxXmlResponse();
        long randomId = 100000000L + (long) (Math.random() * 900000000L);
        long randomAuth = 10000000L + (long) (Math.random() * 90000000L);

        xml.setIdTx(String.valueOf(randomId));
        xml.setNumAutorizacion(String.valueOf(randomAuth));
        xml.setSaldo("157731.83");
        xml.setSaldoF("157,731.83");
        xml.setComision("0.70");
        xml.setComisionF("0.70");
        xml.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm:ss")));
        xml.setMonto(montoOriginal != null ? montoOriginal.toPlainString() : "10.00");

        GestoPagoSendTxXmlMensaje mensaje = new GestoPagoSendTxXmlMensaje();
        mensaje.setCodigo("01");
        mensaje.setTexto("Operacion realizada con exito (MODO SIMULACION LOCAL)");
        if (payload.getReferencia() != null) {
            mensaje.setReferencia(payload.getReferencia());
        }
        if (tipoOperacion == TipoOperacionGestoPago.PAGO_SERVICIO && payload.getTelefono() != null && payload.getTelefono().startsWith("#")) {
            mensaje.setPin("https://cutt.ly/SIMULACION-PIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            mensaje.setLegend("Conserve su comprobante de pago para cualquier aclaración.");
        }
        xml.setMensaje(mensaje);
        return xml;
    }

    private GestoPagoCatalogProduct verificarProductoEnBaseDeDatos(Integer idProducto) {
        if (idProducto == null) {
            return null;
        }
        Optional<GestoPagoCatalogProduct> productoBd =
                catalogProductRepository.findByProductId(String.valueOf(idProducto));

        if (productoBd.isPresent()) {
            GestoPagoCatalogProduct prod = productoBd.get();
            log.info("Producto verificado en base de datos PostgreSQL: id={}, nombre='{}', precio={}",
                    prod.getProductId(), prod.getName(), prod.getPrice());
            return prod;
        } else {
            log.warn("El producto idProducto={} no fue localizado en la tabla local gestopago_catalog_products. Se procederá a enviar la transacción a GestoPago.", idProducto);
            return null;
        }
    }

    private void validarReglaDuplicadoRecarga(String upc) {
        // Regla GestoPago: Recargas telefónicas se consideran duplicadas si se repite el mismo UPC en 15 minutos
        LocalDateTime limite = LocalDateTime.now().minusMinutes(15);
        Optional<GestoPagoTransaccion> existente =
                transaccionRepository.findTopByUpcAndCreatedAtAfterOrderByCreatedAtDesc(upc, limite);

        if (existente.isPresent() && EstadoTransaccion.EXITOSA.name().equals(existente.get().getEstado())) {
            throw new GestoPagoIntegrationException(
                    "Transacción duplicada: El UPC " + upc + " ya fue procesado en los últimos 15 minutos con autorización " +
                            existente.get().getNumAutorizacion(), HttpStatus.CONFLICT, null);
        }
    }

    private void validarReglaDuplicadoServicio(String upc, Integer idServicio, BigDecimal monto) {
        // Regla GestoPago: Pagos de servicio se consideran duplicados si el mismo UPC se recibe en 24h
        // O si la combinación mismo servicio y mismo monto se encuentra en las últimas 24 horas.
        LocalDateTime limite = LocalDateTime.now().minusHours(24);

        Optional<GestoPagoTransaccion> porUpc =
                transaccionRepository.findTopByUpcAndCreatedAtAfterOrderByCreatedAtDesc(upc, limite);
        if (porUpc.isPresent() && EstadoTransaccion.EXITOSA.name().equals(porUpc.get().getEstado())) {
            throw new GestoPagoIntegrationException(
                    "Transacción duplicada: El UPC " + upc + " ya fue aplicado en las últimas 24 horas (Autorización: " +
                            porUpc.get().getNumAutorizacion() + ")", HttpStatus.CONFLICT, null);
        }

        if (idServicio != null && monto != null) {
            Optional<GestoPagoTransaccion> porMonto =
                    transaccionRepository.findTopByIdServicioAndMontoPagoAndCreatedAtAfterOrderByCreatedAtDesc(
                            idServicio, monto, limite);
            if (porMonto.isPresent() && EstadoTransaccion.EXITOSA.name().equals(porMonto.get().getEstado())) {
                log.warn("Alerta de duplicado por regla de servicio+monto en 24h: servicio={}, monto={}", idServicio, monto);
            }
        }
    }

    private GestoPagoTransaccion guardarTransaccionEnBaseDeDatos(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            GestoPagoSendTxXmlResponse xmlResponse,
            EstadoTransaccion estado,
            String rawRequest,
            String rawXml,
            String numeroCuenta,
            BigDecimal montoOriginal) {

        GestoPagoTransaccion entidad = new GestoPagoTransaccion();
        entidad.setUpc(payload.getUpc());
        entidad.setNumeroCuenta(numeroCuenta);
        entidad.setTipoOperacion(tipoOperacion.name());
        entidad.setIdProducto(payload.getIdProducto());
        entidad.setIdServicio(payload.getIdServicio());
        entidad.setTelefono(payload.getTelefono());
        entidad.setReferencia(payload.getReferencia());
        entidad.setMontoPago(payload.getMontoPago() != null ? payload.getMontoPago() : montoOriginal);
        entidad.setHoraLocal(payload.getHoraLocal());
        entidad.setUnidad(payload.getUnidad());
        entidad.setEstado(estado.name());
        entidad.setRawRequestPayload(rawRequest);
        entidad.setRawResponseXml(rawXml);

        if (xmlResponse != null) {
            entidad.setIdTx(xmlResponse.getIdTx());
            entidad.setNumAutorizacion(xmlResponse.getNumAutorizacion());
            entidad.setCodigoRespuesta(xmlResponse.obtenerCodigo());
            entidad.setTextoRespuesta(xmlResponse.obtenerTextoMensaje());
            entidad.setSaldo(xmlResponse.getSaldo());
            entidad.setSaldoFormateado(xmlResponse.getSaldoF());
            entidad.setComision(xmlResponse.getComision());
            entidad.setComisionFormateada(xmlResponse.getComisionF());
            entidad.setFechaTransaccion(xmlResponse.getFecha());
            entidad.setPin(xmlResponse.obtenerPin());
            entidad.setLegend(xmlResponse.obtenerLegend());
        }

        return transaccionRepository.save(entidad);
    }

    private GestoPagoTransaccionResponse construirRespuesta(
            GestoPagoTransaccion entidad,
            GestoPagoSendTxXmlResponse xmlResponse,
            BigDecimal montoOriginal,
            BigDecimal nuevoSaldoCuenta) {

        BigDecimal montoFinal = montoOriginal;
        if (xmlResponse != null && xmlResponse.getMonto() != null) {
            try {
                montoFinal = new BigDecimal(xmlResponse.getMonto().trim().replace(",", ""));
            } catch (Exception ignored) {
            }
        }

        boolean exitoso = EstadoTransaccion.EXITOSA.name().equals(entidad.getEstado());

        return GestoPagoTransaccionResponse.builder()
                .upc(entidad.getUpc())
                .tipoOperacion(TipoOperacionGestoPago.valueOf(entidad.getTipoOperacion()))
                .exitoso(exitoso)
                .estado(EstadoTransaccion.valueOf(entidad.getEstado()))
                .codigo(entidad.getCodigoRespuesta())
                .mensaje(entidad.getTextoRespuesta() != null ? entidad.getTextoRespuesta() : "Transacción registrada")
                .idTx(entidad.getIdTx())
                .numAutorizacion(entidad.getNumAutorizacion())
                .monto(montoFinal)
                .saldo(entidad.getSaldo())
                .saldoFormateado(entidad.getSaldoFormateado())
                .comision(entidad.getComision())
                .comisionFormateada(entidad.getComisionFormateada())
                .fecha(entidad.getFechaTransaccion())
                .telefono(entidad.getTelefono())
                .referencia(entidad.getReferencia())
                .pin(entidad.getPin())
                .legend(entidad.getLegend())
                .numeroCuenta(entidad.getNumeroCuenta())
                .nuevoSaldoCuenta(nuevoSaldoCuenta)
                .fechaRegistro(entidad.getCreatedAt())
                .build();
    }

    private GestoPagoTransaccionResponse mapearEntidadARespuesta(GestoPagoTransaccion entidad) {
        return GestoPagoTransaccionResponse.builder()
                .upc(entidad.getUpc())
                .tipoOperacion(TipoOperacionGestoPago.valueOf(entidad.getTipoOperacion()))
                .exitoso(EstadoTransaccion.EXITOSA.name().equals(entidad.getEstado()))
                .estado(EstadoTransaccion.valueOf(entidad.getEstado()))
                .codigo(entidad.getCodigoRespuesta())
                .mensaje(entidad.getTextoRespuesta())
                .idTx(entidad.getIdTx())
                .numAutorizacion(entidad.getNumAutorizacion())
                .monto(entidad.getMontoPago())
                .saldo(entidad.getSaldo())
                .saldoFormateado(entidad.getSaldoFormateado())
                .comision(entidad.getComision())
                .comisionFormateada(entidad.getComisionFormateada())
                .fecha(entidad.getFechaTransaccion())
                .telefono(entidad.getTelefono())
                .referencia(entidad.getReferencia())
                .pin(entidad.getPin())
                .legend(entidad.getLegend())
                .numeroCuenta(entidad.getNumeroCuenta())
                .fechaRegistro(entidad.getCreatedAt())
                .build();
    }

    private TipoOperacionGestoPago determinarTipoOperacion(TransaccionGestoPagoRequest request) {
        if (request.getTipoOperacion() != null) {
            return request.getTipoOperacion();
        }
        if (Boolean.TRUE.equals(request.getEsServicio())) {
            return TipoOperacionGestoPago.PAGO_SERVICIO;
        }
        if (request.getReferencia() != null && !request.getReferencia().isBlank()) {
            return TipoOperacionGestoPago.PAGO_SERVICIO;
        }
        return TipoOperacionGestoPago.RECARGA;
    }

    private String sanitizarOGenerarUpc(String upcInput) {
        if (upcInput != null && !upcInput.isBlank()) {
            String upc = upcInput.trim();
            if (upc.length() > 15) {
                return upc.substring(0, 15);
            }
            return upc;
        }
        // Generar un código único de 15 caracteres alfanuméricos
        String raw = "TX" + Long.toHexString(System.currentTimeMillis()).toUpperCase() +
                UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return raw.substring(0, 15);
    }

    private String obtenerTokenValido() {
        GestoPagoToken token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .orElseGet(() -> {
                    tokenService.renovarToken();
                    return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo).orElse(null);
                });

        if (token == null || token.getToken() == null || token.getToken().isBlank()) {
            throw new GestoPagoIntegrationException(
                    "No fue posible obtener el token de autenticación de GestoPago", HttpStatus.UNAUTHORIZED, null);
        }
        return token.getToken();
    }
}
