import 'dart:convert';

import 'package:decimo_cuatri/core/network/api_client.dart';
import 'package:decimo_cuatri/core/storage/secure_session_storage.dart';
import 'package:decimo_cuatri/models/product_models.dart';
import 'package:decimo_cuatri/services/auth_service.dart';
import 'package:decimo_cuatri/services/catalog_service.dart';
import 'package:decimo_cuatri/services/transaction_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  test('Escenario practico completo de Ana entre Movil y Web (6 pasos)', () async {
    String? plataformaActivaServidor;
    String? tokenActivoServidor;
    final historialServidor = <Map<String, dynamic>>[];

    final mockHttp = MockClient((request) async {
      final path = request.url.path;
      final authHeader = request.headers['Authorization'];

      if (path.endsWith('/auth/login/2fa/solicitar')) {
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        final plataformaSolicitada = body['plataforma'] as String? ?? 'MOVIL';
        final forzar = body['forzarCierreSesion'] as bool? ?? false;

        if (plataformaActivaServidor != null &&
            plataformaActivaServidor != plataformaSolicitada &&
            !forzar) {
          return http.Response.bytes(
            utf8.encode(
              jsonEncode({
                'success': false,
                'message': 'Ya existe una sesión activa en $plataformaActivaServidor.',
                'data': {
                  'sesionActivaExistente': true,
                  'plataformaActiva': plataformaActivaServidor,
                  'fechaInicio': '2026-11-09T19:00:00Z',
                  'accionRequerida':
                      'Debes finalizar la sesión anterior en $plataformaActivaServidor antes de iniciar una nueva.',
                },
              }),
            ),
            409,
          );
        }

        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': true,
              'message': 'Código 2FA generado',
              'data': {
                'requiere2FA': true,
                'correo': body['correo'],
                'numeroCuenta': '4229215075268914',
                'plataforma': plataformaSolicitada,
                'codigoOtp': '482910',
                'expiraEnSegundos': 300,
                'mensaje': 'Ingresa el código de 6 dígitos.',
              },
            }),
          ),
          200,
        );
      }

      if (path.endsWith('/auth/login/2fa/verificar')) {
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        final plataformaSolicitada = body['plataforma'] as String? ?? 'MOVIL';
        plataformaActivaServidor = plataformaSolicitada;
        tokenActivoServidor = 'jwt-$plataformaSolicitada-token';

        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': true,
              'message': 'Inicio de sesión exitoso',
              'data': {
                'accessToken': tokenActivoServidor,
                'tokenType': 'Bearer',
                'expiresIn': 900,
                'sessionId': 'sess-$plataformaSolicitada',
                'plataforma': plataformaSolicitada,
                'clienteId': 1,
                'nombreCliente': 'Ana María López García',
                'correo': body['correo'],
                'numeroCuenta': '4229215075268914',
                'saldoCuenta': 4990.0,
              },
            }),
          ),
          200,
        );
      }

      if (authHeader != 'Bearer $tokenActivoServidor') {
        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': false,
              'message': 'Tu sesión fue cerrada o reemplazada desde otro dispositivo.',
            }),
          ),
          401,
        );
      }

      if (path.endsWith('/productos') || path.endsWith('/api/gestopago/catalogo')) {
        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': true,
              'message': 'Catálogo obtenido',
              'data': {
                'data': [
                  {
                    'servicio': 'Telcel',
                    'idServicio': '133',
                    'idCatTipoServicio': '1',
                    'tipoServicio': 'Tiempo Aire',
                    'producto': 'Recarga Telcel \$50',
                    'idProducto': '5',
                    'precio': '50.00',
                    'tipoFront': '1',
                    'hasDigitoVerificador': 'false',
                    'legend': 'Ingresa tu número de 10 dígitos',
                  },
                ],
              },
            }),
          ),
          200,
        );
      }

      if (path.endsWith('/api/gestopago/transacciones/recarga')) {
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        final nuevaCompra = {
          'upc': 'TX1A9001',
          'tipoOperacion': 'RECARGA',
          'idProducto': body['idProducto'],
          'idServicio': body['idServicio'],
          'nombreProducto': 'Recarga Telcel \$50',
          'exitoso': true,
          'estado': 'EXITOSA',
          'codigo': '01',
          'mensaje': 'Abono realizado exitosamente',
          'idTx': 'TX-ANA-9001',
          'numAutorizacion': 'AUTH-778899',
          'saldo': 1000.0,
          'monto': body['monto'],
          'comision': 0.0,
          'saldoCliente': 4940.0,
          'telefono': body['telefono'],
          'numeroCuenta': body['numeroCuenta'],
          'fecha': '09/Nov/2026 19:35:00',
        };
        historialServidor.add(nuevaCompra);

        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': true,
              'message': 'Compra exitosa',
              'data': nuevaCompra,
            }),
          ),
          200,
        );
      }

      if (path.endsWith('/api/gestopago/transacciones/historial/exitosas')) {
        return http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'success': true,
              'message': 'Historial obtenido',
              'data': historialServidor,
            }),
          ),
          200,
        );
      }

      return http.Response('{}', 404);
    });

    final storageMovil = SecureSessionStorage();
    final apiMovil = ApiClient(sessionStorage: storageMovil, httpClient: mockHttp);
    final authMovil = AuthService(apiClient: apiMovil);
    final catalogMovil = CatalogService(apiClient: apiMovil);
    final txMovil = TransactionService(apiClient: apiMovil);

    final retoMovil = await authMovil.solicitarReto2FA(
      correo: 'ana.lopez.prueba@example.com',
      password: 'Prueba2026!',
      plataforma: 'MOVIL',
    );
    expect(retoMovil.requiere2FA, isTrue);
    expect(retoMovil.codigoOtp, '482910');

    final sesionMovil = await authMovil.verificar2FAYCrearSesion(
      correo: 'ana.lopez.prueba@example.com',
      password: 'Prueba2026!',
      codigo2FA: retoMovil.codigoOtp,
      plataforma: 'MOVIL',
    );
    storageMovil.guardarSesion(sesionMovil);
    expect(sesionMovil.plataforma, 'MOVIL');

    final productos = await catalogMovil.obtenerCatalogoProductos();
    expect(productos.length, 1);
    final CatalogProduct productoElegido = productos.first;
    expect(productoElegido.nombre, 'Recarga Telcel \$50');

    final resultadoCompra = await txMovil.procesarCompra(
      producto: productoElegido,
      numeroCuenta: sesionMovil.numeroCuenta!,
      monto: productoElegido.precio,
      destino: '5512345678',
    );
    expect(resultadoCompra.exitoso, isTrue);
    expect(resultadoCompra.idTx, 'TX-ANA-9001');

    final historialEnMovil = await txMovil.obtenerHistorialComprasExitosas(
      clienteId: sesionMovil.clienteId,
      numeroCuenta: sesionMovil.numeroCuenta,
    );
    expect(historialEnMovil.length, 1);
    expect(historialEnMovil.first.idTx, 'TX-ANA-9001');

    final storageWeb = SecureSessionStorage();
    final apiWeb = ApiClient(sessionStorage: storageWeb, httpClient: mockHttp);
    final authWeb = AuthService(apiClient: apiWeb);
    final txWeb = TransactionService(apiClient: apiWeb);

    ApiException? conflictoCapturado;
    try {
      await authWeb.solicitarReto2FA(
        correo: 'ana.lopez.prueba@example.com',
        password: 'Prueba2026!',
        plataforma: 'WEB',
        forzarCierreSesion: false,
      );
    } on ApiException catch (e) {
      conflictoCapturado = e;
    }

    expect(conflictoCapturado, isNotNull);
    expect(conflictoCapturado!.statusCode, 409);
    expect(conflictoCapturado.conflictoSesion?.sesionActivaExistente, isTrue);
    expect(conflictoCapturado.conflictoSesion?.plataformaActiva, 'MOVIL');

    final retoWeb = await authWeb.solicitarReto2FA(
      correo: 'ana.lopez.prueba@example.com',
      password: 'Prueba2026!',
      plataforma: 'WEB',
      forzarCierreSesion: true,
    );
    final sesionWeb = await authWeb.verificar2FAYCrearSesion(
      correo: 'ana.lopez.prueba@example.com',
      password: 'Prueba2026!',
      codigo2FA: retoWeb.codigoOtp,
      plataforma: 'WEB',
      forzarCierreSesion: true,
    );
    storageWeb.guardarSesion(sesionWeb);
    expect(sesionWeb.plataforma, 'WEB');

    final historialEnWeb = await txWeb.obtenerHistorialComprasExitosas(
      clienteId: sesionWeb.clienteId,
      numeroCuenta: sesionWeb.numeroCuenta,
    );
    expect(historialEnWeb.length, 1);
    expect(historialEnWeb.first.idTx, 'TX-ANA-9001');
    expect(historialEnWeb.first.nombreProducto, 'Recarga Telcel \$50');

    ApiException? sesionMovilInvalidada;
    try {
      await catalogMovil.obtenerCatalogoProductos();
    } on ApiException catch (e) {
      sesionMovilInvalidada = e;
    }
    expect(sesionMovilInvalidada, isNotNull);
    expect(sesionMovilInvalidada!.statusCode, 401);
    expect(sesionMovilInvalidada.esSesionReemplazada, isTrue);
  });
}
