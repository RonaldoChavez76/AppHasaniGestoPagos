import 'package:flutter/foundation.dart';

enum BackendEnvironment {
  renderCloud('Render Cloud (Producción)', 'https://apphasanigestopagos.onrender.com'),
  localHost('Servidor Local (Puerto 8080)', 'http://localhost:8080'),
  androidEmulator('Emulador Android (10.0.2.2:8080)', 'http://10.0.2.2:8080');

  const BackendEnvironment(this.etiqueta, this.baseUrl);
  final String etiqueta;
  final String baseUrl;
}

class ApiConfig {
  ApiConfig._();

  static final ValueNotifier<BackendEnvironment> environmentNotifier =
      ValueNotifier<BackendEnvironment>(BackendEnvironment.renderCloud);

  static final ValueNotifier<String> plataformaActivaNotifier =
      ValueNotifier<String>(plataformaPorDefecto);

  static String get plataformaPorDefecto => kIsWeb ? 'WEB' : 'MOVIL';

  static String get plataformaActual => plataformaActivaNotifier.value;

  static String get baseUrl => environmentNotifier.value.baseUrl;

  static Uri endpoint(String path) {
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    return Uri.parse('$baseUrl$normalizedPath');
  }
}
