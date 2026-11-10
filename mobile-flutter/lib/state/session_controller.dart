import 'package:flutter/foundation.dart';

import '../core/network/api_client.dart';
import '../core/storage/secure_session_storage.dart';
import '../models/auth_models.dart';
import '../services/auth_service.dart';

class SessionController extends ChangeNotifier {
  SessionController() {
    _storage = SecureSessionStorage();
    _apiClient = ApiClient(
      sessionStorage: _storage,
      onUnauthorizedSession: (motivo) {
        _storage.limpiarSesion();
        _mensajeAlertaSesion = motivo;
        notifyListeners();
      },
    );
    _authService = AuthService(apiClient: _apiClient);
  }

  late final SecureSessionStorage _storage;
  late final ApiClient _apiClient;
  late final AuthService _authService;

  bool _cargando = false;
  String? _mensajeAlertaSesion;

  ApiClient get apiClient => _apiClient;
  AuthService get authService => _authService;
  UserSession? get sesionActual => _storage.currentSession;
  bool get estaAutenticado => sesionActual != null;
  bool get cargando => _cargando;
  String? get mensajeAlertaSesion => _mensajeAlertaSesion;

  void limpiarAlertaSesion() {
    if (_mensajeAlertaSesion != null) {
      _mensajeAlertaSesion = null;
      notifyListeners();
    }
  }

  void actualizarSaldoLocal(double? nuevoSaldo) {
    final actual = sesionActual;
    if (actual != null && nuevoSaldo != null) {
      _storage.guardarSesion(actual.copyWith(saldoCuenta: nuevoSaldo));
      notifyListeners();
    }
  }

  Future<Login2faChallenge> iniciarPreLogin({
    required String correo,
    required String password,
    String? plataforma,
    bool forzarCierreSesion = false,
  }) async {
    _cargando = true;
    _mensajeAlertaSesion = null;
    notifyListeners();
    try {
      return await _authService.solicitarReto2FA(
        correo: correo,
        password: password,
        plataforma: plataforma,
        forzarCierreSesion: forzarCierreSesion,
      );
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }

  Future<UserSession> completarLoginCon2FA({
    required String correo,
    required String password,
    required String codigo2FA,
    String? plataforma,
    bool forzarCierreSesion = false,
  }) async {
    _cargando = true;
    notifyListeners();
    try {
      final sesion = await _authService.verificar2FAYCrearSesion(
        correo: correo,
        password: password,
        codigo2FA: codigo2FA,
        plataforma: plataforma,
        forzarCierreSesion: forzarCierreSesion,
      );
      _storage.guardarSesion(sesion);
      return sesion;
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }

  Future<bool> verificarVigenciaSesion() async {
    final actual = sesionActual;
    if (actual == null) return false;
    try {
      final actualizada = await _authService.sincronizarEstadoSesion(actual);
      if (actualizada != null) {
        _storage.guardarSesion(actualizada);
        notifyListeners();
      }
      return true;
    } on ApiException catch (e) {
      if (e.statusCode == 401) {
        _storage.limpiarSesion();
        _mensajeAlertaSesion = e.message;
        notifyListeners();
        return false;
      }
      return true;
    }
  }

  Future<void> cerrarSesion() async {
    _cargando = true;
    notifyListeners();
    try {
      await _authService.cerrarSesionEnServidor();
    } finally {
      _storage.limpiarSesion();
      _cargando = false;
      notifyListeners();
    }
  }
}

