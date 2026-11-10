class ConflictoSesion {
  const ConflictoSesion({
    required this.sesionActivaExistente,
    required this.plataformaActiva,
    required this.accionRequerida,
    this.fechaInicio,
    this.fechaExpiracion,
  });

  final bool sesionActivaExistente;
  final String plataformaActiva;
  final String? fechaInicio;
  final String? fechaExpiracion;
  final String accionRequerida;

  factory ConflictoSesion.fromJson(Map<String, dynamic> json) {
    return ConflictoSesion(
      sesionActivaExistente: json['sesionActivaExistente'] as bool? ?? true,
      plataformaActiva: (json['plataformaActiva'] ?? 'OTRO DISPOSITIVO').toString(),
      fechaInicio: json['fechaInicio']?.toString(),
      fechaExpiracion: json['fechaExpiracion']?.toString(),
      accionRequerida: (json['accionRequerida'] ??
              'Ya existe una sesión activa en otra plataforma. ¿Deseas cerrarla para continuar?')
          .toString(),
    );
  }
}

class Login2faChallenge {
  const Login2faChallenge({
    required this.requiere2FA,
    required this.correo,
    required this.numeroCuenta,
    required this.plataforma,
    required this.codigoOtp,
    required this.expiraEnSegundos,
    required this.mensaje,
  });

  final bool requiere2FA;
  final String correo;
  final String numeroCuenta;
  final String plataforma;
  final String codigoOtp;
  final int expiraEnSegundos;
  final String mensaje;

  factory Login2faChallenge.fromJson(Map<String, dynamic> json) {
    return Login2faChallenge(
      requiere2FA: json['requiere2FA'] as bool? ?? true,
      correo: (json['correo'] ?? '').toString(),
      numeroCuenta: (json['numeroCuenta'] ?? '').toString(),
      plataforma: (json['plataforma'] ?? 'WEB').toString(),
      codigoOtp: (json['codigoOtp'] ?? '').toString(),
      expiraEnSegundos: (json['expiraEnSegundos'] as num?)?.toInt() ?? 300,
      mensaje: (json['mensaje'] ?? 'Ingresa el código de verificación de 6 dígitos.').toString(),
    );
  }
}

class UserSession {
  const UserSession({
    required this.accessToken,
    required this.tokenType,
    required this.expiresIn,
    required this.plataforma,
    required this.correo,
    this.expiresAt,
    this.sessionId,
    this.clienteId,
    this.nombreCliente,
    this.numeroCuenta,
    this.saldoCuenta,
  });

  final String accessToken;
  final String tokenType;
  final int expiresIn;
  final DateTime? expiresAt;
  final String? sessionId;
  final String plataforma;
  final int? clienteId;
  final String? nombreCliente;
  final String correo;
  final String? numeroCuenta;
  final double? saldoCuenta;

  bool get haExpiradoLocalmente {
    if (expiresAt == null) return false;
    return DateTime.now().toUtc().isAfter(expiresAt!);
  }

  UserSession copyWith({
    double? saldoCuenta,
    String? numeroCuenta,
    String? nombreCliente,
  }) {
    return UserSession(
      accessToken: accessToken,
      tokenType: tokenType,
      expiresIn: expiresIn,
      expiresAt: expiresAt,
      sessionId: sessionId,
      plataforma: plataforma,
      clienteId: clienteId,
      nombreCliente: nombreCliente ?? this.nombreCliente,
      correo: correo,
      numeroCuenta: numeroCuenta ?? this.numeroCuenta,
      saldoCuenta: saldoCuenta ?? this.saldoCuenta,
    );
  }

  factory UserSession.fromJson(Map<String, dynamic> json, {String correoFallback = ''}) {
    DateTime? parsedExpiresAt;
    if (json['expiresAt'] != null) {
      parsedExpiresAt = DateTime.tryParse(json['expiresAt'].toString())?.toUtc();
    }
    return UserSession(
      accessToken: (json['accessToken'] ?? '').toString(),
      tokenType: (json['tokenType'] ?? 'Bearer').toString(),
      expiresIn: (json['expiresIn'] as num?)?.toInt() ?? 900,
      expiresAt: parsedExpiresAt,
      sessionId: json['sessionId']?.toString(),
      plataforma: (json['plataforma'] ?? 'WEB').toString(),
      clienteId: (json['clienteId'] as num?)?.toInt(),
      nombreCliente: json['nombreCliente']?.toString(),
      correo: (json['correo'] ?? correoFallback).toString(),
      numeroCuenta: json['numeroCuenta']?.toString(),
      saldoCuenta: (json['saldoCuenta'] as num?)?.toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'tokenType': tokenType,
      'expiresIn': expiresIn,
      'expiresAt': expiresAt?.toIso8601String(),
      'sessionId': sessionId,
      'plataforma': plataforma,
      'clienteId': clienteId,
      'nombreCliente': nombreCliente,
      'correo': correo,
      'numeroCuenta': numeroCuenta,
      'saldoCuenta': saldoCuenta,
    };
  }
}

