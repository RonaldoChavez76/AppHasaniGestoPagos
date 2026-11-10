import 'package:decimo_cuatri/core/storage/secure_session_storage.dart';
import 'package:decimo_cuatri/main.dart';
import 'package:decimo_cuatri/models/auth_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('Renderiza pantalla de Login y permite autocompletar credenciales de prueba', (tester) async {
    await tester.pumpWidget(const AuthShellApp());

    expect(find.text('Bienvenido a GestoPago'), findsOneWidget);
    expect(find.text('Iniciar Sesión con 2FA'), findsOneWidget);

    final botonPrueba = find.text('Cargar credenciales de prueba (Ana)');
    await tester.ensureVisible(botonPrueba);
    await tester.pumpAndSettle();
    await tester.tap(botonPrueba);
    await tester.pump();

    expect(find.text('ana.lopez.prueba@example.com'), findsAtLeastNWidgets(1));
  });

  test('Parsea correctamente ConflictoSesion (409) y UserSession', () {
    final conflicto = ConflictoSesion.fromJson({
      'sesionActivaExistente': true,
      'plataformaActiva': 'MOVIL',
      'fechaInicio': '2026-11-09T18:30:00',
      'accionRequerida': 'Cerrar sesión previa',
    });

    expect(conflicto.sesionActivaExistente, isTrue);
    expect(conflicto.plataformaActiva, 'MOVIL');

    final sesion = UserSession.fromJson({
      'accessToken': 'jwt-token',
      'tokenType': 'Bearer',
      'expiresIn': 900,
      'sessionId': 'sess-123',
      'plataforma': 'WEB',
      'clienteId': 3,
      'nombreCliente': 'Ana María López García',
      'correo': 'ana.lopez.prueba@example.com',
      'numeroCuenta': '4229215075268914',
      'saldoCuenta': 4990.0,
    });

    final storage = SecureSessionStorage();
    storage.guardarSesion(sesion);
    expect(storage.currentSession?.numeroCuenta, '4229215075268914');
    expect(storage.currentSession?.plataforma, 'WEB');
  });
}

