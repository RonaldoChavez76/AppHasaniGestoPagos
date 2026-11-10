import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../models/auth_models.dart';
import '../config/api_config.dart';
import '../storage/secure_session_storage.dart';

class ApiException implements Exception {
  const ApiException({
    required this.statusCode,
    required this.message,
    this.conflictoSesion,
    this.esSesionReemplazada = false,
  });

  final int statusCode;
  final String message;
  final ConflictoSesion? conflictoSesion;
  final bool esSesionReemplazada;

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({
    required this.sessionStorage,
    http.Client? httpClient,
    this.onUnauthorizedSession,
  }) : _httpClient = httpClient ?? http.Client();

  final SecureSessionStorage sessionStorage;
  final http.Client _httpClient;
  final void Function(String motivo)? onUnauthorizedSession;

  Map<String, String> _construirHeaders({bool incluirAuth = true}) {
    final headers = <String, String>{
      'Content-Type': 'application/json; charset=utf-8',
      'Accept': 'application/json',
    };
    if (incluirAuth) {
      final sesion = sessionStorage.currentSession;
      if (sesion != null && sesion.accessToken.isNotEmpty) {
        headers['Authorization'] = 'Bearer ${sesion.accessToken}';
      }
    }
    return headers;
  }

  Future<Map<String, dynamic>> getJson(
    String path, {
    Map<String, String>? queryParameters,
    bool requiereAuth = true,
    Duration timeout = const Duration(seconds: 25),
  }) async {
    var uri = ApiConfig.endpoint(path);
    if (queryParameters != null && queryParameters.isNotEmpty) {
      uri = uri.replace(queryParameters: queryParameters);
    }

    try {
      final response = await _httpClient
          .get(uri, headers: _construirHeaders(incluirAuth: requiereAuth))
          .timeout(timeout);
      return _procesarRespuesta(response, notificarExpiracion: requiereAuth);
    } on TimeoutException {
      throw const ApiException(
        statusCode: 504,
        message: 'Tiempo de espera agotado al comunicarse con el servidor. Verifica tu conexión.',
      );
    } on ApiException {
      rethrow;
    } catch (_) {
      throw const ApiException(
        statusCode: 503,
        message: 'No fue posible establecer comunicación con el servidor GestoPago.',
      );
    }
  }

  Future<Map<String, dynamic>> postJson(
    String path, {
    Map<String, dynamic>? body,
    bool requiereAuth = true,
    Duration timeout = const Duration(seconds: 35),
  }) async {
    final uri = ApiConfig.endpoint(path);
    try {
      final response = await _httpClient
          .post(
            uri,
            headers: _construirHeaders(incluirAuth: requiereAuth),
            body: body != null ? jsonEncode(body) : null,
          )
          .timeout(timeout);
      return _procesarRespuesta(response, notificarExpiracion: requiereAuth);
    } on TimeoutException {
      throw const ApiException(
        statusCode: 504,
        message: 'La solicitud excedió el tiempo de espera. Por seguridad no reintentes cargos sin verificar tu historial.',
      );
    } on ApiException {
      rethrow;
    } catch (_) {
      throw const ApiException(
        statusCode: 503,
        message: 'Error de red al conectar con el servidor. Verifica tu conexión a Internet.',
      );
    }
  }

  Map<String, dynamic> _procesarRespuesta(
    http.Response response, {
    required bool notificarExpiracion,
  }) {
    Map<String, dynamic> decoded = <String, dynamic>{};
    if (response.bodyBytes.isNotEmpty) {
      try {
        final rawText = utf8.decode(response.bodyBytes);
        final parsed = jsonDecode(rawText);
        if (parsed is Map<String, dynamic>) {
          decoded = parsed;
        }
      } catch (_) {
        decoded = {'message': 'Respuesta inesperada del servidor (${response.statusCode}).'};
      }
    }

    final status = response.statusCode;
    final mensaje = (decoded['message'] ?? 'Error en la operación ($status)').toString();

    if (status >= 200 && status < 300) {
      return decoded;
    }

    if (status == 409) {
      ConflictoSesion? conflicto;
      final data = decoded['data'];
      if (data is Map<String, dynamic> && (data['sesionActivaExistente'] == true || data.containsKey('plataformaActiva'))) {
        conflicto = ConflictoSesion.fromJson(data);
      }
      throw ApiException(
        statusCode: 409,
        message: mensaje,
        conflictoSesion: conflicto,
      );
    }

    if (status == 401) {
      final esReemplazada = mensaje.toLowerCase().contains('cerrada') ||
          mensaje.toLowerCase().contains('reemplazada') ||
          mensaje.toLowerCase().contains('expir');
      if (notificarExpiracion && onUnauthorizedSession != null) {
        onUnauthorizedSession!(mensaje);
      }
      throw ApiException(
        statusCode: 401,
        message: mensaje,
        esSesionReemplazada: esReemplazada,
      );
    }

    throw ApiException(statusCode: status, message: mensaje);
  }
}

