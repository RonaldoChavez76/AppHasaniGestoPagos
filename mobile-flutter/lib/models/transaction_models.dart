enum EstadoCompraResultado {
  exitosa,
  rechazada,
  pendiente,
  errorComunicacion,
}

class PurchaseTransactionResult {
  const PurchaseTransactionResult({
    required this.upc,
    required this.tipoOperacion,
    required this.exitoso,
    required this.estadoRaw,
    required this.estadoVisual,
    required this.mensaje,
    required this.monto,
    this.idProducto,
    this.idServicio,
    this.nombreProducto,
    this.codigo,
    this.idTx,
    this.numAutorizacion,
    this.comision,
    this.fecha,
    this.telefono,
    this.referencia,
    this.pin,
    this.legend,
    this.numeroCuenta,
    this.nuevoSaldoCuenta,
    this.fechaRegistro,
  });

  final String upc;
  final String tipoOperacion;
  final int? idProducto;
  final int? idServicio;
  final String? nombreProducto;
  final bool exitoso;
  final String estadoRaw;
  final EstadoCompraResultado estadoVisual;
  final String? codigo;
  final String mensaje;
  final String? idTx;
  final String? numAutorizacion;
  final double monto;
  final String? comision;
  final String? fecha;
  final String? telefono;
  final String? referencia;
  final String? pin;
  final String? legend;
  final String? numeroCuenta;
  final double? nuevoSaldoCuenta;
  final String? fechaRegistro;

  String get fechaMostrar {
    if (fecha != null && fecha!.trim().isNotEmpty) return fecha!;
    if (fechaRegistro != null && fechaRegistro!.trim().isNotEmpty) {
      return fechaRegistro!.replaceFirst('T', ' ').split('.').first;
    }
    return DateTime.now().toIso8601String().replaceFirst('T', ' ').split('.').first;
  }

  String get destinoMostrar {
    if (referencia != null && referencia!.trim().isNotEmpty) {
      return referencia!;
    }
    return telefono ?? '-';
  }

  factory PurchaseTransactionResult.fromJson(
    Map<String, dynamic> json, {
    String? nombreProductoFallback,
    int? idProductoFallback,
    int? idServicioFallback,
  }) {
    final estadoStr = (json['estado'] ?? '').toString().toUpperCase();
    final exitosoFlag = json['exitoso'] as bool? ?? (estadoStr == 'EXITOSA');
    final montoVal = (json['monto'] as num?)?.toDouble() ?? 0.0;

    final EstadoCompraResultado visual;
    if (exitosoFlag || estadoStr == 'EXITOSA') {
      visual = EstadoCompraResultado.exitosa;
    } else if (estadoStr == 'TIMEOUT' || estadoStr == 'PENDIENTE') {
      visual = EstadoCompraResultado.pendiente;
    } else if (estadoStr == 'RECHAZADA' || estadoStr == 'DUPLICADA') {
      visual = EstadoCompraResultado.rechazada;
    } else {
      visual = EstadoCompraResultado.errorComunicacion;
    }

    return PurchaseTransactionResult(
      upc: (json['upc'] ?? 'SIN-UPC').toString(),
      tipoOperacion: (json['tipoOperacion'] ?? 'RECARGA').toString(),
      idProducto: (json['idProducto'] as num?)?.toInt() ?? idProductoFallback,
      idServicio: (json['idServicio'] as num?)?.toInt() ?? idServicioFallback,
      nombreProducto: (json['nombreProducto'] ?? nombreProductoFallback)?.toString(),
      exitoso: visual == EstadoCompraResultado.exitosa,
      estadoRaw: estadoStr.isEmpty ? 'DESCONOCIDO' : estadoStr,
      estadoVisual: visual,
      codigo: json['codigo']?.toString(),
      mensaje: (json['mensaje'] ?? 'Operación procesada').toString(),
      idTx: json['idTx']?.toString(),
      numAutorizacion: json['numAutorizacion']?.toString(),
      monto: montoVal,
      comision: (json['comisionFormateada'] ?? json['comision'])?.toString(),
      fecha: json['fecha']?.toString(),
      telefono: json['telefono']?.toString(),
      referencia: json['referencia']?.toString(),
      pin: json['pin']?.toString(),
      legend: json['legend']?.toString(),
      numeroCuenta: json['numeroCuenta']?.toString(),
      nuevoSaldoCuenta: (json['nuevoSaldoCuenta'] as num?)?.toDouble(),
      fechaRegistro: json['fechaRegistro']?.toString(),
    );
  }
}

