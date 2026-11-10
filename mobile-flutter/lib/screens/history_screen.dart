import 'package:flutter/material.dart';

import '../core/network/api_client.dart';
import '../models/transaction_models.dart';
import '../services/transaction_service.dart';
import '../state/session_controller.dart';
import 'purchase_screen.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({
    required this.sessionController,
    this.transactionService,
    super.key,
  });

  final SessionController sessionController;
  final TransactionService? transactionService;

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late final TransactionService _service;
  final _filtroController = TextEditingController();

  List<PurchaseTransactionResult> _comprasExitosas = const [];
  String _busqueda = '';
  bool _cargando = true;
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _service = widget.transactionService ??
        TransactionService(apiClient: widget.sessionController.apiClient);
    _cargarHistorial();
  }

  @override
  void dispose() {
    _filtroController.dispose();
    super.dispose();
  }

  Future<void> _cargarHistorial() async {
    setState(() {
      _cargando = true;
      _errorMensaje = null;
    });

    final sesion = widget.sessionController.sesionActual;
    try {
      final lista = await _service.obtenerHistorialComprasExitosas(
        clienteId: sesion?.clienteId,
        numeroCuenta: sesion?.numeroCuenta,
      );
      if (!mounted) return;
      setState(() {
        _comprasExitosas = lista;
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
        _errorMensaje = 'No fue posible consultar tu historial de compras exitosas.';
        _cargando = false;
      });
    }
  }

  List<PurchaseTransactionResult> get _comprasFiltradas {
    if (_busqueda.isEmpty) return _comprasExitosas;
    final q = _busqueda.toLowerCase();
    return _comprasExitosas.where((item) {
      final prod = (item.nombreProducto ?? item.tipoOperacion).toLowerCase();
      final upc = item.upc.toLowerCase();
      final idTx = (item.idTx ?? '').toLowerCase();
      final auth = (item.numAutorizacion ?? '').toLowerCase();
      final dest = item.destinoMostrar.toLowerCase();
      return prod.contains(q) ||
          upc.contains(q) ||
          idTx.contains(q) ||
          auth.contains(q) ||
          dest.contains(q);
    }).toList();
  }

  double get _totalAcumulado {
    return _comprasFiltradas.fold<double>(0.0, (acc, item) => acc + item.monto);
  }

  Future<void> _mostrarComprobanteDetalle(PurchaseTransactionResult compra) async {
    PurchaseTransactionResult detalle = compra;
    try {
      detalle = await _service.consultarDetallePorUpc(compra.upc);
    } catch (_) {}

    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 560),
                child: PurchaseResultReceiptCard(
                  resultado: detalle,
                  onNuevaOperacion: () => Navigator.pop(ctx),
                ),
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
    final filtradas = _comprasFiltradas;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _filtroController,
                  onChanged: (val) => setState(() => _busqueda = val.trim()),
                  decoration: InputDecoration(
                    hintText: 'Buscar por producto, UPC, idTx, autorización o teléfono...',
                    prefixIcon: const Icon(Icons.search_rounded),
                    suffixIcon: _busqueda.isEmpty
                        ? null
                        : IconButton(
                            onPressed: () {
                              _filtroController.clear();
                              setState(() => _busqueda = '');
                            },
                            icon: const Icon(Icons.clear_rounded),
                          ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              IconButton.filledTonal(
                tooltip: 'Sincronizar historial con el servidor',
                onPressed: _cargando ? null : _cargarHistorial,
                icon: const Icon(Icons.sync_rounded),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 6),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: colorScheme.primaryContainer.withValues(alpha: 0.45),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              spacing: 12,
              runSpacing: 6,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.verified_rounded, size: 18, color: Colors.green.shade700),
                    const SizedBox(width: 8),
                    Text(
                      'Operaciones Exitosas: ${filtradas.length}',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                Text(
                  'Total operado: \$${_totalAcumulado.toStringAsFixed(2)} MXN',
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: colorScheme.primary,
                  ),
                ),
              ],
            ),
          ),
        ),
        Expanded(
          child: _construirListado(filtradas, colorScheme),
        ),
      ],
    );
  }

  Widget _construirListado(
    List<PurchaseTransactionResult> filtradas,
    ColorScheme colorScheme,
  ) {
    if (_cargando) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 12),
            Text('Consultando historial de compras exitosas...'),
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
              Icon(Icons.error_outline_rounded, size: 54, color: colorScheme.error),
              const SizedBox(height: 12),
              Text(_errorMensaje!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _cargarHistorial,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      );
    }

    if (filtradas.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.receipt_long_outlined, size: 60, color: colorScheme.outline),
              const SizedBox(height: 12),
              Text(
                'Aún no se registran compras exitosas con este filtro.',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 6),
              Text(
                'Las compras confirmadas en Web o Móvil aparecerán aquí de inmediato.',
                textAlign: TextAlign.center,
                style: TextStyle(color: colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _cargarHistorial,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
        itemCount: filtradas.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (context, index) {
          final item = filtradas[index];
          return _TarjetaCompraHistorial(
            compra: item,
            onTapDetalle: () => _mostrarComprobanteDetalle(item),
          );
        },
      ),
    );
  }
}

class _TarjetaCompraHistorial extends StatelessWidget {
  const _TarjetaCompraHistorial({
    required this.compra,
    required this.onTapDetalle,
  });

  final PurchaseTransactionResult compra;
  final VoidCallback onTapDetalle;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final esRecarga = compra.tipoOperacion.toUpperCase() == 'RECARGA';
    final tituloProducto = (compra.nombreProducto != null && compra.nombreProducto!.isNotEmpty)
        ? compra.nombreProducto!
        : (esRecarga ? 'Recarga Telefónica' : 'Pago de Servicio');

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTapDetalle,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: Colors.green.withValues(alpha: 0.14),
                child: Icon(
                  esRecarga ? Icons.phone_android_rounded : Icons.receipt_long_rounded,
                  color: Colors.green.shade800,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      tituloProducto,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Fecha: ${compra.fechaMostrar}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Ref/Tel: ${compra.destinoMostrar} • ID Tx: ${compra.idTx ?? compra.upc}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    '\$${compra.monto.toStringAsFixed(2)}',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w800,
                          color: colorScheme.primary,
                        ),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: Colors.green.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      'EXITOSA',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Colors.green.shade800,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 6),
              const Icon(Icons.chevron_right_rounded),
            ],
          ),
        ),
      ),
    );
  }
}

