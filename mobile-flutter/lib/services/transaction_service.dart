import '../core/network/api_client.dart';
import '../models/product_models.dart';
import '../models/transaction_models.dart';

class TransactionService {
  const TransactionService({required this.apiClient});

  final ApiClient apiClient;

  Future<String> solicitarCodigo2FAParaCompra(String numeroCuenta) async {
    final response = await apiClient.postJson(
      '/api/seguridad/2fa/solicitar',
      body: {'numeroCuenta': numeroCuenta.trim()},
    );
    final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
    return (data['codigoOtp'] ?? '').toString();
  }

  Future<PurchaseTransactionResult> procesarCompra({
    required CatalogProduct producto,
    required String numeroCuenta,
    required double monto,
    required String destino,
    String? codigo2FA,
    bool simular = true,
  }) async {
    try {
      final Map<String, dynamic> response;
      if (producto.esRecarga) {
        response = await apiClient.postJson(
          '/api/gestopago/transacciones/recarga',
          body: {
            'idProducto': producto.idProducto,
            'idServicio': producto.idServicio,
            'telefono': destino.trim(),
            'monto': monto,
            'numeroCuenta': numeroCuenta.trim(),
            if (codigo2FA != null && codigo2FA.trim().isNotEmpty) 'codigo2FA': codigo2FA.trim(),
            'simular': simular,
          },
        );
      } else {
        response = await apiClient.postJson(
          '/api/gestopago/transacciones/servicio',
          body: {
            'idProducto': producto.idProducto,
            'idServicio': producto.idServicio,
            'referencia': destino.trim(),
            'montoPago': monto,
            'numeroCuenta': numeroCuenta.trim(),
            if (codigo2FA != null && codigo2FA.trim().isNotEmpty) 'codigo2FA': codigo2FA.trim(),
            'simular': simular,
          },
        );
      }

      final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
      return PurchaseTransactionResult.fromJson(
        data,
        nombreProductoFallback: producto.nombre,
        idProductoFallback: producto.idProducto,
        idServicioFallback: producto.idServicio,
      );
    } on ApiException catch (e) {
      if (e.statusCode == 401) rethrow;
      if (e.statusCode == 504) {
        return PurchaseTransactionResult(
          upc: 'PENDIENTE-CONCILIACION',
          tipoOperacion: producto.esRecarga ? 'RECARGA' : 'PAGO_SERVICIO',
          idProducto: producto.idProducto,
          idServicio: producto.idServicio,
          nombreProducto: producto.nombre,
          exitoso: false,
          estadoRaw: 'TIMEOUT',
          estadoVisual: EstadoCompraResultado.pendiente,
          mensaje: e.message,
          monto: monto,
          numeroCuenta: numeroCuenta,
        );
      }
      if (e.statusCode == 503 || e.statusCode == 502 || e.statusCode >= 500) {
        return PurchaseTransactionResult(
          upc: 'ERROR-COMUNICACION',
          tipoOperacion: producto.esRecarga ? 'RECARGA' : 'PAGO_SERVICIO',
          idProducto: producto.idProducto,
          idServicio: producto.idServicio,
          nombreProducto: producto.nombre,
          exitoso: false,
          estadoRaw: 'ERROR',
          estadoVisual: EstadoCompraResultado.errorComunicacion,
          mensaje: e.message,
          monto: monto,
          numeroCuenta: numeroCuenta,
        );
      }
      return PurchaseTransactionResult(
        upc: 'RECHAZADA',
        tipoOperacion: producto.esRecarga ? 'RECARGA' : 'PAGO_SERVICIO',
        idProducto: producto.idProducto,
        idServicio: producto.idServicio,
        nombreProducto: producto.nombre,
        exitoso: false,
        estadoRaw: 'RECHAZADA',
        estadoVisual: EstadoCompraResultado.rechazada,
        mensaje: e.message,
        monto: monto,
        numeroCuenta: numeroCuenta,
      );
    }
  }

  Future<List<PurchaseTransactionResult>> obtenerHistorialComprasExitosas({
    int? clienteId,
    String? numeroCuenta,
  }) async {
    final queryParams = <String, String>{};
    if (clienteId != null) {
      queryParams['clienteId'] = clienteId.toString();
    }
    if (numeroCuenta != null && numeroCuenta.trim().isNotEmpty) {
      queryParams['numeroCuenta'] = numeroCuenta.trim();
    }

    try {
      final response = await apiClient.getJson(
        '/api/gestopago/transacciones/historial/exitosas',
        queryParameters: queryParams.isEmpty ? null : queryParams,
      );
      final data = response['data'];
      if (data is List) {
        return data
            .whereType<Map<String, dynamic>>()
            .map(PurchaseTransactionResult.fromJson)
            .where((tx) => tx.exitoso)
            .toList();
      }
    } on ApiException catch (e) {
      if (e.statusCode == 401) rethrow;
      if (e.statusCode != 404) rethrow;
    }

    final fallbackResp = await apiClient.getJson('/api/gestopago/transacciones');
    final data = fallbackResp['data'];
    if (data is List) {
      final cuentaLimpia = numeroCuenta?.trim();
      return data
          .whereType<Map<String, dynamic>>()
          .map(PurchaseTransactionResult.fromJson)
          .where((tx) {
            if (!tx.exitoso) return false;
            if (cuentaLimpia != null &&
                cuentaLimpia.isNotEmpty &&
                tx.numeroCuenta != null &&
                tx.numeroCuenta!.isNotEmpty) {
              return tx.numeroCuenta == cuentaLimpia;
            }
            return true;
          })
          .toList();
    }
    return const <PurchaseTransactionResult>[];
  }

  Future<PurchaseTransactionResult> consultarDetallePorUpc(String upc) async {
    final response = await apiClient.getJson('/api/gestopago/transacciones/${upc.trim()}');
    final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
    return PurchaseTransactionResult.fromJson(data);
  }
}
