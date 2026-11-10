import '../../models/auth_models.dart';

class SecureSessionStorage {
  UserSession? _currentSession;

  UserSession? get currentSession {
    if (_currentSession != null && _currentSession!.haExpiradoLocalmente) {
      _currentSession = null;
      return null;
    }
    return _currentSession;
  }

  void guardarSesion(UserSession session) {
    _currentSession = session;
  }

  void limpiarSesion() {
    _currentSession = null;
  }
}
