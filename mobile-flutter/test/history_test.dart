import 'package:decimo_cuatri/core/network/api_client.dart';
import 'package:decimo_cuatri/core/storage/secure_session_storage.dart';
import 'package:decimo_cuatri/models/transaction_models.dart';
import 'package:decimo_cuatri/screens/history_screen.dart';
import 'package:decimo_cuatri/services/transaction_service.dart';
import 'package:decimo_cuatri/state/session_controller.dart';
import 'package:decimo_cuatri/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeHistoryTransactionService extends TransactionService {
  _FakeHistoryTransactionService()
      : super(apiClient: ApiClient(sessionStorage: SecureSessionStorage()));

  @override
  Future<List<PurchaseTransactionResult>> obtenerHistorialComprasExitosas({
    int? clienteId,
    String? numeroCuenta,
  }) async {
    return [
      PurchaseTransactionResult.fromJson({
        'upc': 'TX1A18913184248',
        'tipoOperacion': 'RECARGA',
        'idProducto': 582,
        'idServicio': 133,
        'nombreProducto': 'Recarga Telcel \$10',
        'exitoso': true,
        'estado': 'EXITOSA',
        'codigo': '01',
        'mensaje': 'Operacion realizada con exito',
        'idTx': '826364546',
        'numAutorizacion': '18596434',
        'monto': 10.0,
        'telefono': '5512345678',
        'numeroCuenta': '4229215075268914',
        'fecha': '29/Oct/2026 19:11:32',
      }),
    ];
  }

  @override
  Future<PurchaseTransactionResult> consultarDetallePorUpc(String upc) async {
    final items = await obtenerHistorialComprasExitosas();
    return items.first;
  }
}

void main() {
  testWidgets('Renderiza HistoryScreen con operaciones exitosas y muestra el detalle al tocar', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: Scaffold(
          body: HistoryScreen(
            sessionController: SessionController(),
            transactionService: _FakeHistoryTransactionService(),
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Operaciones Exitosas: 1'), findsOneWidget);
    expect(find.text('Recarga Telcel \$10'), findsOneWidget);

    await tester.tap(find.text('Recarga Telcel \$10'));
    await tester.pumpAndSettle();

    expect(find.text('¡Compra Exitosa!'), findsOneWidget);
    expect(find.text('18596434'), findsOneWidget);
  });
}

