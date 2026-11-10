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
}

