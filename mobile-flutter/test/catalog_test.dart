import 'package:decimo_cuatri/core/network/api_client.dart';
import 'package:decimo_cuatri/core/storage/secure_session_storage.dart';
import 'package:decimo_cuatri/models/product_models.dart';
import 'package:decimo_cuatri/screens/catalog_screen.dart';
import 'package:decimo_cuatri/services/catalog_service.dart';
import 'package:decimo_cuatri/state/session_controller.dart';
import 'package:decimo_cuatri/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeCatalogService extends CatalogService {
  _FakeCatalogService()
      : super(apiClient: ApiClient(sessionStorage: SecureSessionStorage()));

  @override
  Future<List<CatalogProduct>> obtenerCatalogoProductos() async {
    return [
      CatalogProduct.fromJson({
        'idProducto': '582',
        'idServicio': '133',
        'producto': 'Recarga Telcel \$10',
        'servicio': 'Telcel',
        'tipoFront': '1',
        'precio': 10.0,
        'active': true,
      }),
      CatalogProduct.fromJson({
        'idProducto': '205',
        'idServicio': '76',
        'producto': 'Pago CFE Electricidad',
        'servicio': 'CFE',
        'tipoFront': '2',
        'precio': 0.0,
        'active': true,
      }),
    ];
  }
}

void main() {
  test('Clasifica y parsea productos del catalogo GestoPago correctamente', () {
    final recarga = CatalogProduct.fromJson({
      'idProducto': '582',
      'idServicio': '133',
      'producto': 'Recarga Telcel \$10',
      'servicio': 'Telcel',
      'tipoFront': '1',
      'precio': 10.0,
      'active': true,
    });

    expect(recarga.idProducto, 582);
    expect(recarga.idServicio, 133);
    expect(recarga.esRecarga, isTrue);
    expect(recarga.categoria, 'Recargas Telefónicas');
    expect(recarga.precio, 10.0);
  });

  testWidgets('Renderiza CatalogScreen con filtrado por categoria y busqueda', (tester) async {
    final sessionController = SessionController();
    final fakeCatalogService = _FakeCatalogService();

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: Scaffold(
          body: CatalogScreen(
            sessionController: sessionController,
            catalogService: fakeCatalogService,
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Recarga Telcel \$10'), findsOneWidget);
    expect(find.text('Pago CFE Electricidad'), findsOneWidget);

    await tester.tap(find.text('Pago de Servicios'));
    await tester.pumpAndSettle();

    expect(find.text('Pago CFE Electricidad'), findsOneWidget);
    expect(find.text('Recarga Telcel \$10'), findsNothing);
  });
}

