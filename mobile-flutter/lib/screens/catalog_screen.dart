import 'package:flutter/material.dart';

import '../core/network/api_client.dart';
import '../models/product_models.dart';
import '../services/catalog_service.dart';
import '../state/session_controller.dart';

class CatalogScreen extends StatefulWidget {
  const CatalogScreen({
    required this.sessionController,
    this.catalogService,
    this.onProductoSeleccionado,
    super.key,
  });

  final SessionController sessionController;
  final CatalogService? catalogService;
  final void Function(CatalogProduct producto)? onProductoSeleccionado;

  @override
  State<CatalogScreen> createState() => _CatalogScreenState();
}

class _CatalogScreenState extends State<CatalogScreen> {
  static const List<String> _categorias = [
    'Todos',
    'Recargas Telefónicas',
    'Pago de Servicios',
    'Entretenimiento y Pines',
  ];

  late final CatalogService _service;
  final _busquedaController = TextEditingController();

  List<CatalogProduct> _productos = const [];
  String _categoriaSeleccionada = 'Todos';
  String _filtroBusqueda = '';
  bool _cargando = true;
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _service = widget.catalogService ?? CatalogService(apiClient: widget.sessionController.apiClient);
    _cargarCatalogo();
  }

  @override
  void dispose() {
    _busquedaController.dispose();
    super.dispose();
  }

  Future<void> _cargarCatalogo() async {
    setState(() {
      _cargando = true;
      _errorMensaje = null;
    });

    try {
      final lista = await _service.obtenerCatalogoProductos();
      if (!mounted) return;
      setState(() {
        _productos = lista;
        _cargando = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = e.message;
        _cargando = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMensaje = 'No fue posible cargar el catálogo de productos GestoPago.';
        _cargando = false;
      });
    }
  }

  List<CatalogProduct> get _productosFiltrados {
    return _productos.where((prod) {
      final coincideCategoria =
          _categoriaSeleccionada == 'Todos' || prod.categoria == _categoriaSeleccionada;
      if (!coincideCategoria) return false;
      if (_filtroBusqueda.isEmpty) return true;
      final q = _filtroBusqueda.toLowerCase();
      return prod.nombre.toLowerCase().contains(q) ||
          prod.servicio.toLowerCase().contains(q) ||
          prod.idProducto.toString().contains(q) ||
          prod.categoria.toLowerCase().contains(q);
    }).toList();
  }

  void _mostrarDetalleProducto(CatalogProduct producto) {
    final colorScheme = Theme.of(context).colorScheme;
    final visual = _VisualProducto.paraProducto(producto, colorScheme);

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 540),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 28,
                        backgroundColor: visual.colorFondo,
                        child: Icon(visual.icono, color: visual.colorIcono, size: 30),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              producto.nombre,
                              style: Theme.of(ctx).textTheme.titleLarge?.copyWith(
                                    fontWeight: FontWeight.bold,
                                  ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${producto.servicio} • ${producto.categoria}',
                              style: TextStyle(color: colorScheme.onSurfaceVariant),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.45),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text('Precio / Importe:'),
                            Text(
                              producto.precio > 0
                                  ? '\$${producto.precio.toStringAsFixed(2)} MXN'
                                  : 'Monto variable de recibo',
                              style: Theme.of(ctx).textTheme.titleMedium?.copyWith(
                                    fontWeight: FontWeight.w800,
                                    color: colorScheme.primary,
                                  ),
                            ),
                          ],
                        ),
                        const Divider(height: 20),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text('ID Producto: #${producto.idProducto}'),
                            Text('ID Servicio: #${producto.idServicio}'),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Tipo de operación: ${producto.esRecarga ? "Recarga Tiempo Aire" : "Pago de Servicio"}',
                            ),
                            Chip(
                              label: Text(
                                producto.disponible ? 'Disponible' : 'No disponible',
                                style: const TextStyle(fontSize: 12),
                              ),
                              visualDensity: VisualDensity.compact,
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Descripción del producto',
                    style: Theme.of(ctx).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    producto.descripcion,
                    style: TextStyle(color: colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 24),
                  FilledButton.icon(
                    onPressed: !producto.disponible
                        ? null
                        : () {
                            Navigator.pop(ctx);
                            if (widget.onProductoSeleccionado != null) {
                              widget.onProductoSeleccionado!(producto);
                            }
                          },
                    icon: const Icon(Icons.shopping_bag_outlined),
                    label: const Text('Seleccionar para Compra'),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final filtrados = _productosFiltrados;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _busquedaController,
                  onChanged: (val) => setState(() => _filtroBusqueda = val.trim()),
                  decoration: InputDecoration(
                    hintText: 'Buscar por producto, compañía o código (ej. Telcel, CFE, 582)...',
                    prefixIcon: const Icon(Icons.search_rounded),
                    suffixIcon: _filtroBusqueda.isEmpty
                        ? null
                        : IconButton(
                            onPressed: () {
                              _busquedaController.clear();
                              setState(() => _filtroBusqueda = '');
                            },
                            icon: const Icon(Icons.clear_rounded),
                          ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              IconButton.filledTonal(
                tooltip: 'Actualizar catálogo desde GestoPago',
                onPressed: _cargando ? null : _cargarCatalogo,
                icon: const Icon(Icons.refresh_rounded),
              ),
            ],
          ),
        ),
        SizedBox(
          height: 48,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            scrollDirection: Axis.horizontal,
            itemCount: _categorias.length,
            separatorBuilder: (_, _) => const SizedBox(width: 8),
            itemBuilder: (context, index) {
              final cat = _categorias[index];
              final seleccionada = cat == _categoriaSeleccionada;
              return ChoiceChip(
                label: Text(cat),
                selected: seleccionada,
                onSelected: (_) => setState(() => _categoriaSeleccionada = cat),
              );
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 4),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Mostrando ${filtrados.length} productos disponibles',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
              ),
              if (_categoriaSeleccionada != 'Todos' || _filtroBusqueda.isNotEmpty)
                TextButton(
                  onPressed: () {
                    _busquedaController.clear();
                    setState(() {
                      _categoriaSeleccionada = 'Todos';
                      _filtroBusqueda = '';
                    });
                  },
                  child: const Text('Limpiar filtros'),
                ),
            ],
          ),
        ),
        Expanded(
          child: _construirContenido(filtrados, colorScheme),
        ),
      ],
    );
  }

  Widget _construirContenido(List<CatalogProduct> filtrados, ColorScheme colorScheme) {
    if (_cargando) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 14),
            Text('Consultando catálogo oficial de GestoPago...'),
          ],
        ),
      );
    }

    if (_errorMensaje != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 56, color: colorScheme.error),
              const SizedBox(height: 12),
              Text(
                _errorMensaje!,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _cargarCatalogo,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Reintentar consulta'),
              ),
            ],
          ),
        ),
      );
    }

    if (filtrados.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.search_off_rounded, size: 56, color: colorScheme.outline),
              const SizedBox(height: 12),
              const Text(
                'No se encontraron productos que coincidan con tu búsqueda.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final crossAxisCount = width >= 1200
            ? 4
            : width >= 860
                ? 3
                : width >= 560
                    ? 2
                    : 1;

        return RefreshIndicator(
          onRefresh: _cargarCatalogo,
          child: GridView.builder(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: crossAxisCount,
              crossAxisSpacing: 14,
              mainAxisSpacing: 14,
              mainAxisExtent: 215,
            ),
            itemCount: filtrados.length,
            itemBuilder: (context, index) {
              final prod = filtrados[index];
              return _TarjetaProductoCatalogo(
                producto: prod,
                onVerDetalle: () => _mostrarDetalleProducto(prod),
                onComprar: () {
                  if (widget.onProductoSeleccionado != null) {
                    widget.onProductoSeleccionado!(prod);
                  } else {
                    _mostrarDetalleProducto(prod);
                  }
                },
              );
            },
          ),
        );
      },
    );
  }
}

class _TarjetaProductoCatalogo extends StatelessWidget {
  const _TarjetaProductoCatalogo({
    required this.producto,
    required this.onVerDetalle,
    required this.onComprar,
  });

  final CatalogProduct producto;
  final VoidCallback onVerDetalle;
  final VoidCallback onComprar;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final visual = _VisualProducto.paraProducto(producto, colorScheme);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onVerDetalle,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(
                    radius: 22,
                    backgroundColor: visual.colorFondo,
                    child: Icon(visual.icono, color: visual.colorIcono, size: 24),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          producto.nombre,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          producto.servicio,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: colorScheme.onSurfaceVariant,
                              ),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: producto.disponible
                          ? Colors.green.withValues(alpha: 0.14)
                          : colorScheme.errorContainer,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      producto.disponible ? 'Activo' : 'Inactivo',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: producto.disponible ? Colors.green.shade800 : colorScheme.onErrorContainer,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Expanded(
                child: Text(
                  producto.descripcion,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                ),
              ),
              const Divider(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          producto.precio > 0
                              ? '\$${producto.precio.toStringAsFixed(2)} MXN'
                              : 'Monto abierto',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.w800,
                                color: colorScheme.primary,
                              ),
                        ),
                        Text(
                          'ID #${producto.idProducto}',
                          style: Theme.of(context).textTheme.labelSmall?.copyWith(
                                color: colorScheme.outline,
                              ),
                        ),
                      ],
                    ),
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        tooltip: 'Ver detalle',
                        visualDensity: VisualDensity.compact,
                        onPressed: onVerDetalle,
                        icon: const Icon(Icons.info_outline_rounded, size: 20),
                      ),
                      FilledButton.tonalIcon(
                        style: FilledButton.styleFrom(
                          visualDensity: VisualDensity.compact,
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        ),
                        onPressed: producto.disponible ? onComprar : null,
                        icon: const Icon(Icons.shopping_cart_checkout_rounded, size: 16),
                        label: const Text('Comprar'),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _VisualProducto {
  const _VisualProducto({
    required this.icono,
    required this.colorFondo,
    required this.colorIcono,
  });

  final IconData icono;
  final Color colorFondo;
  final Color colorIcono;

  static _VisualProducto paraProducto(CatalogProduct producto, ColorScheme scheme) {
    final upper = '${producto.nombre} ${producto.servicio}'.toUpperCase();
    if (producto.categoria == 'Recargas Telefónicas') {
      return _VisualProducto(
        icono: Icons.phone_android_rounded,
        colorFondo: scheme.primaryContainer,
        colorIcono: scheme.onPrimaryContainer,
      );
    }
    if (producto.categoria == 'Entretenimiento y Pines') {
      return _VisualProducto(
        icono: Icons.card_giftcard_rounded,
        colorFondo: scheme.tertiaryContainer,
        colorIcono: scheme.onTertiaryContainer,
      );
    }
    if (upper.contains('CFE') || upper.contains('LUZ')) {
      return _VisualProducto(
        icono: Icons.bolt_rounded,
        colorFondo: Colors.amber.withValues(alpha: 0.2),
        colorIcono: Colors.amber.shade900,
      );
    }
    if (upper.contains('TELMEX') || upper.contains('IZZI') || upper.contains('TOTALPLAY')) {
      return _VisualProducto(
        icono: Icons.router_rounded,
        colorFondo: scheme.secondaryContainer,
        colorIcono: scheme.onSecondaryContainer,
      );
    }
    return _VisualProducto(
      icono: Icons.receipt_long_rounded,
      colorFondo: scheme.secondaryContainer,
      colorIcono: scheme.onSecondaryContainer,
    );
  }
}

