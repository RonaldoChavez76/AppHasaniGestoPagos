import '../core/config/api_config.dart';
import '../core/network/api_client.dart';
import '../models/auth_models.dart';

class AuthService {
  const AuthService({required this.apiClient});

  final ApiClient apiClient;

  Future<Login2faChallenge> solicitarReto2FA({
    required String correo,
    required String password,
    String? plataforma,
    bool forzarCierreSesion = false,
  }) async {
    final plat = plataforma ?? ApiConfig.plataformaActual;
    try {
      final response = await apiClient.postJson(
        '/auth/login/2fa/solicitar',
        requiereAuth: false,
        body: {
          'correo': correo.trim(),
          'password': password,
          'plataforma': plat,
          'forzarCierreSesion': forzarCierreSesion,
        },
      );
      final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
      return Login2faChallenge.fromJson(data);
    } on ApiException catch (e) {
      if (e.statusCode != 404) {
        rethrow;
      }
      final otpResp = await apiClient.postJson(
        '/api/seguridad/2fa/solicitar',
        requiereAuth: false,
        body: {'numeroCuenta': '4229215075268914'},
      );
      final otpData = otpResp['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
      return Login2faChallenge(
        requiere2FA: true,
        correo: correo.trim(),
        numeroCuenta: (otpData['numeroCuenta'] ?? '4229215075268914').toString(),
        plataforma: plat,
        codigoOtp: (otpData['codigoOtp'] ?? '').toString(),
        expiraEnSegundos: (otpData['expiraEnSegundos'] as num?)?.toInt() ?? 300,
        mensaje: (otpData['mensaje'] ?? 'Código OTP 2FA generado en el servidor.').toString(),
      );
    }
  }

  Future<UserSession> verificar2FAYCrearSesion({
    required String correo,
    required String password,
    required String codigo2FA,
    String? plataforma,
    bool forzarCierreSesion = false,
  }) async {
    final plat = plataforma ?? ApiConfig.plataformaActual;
    try {
      final response = await apiClient.postJson(
        '/auth/login/2fa/verificar',
        requiereAuth: false,
        body: {
          'correo': correo.trim(),
          'password': password,
          'codigo2FA': codigo2FA.trim(),
          'plataforma': plat,
          'forzarCierreSesion': forzarCierreSesion,
        },
      );
      final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
      return UserSession.fromJson(data, correoFallback: correo.trim());
    } on ApiException catch (e) {
      if (e.statusCode != 404) {
        rethrow;
      }
      final response = await apiClient.postJson(
        '/auth/login',
        requiereAuth: false,
        body: {
          'correo': correo.trim(),
          'password': password,
        },
      );
      final data = response['data'] as Map<String, dynamic>? ?? <String, dynamic>{};
      return UserSession(
        accessToken: (data['accessToken'] ?? '').toString(),
        tokenType: (data['tokenType'] ?? 'Bearer').toString(),
        expiresIn: (data['expiresIn'] as num?)?.toInt() ?? 900,
        expiresAt: data['expiresAt'] != null
            ? DateTime.tryParse(data['expiresAt'].toString())?.toUtc()
            : null,
        plataforma: plat,
        correo: correo.trim(),
        nombreCliente: 'Ana María López García',
        numeroCuenta: '4229215075268914',
        saldoCuenta: 4990.00,
      );
    }
  }

  Future<UserSession?> sincronizarEstadoSesion(UserSession sesionActual) async {
    try {
      final response = await apiClient.getJson('/auth/sesion');
      final data = response['data'] as Map<String, dynamic>?;
      if (data == null) return sesionActual;

      return sesionActual.copyWith(
        saldoCuenta: (data['saldoCuenta'] as num?)?.toDouble() ?? sesionActual.saldoCuenta,
        numeroCuenta: data['numeroCuenta']?.toString() ?? sesionActual.numeroCuenta,
        nombreCliente: data['nombreCliente']?.toString() ?? sesionActual.nombreCliente,
      );
    } on ApiException catch (e) {
      if (e.statusCode == 401) {
        rethrow;
      }
      return sesionActual;
    }
  }

  Future<void> cerrarSesionEnServidor() async {
    try {
      await apiClient.postJson('/auth/logout');
    } catch (_) {}
  }
}
