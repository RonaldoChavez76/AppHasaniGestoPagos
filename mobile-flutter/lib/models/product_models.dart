class CatalogProduct {
  const CatalogProduct({
    required this.idProducto,
    required this.idServicio,
    required this.nombre,
    required this.servicio,
    required this.categoria,
    required this.tipoFront,
    required this.precio,
    required this.disponible,
    required this.descripcion,
    this.tipoReferencia,
  });

  final int idProducto;
  final int idServicio;
  final String nombre;
  final String servicio;
  final String categoria;
  final String tipoFront;
  final double precio;
  final bool disponible;
  final String descripcion;
  final String? tipoReferencia;

  bool get esRecarga => tipoFront == '1' || categoria == 'Recargas Telefónicas';

  factory CatalogProduct.fromJson(Map<String, dynamic> json) {
    final rawIdProd = json['idProducto'] ?? json['productId'] ?? json['id'] ?? '0';
    final rawIdServ = json['idServicio'] ?? '133';
    final idProd = int.tryParse(rawIdProd.toString()) ?? 0;
    final nombre = (json['producto'] ?? json['name'] ?? 'Producto #$idProd').toString().trim();
    final servicioRaw = (json['servicio'] ?? _inferirMarcaServicio(nombre)).toString().trim();
    final idServ = int.tryParse(rawIdServ.toString()) ?? _inferirIdServicio(nombre, servicioRaw, idProd);

    final precioRaw = json['precio'] ?? json['price'] ?? 0;
    final precio = precioRaw is num
        ? precioRaw.toDouble()
        : (double.tryParse(precioRaw.toString()) ?? 0.0);

    final tipoFrontRaw = json['tipoFront']?.toString();
    final categoria = _clasificarCategoria(nombre, servicioRaw, tipoFrontRaw);
    final tipoFront = tipoFrontRaw ?? (categoria == 'Recargas Telefónicas' ? '1' : '2');
    final activo = json['active'] as bool? ?? true;
    final legend = json['legend']?.toString().trim();
    final descripcion = (legend != null && legend.isNotEmpty)
        ? legend
        : _construirDescripcion(nombre, servicioRaw, categoria, precio);

    return CatalogProduct(
      idProducto: idProd,
      idServicio: idServ,
      nombre: nombre,
      servicio: servicioRaw,
      categoria: categoria,
      tipoFront: tipoFront,
      precio: precio,
      disponible: activo,
      descripcion: descripcion,
      tipoReferencia: json['tipoReferencia']?.toString(),
    );
  }

  static String _inferirMarcaServicio(String nombre) {
    final upper = nombre.toUpperCase();
    if (upper.contains('TELCEL') || upper.contains('AMIGO')) return 'Telcel';
    if (upper.contains('MOVISTAR')) return 'Movistar';
    if (upper.contains('AT&T') || upper.contains('ATT') || upper.contains('UNEFON')) return 'AT&T / Unefon';
    if (upper.contains('BAIT')) return 'Bait';
    if (upper.contains('CFE') || upper.contains('LUZ')) return 'CFE Electricidad';
    if (upper.contains('TELMEX') || upper.contains('INFINITUM')) return 'Telmex';
    if (upper.contains('TOTALPLAY')) return 'Totalplay';
    if (upper.contains('IZZI')) return 'izzi';
    if (upper.contains('SKY') || upper.contains('VETV')) return 'Sky';
    if (upper.contains('NETFLIX') || upper.contains('SPOTIFY') || upper.contains('XBOX') ||
        upper.contains('PLAYSTATION') || upper.contains('STEAM') || upper.contains('NINTENDO') ||
        upper.contains('GOOGLE') || upper.contains('AMAZON')) {
      return 'Pines Digitales';
    }
    return 'GestoPago';
  }

  static int _inferirIdServicio(String nombre, String servicio, int idProducto) {
    final upper = '$nombre $servicio'.toUpperCase();
    if (upper.contains('TELCEL')) return 133;
    if (upper.contains('MOVISTAR')) return 134;
    if (upper.contains('AT&T') || upper.contains('ATT')) return 135;
    if (upper.contains('CFE')) return 76;
    if (upper.contains('TELMEX')) return 77;
    return idProducto > 0 ? idProducto : 133;
  }

  static String _clasificarCategoria(String nombre, String servicio, String? tipoFront) {
    final upper = '$nombre $servicio'.toUpperCase();
    if (upper.contains('NETFLIX') || upper.contains('SPOTIFY') || upper.contains('XBOX') ||
        upper.contains('PLAYSTATION') || upper.contains('STEAM') || upper.contains('PIN') ||
        upper.contains('CINEPOLIS') || upper.contains('AMAZON') || upper.contains('GOOGLE')) {
      return 'Entretenimiento y Pines';
    }
    if (tipoFront == '1' || upper.contains('RECARGA') || upper.contains('TIEMPO AIRE') ||
        upper.contains('PAQUETE') || upper.contains('AMIGO') || upper.contains('TELCEL') ||
        upper.contains('MOVISTAR') || upper.contains('AT&T') || upper.contains('UNEFON') ||
        upper.contains('BAIT') || upper.contains('VIRGIN')) {
      return 'Recargas Telefónicas';
    }
    return 'Pago de Servicios';
  }

  static String _construirDescripcion(String nombre, String servicio, String categoria, double precio) {
    final montoStr = precio > 0 ? ' por \$${precio.toStringAsFixed(2)} MXN' : ' con monto abierto o de recibo';
    if (categoria == 'Recargas Telefónicas') {
      return 'Acreditación inmediata de tiempo aire / datos móviles para $servicio ($nombre)$montoStr a número celular de 10 dígitos.';
    }
    if (categoria == 'Entretenimiento y Pines') {
      return 'Adquisición de código o PIN electrónico para $nombre ($servicio)$montoStr con entrega inmediata en tu comprobante.';
    }
    return 'Pago electrónico de recibo o referencia para el servicio $servicio ($nombre)$montoStr procesado en línea vía GestoPago.';
  }
}

