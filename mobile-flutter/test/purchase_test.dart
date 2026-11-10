import 'package:decimo_cuatri/core/network/api_client.dart';
import 'package:decimo_cuatri/core/storage/secure_session_storage.dart';
import 'package:decimo_cuatri/models/product_models.dart';
import 'package:decimo_cuatri/models/transaction_models.dart';
import 'package:decimo_cuatri/screens/purchase_screen.dart';
import 'package:decimo_cuatri/services/transaction_service.dart';
import 'package:decimo_cuatri/state/session_controller.dart';
import 'package:decimo_cuatri/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeTransactionService extends TransactionService {
  _FakeTransactionService()
      : super(apiClient: ApiClient(sessionStorage: SecureSessionStorage()));

  @override
  Future<String> solicitarCodigo2FAParaCompra(String numeroCuenta) async {
    return '654321';
  }

  @override
  Future<PurchaseTransactionResult> procesarCompra({
    required CatalogProduct producto,
    required String numeroCuenta,
    required double monto,
    required String destino,
    String? codigo2FA,
    bool simular = true,
  }) async {
    return PurchaseTransactionResult.fromJson({
      'upc': 'TX1A18913184248',
      'tipoOperacion': 'RECARGA',
      'idProducto': producto.idProducto,
      'idServicio': producto.idServicio,
      'nombreProducto': producto.nombre,
      'exitoso': true,
      'estado': 'EXITOSA',
      'codigo': '01',
      'mensaje': 'Operacion realizada con exito',
      'idTx': '826364546',
      'numAutorizacion': '18596434',
      'monto': monto,
      'telefono': destino,
      'numeroCuenta': numeroCuenta,
      'nuevoSaldoCuenta': 4980.0,
      'fecha': '09/Nov/2026 19:25:00',
    });
  }
}

void main() {
  test('Distingue claramente entre compra exitosa, rechazada, pendiente y error de comunicacion', () {
    final exitosa = PurchaseTransactionResult.fromJson({
      'upc': 'UPC1',
      'estado': 'EXITOSA',
      'exitoso': true,
      'monto': 10.0,
    });
    final rechazada = PurchaseTransactionResult.fromJson({
      'upc': 'UPC2',
      'estado': 'RECHAZADA',
      'exitoso': false,
      'monto': 10.0,
    });
    final pendiente = PurchaseTransactionResult.fromJson({
      'upc': 'UPC3',
      'estado': 'TIMEOUT',
      'exitoso': false,
      'monto': 10.0,
    });
    final errorCom = PurchaseTransactionResult.fromJson({
      'upc': 'UPC4',
      'estado': 'ERROR',
      'exitoso': false,
      'monto': 10.0,
    });

    expect(exitosa.estadoVisual, EstadoCompraResultado.exitosa);
    expect(exitosa.exitoso, isTrue);
    expect(rechazada.estadoVisual, EstadoCompraResultado.rechazada);
    expect(rechazada.exitoso, isFalse);
    expect(pendiente.estadoVisual, EstadoCompraResultado.pendiente);
    expect(errorCom.estadoVisual, EstadoCompraResultado.errorComunicacion);
  });

  testWidgets('Ejecuta flujo de confirmacion de intencion de compra y muestra comprobante exitoso', (tester) async {
    final producto = CatalogProduct.fromJson({
      'idProducto': '582',
      'idServicio': '133',
      'producto': 'Recarga Telcel \$10',
      'servicio': 'Telcel',
      'tipoFront': '1',
      'precio': 10.0,
      'active': true,
    });

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: PurchaseScreen(
          producto: producto,
          sessionController: SessionController(),
          transactionService: _FakeTransactionService(),
        ),
      ),
    );

    final btnContinuar = find.text('Continuar a Confirmación');
    await tester.ensureVisible(btnContinuar);
    await tester.pumpAndSettle();
    await tester.tap(btnContinuar);
    await tester.pumpAndSettle();

    expect(find.text('Confirmar Intención de Compra'), findsOneWidget);

    await tester.tap(find.text('Confirmar y Pagar'));
    await tester.pumpAndSettle();

    expect(find.text('¡Compra Exitosa!'), findsOneWidget);
    expect(find.text('18596434'), findsOneWidget);
  });
}

