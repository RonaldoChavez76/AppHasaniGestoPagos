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
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
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
    private final GestoPagoTransaccionRegistroService registroService;
    private final CuentaRepository cuentaRepository;
    private final TwoFactorAuthService twoFactorAuthService;
    private final ObjectMapper objectMapper;

    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String apiKey;
    private final String defaultUnidad;
    private final boolean mockModeGlobal;
    private final boolean twoFactorRequired;

    @org.springframework.beans.factory.annotation.Autowired
    public GestoPagoTransactionServiceImpl(
            GestoPagoTransactionClient transactionClient,
            GestoPagoTokenService tokenService,
            GestoPagoCryptoService cryptoService,
            GestoPagoCatalogProductRepository catalogProductRepository,
            GestoPagoTransaccionRepository transaccionRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) GestoPagoTransaccionRegistroService registroService,
            CuentaRepository cuentaRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) TwoFactorAuthService twoFactorAuthService,
            ObjectMapper objectMapper,
            @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
            @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
            @Value("${gestopago.auth.api-key}") String apiKey,
            @Value("${gestopago.unidad:083}") String defaultUnidad,
            @Value("${gestopago.mock-mode:false}") boolean mockModeGlobal,
            @Value("${gestopago.security.2fa.required:true}") boolean twoFactorRequired) {
        this.transactionClient = transactionClient;
        this.tokenService = tokenService;
        this.cryptoService = cryptoService;
        this.catalogProductRepository = catalogProductRepository;
        this.transaccionRepository = transaccionRepository;
        this.registroService = registroService;
        this.cuentaRepository = cuentaRepository;
        this.twoFactorAuthService = twoFactorAuthService;
        this.objectMapper = objectMapper;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.apiKey = apiKey;
        this.defaultUnidad = defaultUnidad;
        this.mockModeGlobal = mockModeGlobal;
        this.twoFactorRequired = twoFactorRequired;
    }

    public GestoPagoTransactionServiceImpl(
            GestoPagoTransactionClient transactionClient,
            GestoPagoTokenService tokenService,
            GestoPagoCryptoService cryptoService,
            GestoPagoCatalogProductRepository catalogProductRepository,
            GestoPagoTransaccionRepository transaccionRepository,
            CuentaRepository cuentaRepository,
            TwoFactorAuthService twoFactorAuthService,
            ObjectMapper objectMapper,
            Integer idDistribuidor,
            String codigoDispositivo,
            String apiKey,
            String defaultUnidad,
            boolean mockModeGlobal,
            boolean twoFactorRequired) {
        this(transactionClient, tokenService, cryptoService, catalogProductRepository,
                transaccionRepository, null, cuentaRepository, twoFactorAuthService, objectMapper,
                idDistribuidor, codigoDispositivo, apiKey, defaultUnidad, mockModeGlobal, twoFactorRequired);
    }

    public GestoPagoTransactionServiceImpl(
            GestoPagoTransactionClient transactionClient,
            GestoPagoTokenService tokenService,
            GestoPagoCryptoService cryptoService,
            GestoPagoCatalogProductRepository catalogProductRepository,
            GestoPagoTransaccionRepository transaccionRepository,
            CuentaRepository cuentaRepository,
            ObjectMapper objectMapper,
            Integer idDistribuidor,
            String codigoDispositivo,
            String apiKey,
            String defaultUnidad,
            boolean mockModeGlobal) {
        this(transactionClient, tokenService, cryptoService, catalogProductRepository,
                transaccionRepository, null, cuentaRepository, null, objectMapper,
                idDistribuidor, codigoDispositivo, apiKey, defaultUnidad, mockModeGlobal, false);
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

        String upc = sanitizarOGenerarUpc(request.getUpc());
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank())
                ? request.getUnidad().trim()
                : defaultUnidad;
        String nombreProducto = obtenerNombreProducto(productoBd, request.getIdProducto());

        Cuenta cuentaADebitar;
        try {
            cuentaADebitar = validarYObtenerCuenta(request.getNumeroCuenta(), montoTransaccion);
            validarSegundoFactorSiAplica(cuentaADebitar, request.getCodigo2FA());
            validarReglaDuplicadoRecarga(upc);
        } catch (IllegalArgumentException | GestoPagoIntegrationException ex) {
            registrarRechazoLocal(
                    TipoOperacionGestoPago.RECARGA,
                    request.getIdProducto(),
                    request.getIdServicio(),
                    nombreProducto,
                    request.getTelefono(),
                    null,
                    montoTransaccion,
                    upc,
                    horaLocal,
                    unidad,
                    request.getNumeroCuenta(),
                    ex
            );
            throw ex;
        }

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(request.getTelefono().trim())
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        return ejecutarTransaccion(TipoOperacionGestoPago.RECARGA, payload, montoTransaccion,
                cuentaADebitar, request.getSimular(), nombreProducto);
    }

    @Override
    @Transactional
    public GestoPagoTransaccionResponse procesarPagoServicio(PagoServicioRequest request) {
        log.info("Iniciando procesamiento de pago de servicio: idProducto={}, idServicio={}, referencia={}, monto={}",
                request.getIdProducto(), request.getIdServicio(), request.getReferencia(), request.getMontoPago());

        GestoPagoCatalogProduct productoBd = verificarProductoEnBaseDeDatos(request.getIdProducto());

        String upc = sanitizarOGenerarUpc(request.getUpc());
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = (request.getUnidad() != null && !request.getUnidad().isBlank())
                ? request.getUnidad().trim()
                : defaultUnidad;
        String telefono = (request.getTelefono() != null && !request.getTelefono().isBlank())
                ? request.getTelefono().trim()
                : TELEFONO_FIJO_SERVICIO;
        String nombreProducto = obtenerNombreProducto(productoBd, request.getIdProducto());

        Cuenta cuentaADebitar;
        try {
            cuentaADebitar = validarYObtenerCuenta(request.getNumeroCuenta(), request.getMontoPago());
            validarSegundoFactorSiAplica(cuentaADebitar, request.getCodigo2FA());
            validarReglaDuplicadoServicio(upc, request.getIdServicio(), request.getMontoPago());
        } catch (IllegalArgumentException | GestoPagoIntegrationException ex) {
            registrarRechazoLocal(
                    TipoOperacionGestoPago.PAGO_SERVICIO,
                    request.getIdProducto(),
                    request.getIdServicio(),
                    nombreProducto,
                    telefono,
                    request.getReferencia(),
                    request.getMontoPago(),
                    upc,
                    horaLocal,
                    unidad,
                    request.getNumeroCuenta(),
                    ex
            );
            throw ex;
        }

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
                cuentaADebitar, request.getSimular(), nombreProducto);
    }

    @Override
    @Transactional
    public GestoPagoTransaccionResponse procesarTransaccion(TransaccionGestoPagoRequest request) {
        TipoOperacionGestoPago tipo = determinarTipoOperacion(request);

        if (tipo == TipoOperacionGestoPago.PAGO_SERVICIO) {
            try {
                if (request.getReferencia() == null || request.getReferencia().isBlank()) {
                    throw new IllegalArgumentException("La referencia es requerida para pagos de servicios");
                }
                if (request.getMontoPago() == null || request.getMontoPago().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("El montoPago debe ser mayor a 0 para pagos de servicios");
                }
            } catch (IllegalArgumentException ex) {
                GestoPagoCatalogProduct productoBd = verificarProductoEnBaseDeDatos(request.getIdProducto());
                registrarRechazoLocal(
                        TipoOperacionGestoPago.PAGO_SERVICIO,
                        request.getIdProducto(),
                        request.getIdServicio(),
                        obtenerNombreProducto(productoBd, request.getIdProducto()),
                        request.getTelefono() != null && !request.getTelefono().isBlank()
                                ? request.getTelefono().trim()
                                : TELEFONO_FIJO_SERVICIO,
                        request.getReferencia(),
                        request.getMontoPago(),
                        sanitizarOGenerarUpc(request.getUpc()),
                        LocalDateTime.now().format(HORA_LOCAL_FORMATTER),
                        request.getUnidad() != null && !request.getUnidad().isBlank()
                                ? request.getUnidad().trim()
                                : defaultUnidad,
                        request.getNumeroCuenta(),
                        ex
                );
                throw ex;
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
                    .codigo2FA(request.getCodigo2FA())
                    .build();
            return procesarPagoServicio(servicioReq);
        } else {
            if (request.getTelefono() == null || !request.getTelefono().matches("^[0-9]{10}$")) {
                IllegalArgumentException ex = new IllegalArgumentException(
                        "El teléfono debe contener 10 dígitos numéricos para recargas");
                GestoPagoCatalogProduct productoBd = verificarProductoEnBaseDeDatos(request.getIdProducto());
                registrarRechazoLocal(
                        TipoOperacionGestoPago.RECARGA,
                        request.getIdProducto(),
                        request.getIdServicio(),
                        obtenerNombreProducto(productoBd, request.getIdProducto()),
                        request.getTelefono() != null && !request.getTelefono().isBlank()
                                ? request.getTelefono().trim()
                                : "0000000000",
                        request.getReferencia(),
                        request.getMontoPago(),
                        sanitizarOGenerarUpc(request.getUpc()),
                        LocalDateTime.now().format(HORA_LOCAL_FORMATTER),
                        request.getUnidad() != null && !request.getUnidad().isBlank()
                                ? request.getUnidad().trim()
                                : defaultUnidad,
                        request.getNumeroCuenta(),
                        ex
                );
                throw ex;
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
                    .codigo2FA(request.getCodigo2FA())
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

    @Override
    @Transactional(readOnly = true)
    public List<GestoPagoTransaccionResponse> listarComprasExitosasPorUsuario(Long clienteId, String numeroCuenta) {
        return listarPorFiltro(clienteId, numeroCuenta, FiltroHistorial.SOLO_EXITOSAS);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestoPagoTransaccionResponse> listarHistorialCompletoPorUsuario(Long clienteId, String numeroCuenta) {
        return listarPorFiltro(clienteId, numeroCuenta, FiltroHistorial.COMPLETO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GestoPagoTransaccionResponse> listarComprasRechazadasPorUsuario(Long clienteId, String numeroCuenta) {
        return listarPorFiltro(clienteId, numeroCuenta, FiltroHistorial.SOLO_RECHAZADAS);
    }

    private enum FiltroHistorial {
        SOLO_EXITOSAS,
        COMPLETO,
        SOLO_RECHAZADAS
    }

    private List<GestoPagoTransaccionResponse> listarPorFiltro(
            Long clienteId, String numeroCuenta, FiltroHistorial filtro) {
        String estadoExitoso = EstadoTransaccion.EXITOSA.name();

        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            String cuentaLimpia = numeroCuenta.trim();
            List<GestoPagoTransaccion> filas = switch (filtro) {
                case SOLO_EXITOSAS -> transaccionRepository
                        .findByNumeroCuentaAndEstadoOrderByCreatedAtDesc(cuentaLimpia, estadoExitoso);
                case COMPLETO -> transaccionRepository
                        .findByNumeroCuentaOrderByCreatedAtDesc(cuentaLimpia);
                case SOLO_RECHAZADAS -> transaccionRepository
                        .findByNumeroCuentaAndEstadoNotOrderByCreatedAtDesc(cuentaLimpia, estadoExitoso);
            };
            return filas.stream().map(this::mapearEntidadARespuesta).toList();
        }

        if (clienteId != null && cuentaRepository != null) {
            List<String> cuentasCliente = cuentaRepository.findByClienteIdOrderByIdAsc(clienteId).stream()
                    .map(Cuenta::getNumeroCuenta)
                    .filter(num -> num != null && !num.isBlank())
                    .toList();
            if (cuentasCliente.isEmpty()) {
                return List.of();
            }
            List<GestoPagoTransaccion> filas = switch (filtro) {
                case SOLO_EXITOSAS -> transaccionRepository
                        .findByNumeroCuentaInAndEstadoOrderByCreatedAtDesc(cuentasCliente, estadoExitoso);
                case COMPLETO -> transaccionRepository
                        .findByNumeroCuentaInOrderByCreatedAtDesc(cuentasCliente);
                case SOLO_RECHAZADAS -> transaccionRepository
                        .findByNumeroCuentaInAndEstadoNotOrderByCreatedAtDesc(cuentasCliente, estadoExitoso);
            };
            return filas.stream().map(this::mapearEntidadARespuesta).toList();
        }

        List<GestoPagoTransaccion> filas = switch (filtro) {
            case SOLO_EXITOSAS -> transaccionRepository.findTop50ByEstadoOrderByCreatedAtDesc(estadoExitoso);
            case COMPLETO -> transaccionRepository.findTop50ByOrderByCreatedAtDesc();
            case SOLO_RECHAZADAS -> transaccionRepository.findTop50ByEstadoNotOrderByCreatedAtDesc(estadoExitoso);
        };
        return filas.stream().map(this::mapearEntidadARespuesta).toList();
    }

    private GestoPagoTransaccionResponse ejecutarTransaccion(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            BigDecimal montoOriginal,
            Cuenta cuentaADebitar,
            Boolean simularFlag,
            String nombreProducto) {

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
        String codigoFallback = null;
        String motivoRechazo = null;
        GestoPagoIntegrationException excepcionToken = null;

        boolean usarMock = mockModeGlobal || Boolean.TRUE.equals(simularFlag);

        if (usarMock) {
            log.info("Modo simulación (mock) activo para UPC={}. Generando respuesta simulada aprobada.", payload.getUpc());
            xmlResponse = generarMockResponse(tipoOperacion, payload, montoOriginal);
            estado = EstadoTransaccion.EXITOSA;
        } else {
            try {
                String token = obtenerTokenValido();
                String encryptedSigned = cryptoService.encrypt(jsonPayload);
                String random = System.currentTimeMillis() + "_rand";

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
                        motivoRechazo = construirMotivoRechazoXml(xmlResponse);
                    } else if (xmlResponse.esTimeout()) {
                        estado = EstadoTransaccion.TIMEOUT;
                        motivoRechazo = construirMotivoRechazoXml(xmlResponse);
                    } else {
                        estado = EstadoTransaccion.RECHAZADA;
                        motivoRechazo = construirMotivoRechazoXml(xmlResponse);
                    }
                } else {
                    estado = EstadoTransaccion.ERROR;
                    codigoFallback = "XML_VACIO";
                    motivoRechazo = "GestoPago devolvió una respuesta vacía sin confirmación.";
                }
            } catch (RetryableException ex) {
                log.error("Timeout de lectura (62s) en sendTx.do para UPC={}: {}", payload.getUpc(), ex.getMessage());
                estado = EstadoTransaccion.TIMEOUT;
                codigoFallback = "TIMEOUT";
                motivoRechazo = truncar(
                        "Tiempo de espera agotado (62s) al comunicarse con GestoPago" + sufijoDetalle(ex.getMessage()),
                        500);
            } catch (FeignException ex) {
                if (ex.status() == HttpStatus.FORBIDDEN.value() || ex.status() == HttpStatus.UNAUTHORIZED.value()) {
                    log.warn("Token de GestoPago expirado o rechazado (HTTP {}). Renovando token y reintentando sendTx...", ex.status());
                    try {
                        tokenService.renovarToken();
                        String nuevoToken = obtenerTokenValido();
                        String encryptedSigned = cryptoService.encrypt(jsonPayload);

                        MultiValueMap<String, String> retryForm = new LinkedMultiValueMap<>();
                        retryForm.add("random", System.currentTimeMillis() + "_rand");
                        retryForm.add("signed", encryptedSigned);

                        xmlResponse = transactionClient.sendTxForm("Bearer " + nuevoToken, apiKey, retryForm);
                        if (xmlResponse != null && xmlResponse.esExitosa()) {
                            estado = EstadoTransaccion.EXITOSA;
                        } else if (xmlResponse != null && xmlResponse.esDuplicada()) {
                            estado = EstadoTransaccion.DUPLICADA;
                            motivoRechazo = construirMotivoRechazoXml(xmlResponse);
                        } else if (xmlResponse != null) {
                            estado = xmlResponse.esTimeout() ? EstadoTransaccion.TIMEOUT : EstadoTransaccion.RECHAZADA;
                            motivoRechazo = construirMotivoRechazoXml(xmlResponse);
                        } else {
                            estado = EstadoTransaccion.ERROR;
                            codigoFallback = "XML_VACIO";
                            motivoRechazo = "GestoPago devolvió una respuesta vacía tras renovar token.";
                        }
                    } catch (GestoPagoIntegrationException retryTokenEx) {
                        estado = EstadoTransaccion.ERROR;
                        codigoFallback = "TOKEN_ERR";
                        motivoRechazo = truncar(retryTokenEx.getMessage(), 500);
                        excepcionToken = retryTokenEx;
                    } catch (Exception retryEx) {
                        log.error("Error en reintento tras renovación de token: {}", retryEx.getMessage(), retryEx);
                        estado = EstadoTransaccion.ERROR;
                        codigoFallback = "ERR_INT";
                        motivoRechazo = truncar(
                                "Error al reintentar la transacción con GestoPago" + sufijoDetalle(retryEx.getMessage()),
                                500);
                    }
                } else {
                    log.error("Error HTTP {} de GestoPago en sendTx: {}", ex.status(), ex.getMessage());
                    estado = EstadoTransaccion.ERROR;
                    codigoFallback = truncar("HTTP_" + ex.status(), 10);
                    motivoRechazo = truncar(
                            "Error HTTP " + ex.status() + " al comunicarse con GestoPago" + sufijoDetalle(ex.getMessage()),
                            500);
                }
            } catch (GestoPagoIntegrationException tokenEx) {
                log.error("Error al obtener token de GestoPago para UPC={}: {}", payload.getUpc(), tokenEx.getMessage());
                estado = EstadoTransaccion.ERROR;
                codigoFallback = "TOKEN_ERR";
                motivoRechazo = truncar(tokenEx.getMessage(), 500);
                excepcionToken = tokenEx;
            } catch (Exception ex) {
                log.error("Error inesperado consumiendo sendTx.do en GestoPago: {}", ex.getMessage(), ex);
                estado = EstadoTransaccion.ERROR;
                codigoFallback = "ERR_INT";
                motivoRechazo = truncar(
                        "Error al procesar la transacción con GestoPago" + sufijoDetalle(ex.getMessage()),
                        500);
            }
        }

        if (estado == EstadoTransaccion.EXITOSA) {
            motivoRechazo = null;
        } else if (motivoRechazo == null || motivoRechazo.isBlank()) {
            motivoRechazo = "La transacción no pudo completarse (estado: " + estado.name() + ").";
        }

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

        if (excepcionToken != null) {
            persistirFalloEjecucion(
                    tipoOperacion, payload, xmlResponse, estado, codigoFallback, motivoRechazo,
                    jsonPayload, rawXml, numeroCuentaVinculada, montoOriginal, nombreProducto);
            throw excepcionToken;
        }

        GestoPagoTransaccion entidad = guardarTransaccionEnBaseDeDatos(
                tipoOperacion, payload, xmlResponse, estado, codigoFallback, motivoRechazo,
                jsonPayload, rawXml, numeroCuentaVinculada, montoOriginal, nombreProducto);

        GestoPagoTransaccionResponse respuesta = construirRespuesta(entidad, xmlResponse, montoOriginal, nuevoSaldoCuenta);
        log.info("Transacción UPC={} procesada con estado={}, codigo={}",
                entidad.getUpc(), respuesta.getEstado(), respuesta.getCodigo());

        return respuesta;
    }

    private Cuenta validarYObtenerCuenta(String numeroCuenta, BigDecimal montoRequerido) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return null;
        }
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(numeroCuenta.trim())
                .orElseThrow(() -> new IllegalArgumentException("La cuenta " + numeroCuenta + " no existe"));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new IllegalArgumentException("La cuenta " + numeroCuenta + " no se encuentra activa");
        }
        if (cuenta.getCliente() != null && !cuenta.getCliente().isActivo()) {
            throw new IllegalArgumentException("El cliente titular de la cuenta " + numeroCuenta + " está inactivo.");
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

    private void validarSegundoFactorSiAplica(Cuenta cuenta, String codigo2FA) {
        if (cuenta == null || twoFactorAuthService == null) {
            return;
        }
        if (twoFactorRequired && (codigo2FA == null || codigo2FA.isBlank())) {
            throw new IllegalArgumentException(
                    "El código de verificación 2FA de 6 dígitos es obligatorio para autorizar transacciones de la cuenta " +
                            cuenta.getNumeroCuenta());
        }
        if (codigo2FA != null && !codigo2FA.isBlank()) {
            twoFactorAuthService.validarYConsumirCodigo(cuenta.getNumeroCuenta(), codigo2FA);
        }
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

    private GestoPagoTransaccion construirEntidadTransaccion(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            GestoPagoSendTxXmlResponse xmlResponse,
            EstadoTransaccion estado,
            String codigoFallback,
            String motivoRechazo,
            String rawRequest,
            String rawXml,
            String numeroCuenta,
            BigDecimal montoOriginal,
            String nombreProducto) {

        GestoPagoTransaccion entidad = new GestoPagoTransaccion();
        entidad.setUpc(payload.getUpc());
        entidad.setNumeroCuenta(numeroCuenta);
        entidad.setTipoOperacion(tipoOperacion.name());
        entidad.setIdProducto(payload.getIdProducto());
        entidad.setIdServicio(payload.getIdServicio());
        entidad.setNombreProducto(nombreProducto);
        entidad.setTelefono(payload.getTelefono());
        entidad.setReferencia(payload.getReferencia());
        entidad.setMontoPago(payload.getMontoPago() != null ? payload.getMontoPago() : montoOriginal);
        entidad.setHoraLocal(payload.getHoraLocal());
        entidad.setUnidad(payload.getUnidad());
        entidad.setEstado(estado.name());
        entidad.setRawRequestPayload(rawRequest);
        entidad.setRawResponseXml(rawXml);
        entidad.setMotivoRechazo(truncar(motivoRechazo, 500));

        if (xmlResponse != null) {
            entidad.setIdTx(xmlResponse.getIdTx());
            entidad.setNumAutorizacion(xmlResponse.getNumAutorizacion());
            entidad.setCodigoRespuesta(truncar(xmlResponse.obtenerCodigo(), 10));
            entidad.setTextoRespuesta(truncar(xmlResponse.obtenerTextoMensaje(), 500));
            entidad.setSaldo(xmlResponse.getSaldo());
            entidad.setSaldoFormateado(xmlResponse.getSaldoF());
            entidad.setComision(xmlResponse.getComision());
            entidad.setComisionFormateada(xmlResponse.getComisionF());
            entidad.setFechaTransaccion(xmlResponse.getFecha());
            entidad.setPin(xmlResponse.obtenerPin());
            entidad.setLegend(xmlResponse.obtenerLegend());
        } else {
            entidad.setCodigoRespuesta(truncar(codigoFallback, 10));
            entidad.setTextoRespuesta(truncar(motivoRechazo, 500));
        }

        return entidad;
    }

    private GestoPagoTransaccion guardarTransaccionEnBaseDeDatos(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            GestoPagoSendTxXmlResponse xmlResponse,
            EstadoTransaccion estado,
            String codigoFallback,
            String motivoRechazo,
            String rawRequest,
            String rawXml,
            String numeroCuenta,
            BigDecimal montoOriginal,
            String nombreProducto) {

        GestoPagoTransaccion entidad = construirEntidadTransaccion(
                tipoOperacion, payload, xmlResponse, estado, codigoFallback, motivoRechazo,
                rawRequest, rawXml, numeroCuenta, montoOriginal, nombreProducto);
        return transaccionRepository.save(entidad);
    }

    private void persistirFalloEjecucion(
            TipoOperacionGestoPago tipoOperacion,
            GestoPagoSendTxPayload payload,
            GestoPagoSendTxXmlResponse xmlResponse,
            EstadoTransaccion estado,
            String codigoFallback,
            String motivoRechazo,
            String rawRequest,
            String rawXml,
            String numeroCuenta,
            BigDecimal montoOriginal,
            String nombreProducto) {
        GestoPagoTransaccion entidad = construirEntidadTransaccion(
                tipoOperacion, payload, xmlResponse, estado, codigoFallback, motivoRechazo,
                rawRequest, rawXml, numeroCuenta, montoOriginal, nombreProducto);
        persistirEntidadAuditoria(entidad);
    }

    private void registrarRechazoLocal(
            TipoOperacionGestoPago tipoOperacion,
            Integer idProducto,
            Integer idServicio,
            String nombreProducto,
            String telefono,
            String referencia,
            BigDecimal montoPago,
            String upcSolicitado,
            String horaLocal,
            String unidad,
            String numeroCuenta,
            RuntimeException ex) {
        String codigoError = clasificarCodigoRechazoLocal(ex);
        EstadoTransaccion estado = "DUP_UPC".equals(codigoError)
                ? EstadoTransaccion.DUPLICADA
                : ("TOKEN_ERR".equals(codigoError) ? EstadoTransaccion.ERROR : EstadoTransaccion.RECHAZADA);

        String upcAuditoria = asegurarUpcUnicoRechazo(upcSolicitado);
        String cuentaLimpia = (numeroCuenta != null && !numeroCuenta.isBlank())
                ? truncar(numeroCuenta.trim(), 20)
                : null;
        String telefonoSeguro = (telefono != null && !telefono.isBlank())
                ? truncar(telefono.trim(), 30)
                : (tipoOperacion == TipoOperacionGestoPago.PAGO_SERVICIO ? TELEFONO_FIJO_SERVICIO : "0000000000");
        String motivo = truncar(
                ex.getMessage() != null && !ex.getMessage().isBlank()
                        ? ex.getMessage().trim()
                        : "Transacción rechazada durante la validación previa.",
                500);

        GestoPagoSendTxPayload payloadAudit = GestoPagoSendTxPayload.builder()
                .idProducto(idProducto != null ? idProducto : 0)
                .idServicio(idServicio != null ? idServicio : 0)
                .telefono(telefonoSeguro)
                .referencia(referencia != null && !referencia.isBlank() ? referencia.trim() : null)
                .montoPago(montoPago)
                .horaLocal(horaLocal)
                .upc(upcAuditoria)
                .unidad(unidad)
                .build();

        String rawRequest = null;
        try {
            rawRequest = objectMapper.writeValueAsString(payloadAudit);
        } catch (Exception ignored) {
        }

        GestoPagoTransaccion entidad = construirEntidadTransaccion(
                tipoOperacion, payloadAudit, null, estado, codigoError, motivo,
                rawRequest, null, cuentaLimpia, montoPago != null ? montoPago : BigDecimal.ZERO, nombreProducto);
        persistirEntidadAuditoria(entidad);
    }

    private void persistirEntidadAuditoria(GestoPagoTransaccion entidad) {
        try {
            if (registroService != null) {
                registroService.guardarEnTransaccionIndependiente(entidad);
            } else {
                transaccionRepository.save(entidad);
            }
        } catch (Exception ex) {
            log.warn("No fue posible guardar auditoría de rechazo local para UPC={}: {}",
                    entidad.getUpc(), ex.getMessage());
        }
    }

    private String asegurarUpcUnicoRechazo(String upcBase) {
        String limpio = upcBase != null ? upcBase.trim() : "";
        if (!limpio.isEmpty() && !transaccionRepository.existsByUpc(limpio)) {
            return truncar(limpio, 64);
        }
        String generado = "RJ" + Long.toHexString(System.currentTimeMillis()).toUpperCase()
                + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return generado.substring(0, 15);
    }

    private String clasificarCodigoRechazoLocal(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("no existe")) return "CTA_NO_EX";
        if (msg.contains("no se encuentra activa")) return "CTA_INACT";
        if (msg.contains("está inactivo")) return "CLI_INACT";
        if (msg.contains("Saldo insuficiente")) return "SALDO_INS";
        if (msg.contains("es obligatorio")) return "2FA_REQ";
        if (msg.contains("2FA")) return "2FA_INV";
        if (ex instanceof GestoPagoIntegrationException && msg.contains("duplicada")) return "DUP_UPC";
        if (ex instanceof GestoPagoIntegrationException && msg.contains("token")) return "TOKEN_ERR";
        return "DATOS_INV";
    }

    private String construirMotivoRechazoXml(GestoPagoSendTxXmlResponse xmlResponse) {
        String codigo = xmlResponse.obtenerCodigo();
        String texto = xmlResponse.obtenerTextoMensaje();
        boolean tieneCodigo = codigo != null && !codigo.isBlank();
        boolean tieneTexto = texto != null && !texto.isBlank();

        String detalle;
        if (tieneCodigo && tieneTexto) {
            detalle = "Rechazo de GestoPago (código " + codigo + "): " + texto.trim();
        } else if (tieneTexto) {
            detalle = texto.trim();
        } else if (tieneCodigo) {
            detalle = "Rechazo de GestoPago (código " + codigo + ").";
        } else {
            detalle = "Transacción rechazada por GestoPago sin detalle.";
        }
        return truncar(detalle, 500);
    }

    private String sufijoDetalle(String detail) {
        if (detail == null || detail.isBlank()) {
            return ".";
        }
        return ": " + detail.trim();
    }

    private String truncar(String valor, int maxLen) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maxLen ? valor : valor.substring(0, maxLen);
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
        String nombreProducto = resolverNombreProducto(entidad);
        String motivo = exitoso ? null : (entidad.getMotivoRechazo() != null
                ? entidad.getMotivoRechazo()
                : entidad.getTextoRespuesta());

        return GestoPagoTransaccionResponse.builder()
                .upc(entidad.getUpc())
                .tipoOperacion(TipoOperacionGestoPago.valueOf(entidad.getTipoOperacion()))
                .idProducto(entidad.getIdProducto())
                .idServicio(entidad.getIdServicio())
                .nombreProducto(nombreProducto)
                .exitoso(exitoso)
                .estado(EstadoTransaccion.valueOf(entidad.getEstado()))
                .codigo(entidad.getCodigoRespuesta())
                .mensaje(entidad.getTextoRespuesta() != null ? entidad.getTextoRespuesta() : "Transacción registrada")
                .motivoRechazo(motivo)
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
        boolean exitoso = EstadoTransaccion.EXITOSA.name().equals(entidad.getEstado());
        String motivo = exitoso ? null : (entidad.getMotivoRechazo() != null
                ? entidad.getMotivoRechazo()
                : entidad.getTextoRespuesta());

        return GestoPagoTransaccionResponse.builder()
                .upc(entidad.getUpc())
                .tipoOperacion(TipoOperacionGestoPago.valueOf(entidad.getTipoOperacion()))
                .idProducto(entidad.getIdProducto())
                .idServicio(entidad.getIdServicio())
                .nombreProducto(resolverNombreProducto(entidad))
                .exitoso(exitoso)
                .estado(EstadoTransaccion.valueOf(entidad.getEstado()))
                .codigo(entidad.getCodigoRespuesta())
                .mensaje(entidad.getTextoRespuesta())
                .motivoRechazo(motivo)
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

    private String obtenerNombreProducto(GestoPagoCatalogProduct productoBd, Integer idProducto) {
        if (productoBd != null && productoBd.getName() != null && !productoBd.getName().isBlank()) {
            return productoBd.getName();
        }
        return idProducto != null ? "Producto #" + idProducto : "Operación GestoPago";
    }

    private String resolverNombreProducto(GestoPagoTransaccion entidad) {
        if (entidad.getNombreProducto() != null && !entidad.getNombreProducto().isBlank()) {
            return entidad.getNombreProducto();
        }
        if (entidad.getIdProducto() != null && catalogProductRepository != null) {
            return catalogProductRepository.findByProductId(String.valueOf(entidad.getIdProducto()))
                    .map(GestoPagoCatalogProduct::getName)
                    .orElse("Producto #" + entidad.getIdProducto());
        }
        return entidad.getIdProducto() != null ? "Producto #" + entidad.getIdProducto() : "Operación GestoPago";
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
