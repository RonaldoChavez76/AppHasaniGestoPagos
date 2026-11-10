import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/network/api_client.dart';
import '../models/product_models.dart';
import '../models/transaction_models.dart';
import '../services/transaction_service.dart';
import '../state/session_controller.dart';
import '../widgets/auth_text_field.dart';

class PurchaseScreen extends StatefulWidget {
  const PurchaseScreen({
    required this.producto,
    required this.sessionController,
    this.transactionService,
    this.onVerHistorial,
    super.key,
  });

  final CatalogProduct producto;
  final SessionController sessionController;
  final TransactionService? transactionService;
  final VoidCallback? onVerHistorial;

  @override
  State<PurchaseScreen> createState() => _PurchaseScreenState();
}

class _PurchaseScreenState extends State<PurchaseScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _destinoController;
  late final TextEditingController _montoController;
  late final TextEditingController _cuentaController;
  final _codigo2faController = TextEditingController();

  late final TransactionService _service;

  bool _procesando = false;
  bool _solicitandoOtp = false;
  bool _simularProveedor = true;
  String? _otpGeneradoServidor;
  String? _errorFormulario;
  PurchaseTransactionResult? _resultadoOperacion;

  @override
  void initState() {
    super.initState();
    _service = widget.transactionService ??
        TransactionService(apiClient: widget.sessionController.apiClient);

    final sesion = widget.sessionController.sesionActual;
    _cuentaController = TextEditingController(
      text: sesion?.numeroCuenta ?? '4229215075268914',
    );
    _destinoController = TextEditingController(
      text: widget.producto.esRecarga ? '5512345678' : '9876543210',
    );
    final precioInicial = widget.producto.precio > 0 ? widget.producto.precio : 50.0;
    _montoController = TextEditingController(
      text: precioInicial.toStringAsFixed(2),
    );
  }

  @override
  void dispose() {
    _destinoController.dispose();
    _montoController.dispose();
    _cuentaController.dispose();
    _codigo2faController.dispose();
    super.dispose();
  }

  Future<void> _generarCodigo2FA() async {
    final cuenta = _cuentaController.text.trim();
    if (cuenta.isEmpty) {
      setState(() => _errorFormulario = 'Ingresa primero el número de cuenta bancaria.');
      return;
    }
    setState(() {
      _solicitandoOtp = true;
      _errorFormulario = null;
    });
    try {
      final codigo = await _service.solicitarCodigo2FAParaCompra(cuenta);
      if (!mounted) return;
      setState(() {
        _otpGeneradoServidor = codigo;
        _codigo2faController.text = codigo;
        _solicitandoOtp = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _errorFormulario = e.message;
        _solicitandoOtp = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorFormulario = 'No fue posible generar el código OTP en el servidor.';
        _solicitandoOtp = false;
      });
    }
  }

  Future<void> _solicitarConfirmacionYComprar() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _errorFormulario = null);

    final monto = double.tryParse(_montoController.text.trim()) ?? 0.0;
    final destino = _destinoController.text.trim();
    final cuenta = _cuentaController.text.trim();

    final confirmar = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        icon: Icon(
          Icons.verified_outlined,
          size: 44,
          color: Theme.of(ctx).colorScheme.primary,
        ),
        title: const Text(
          'Confirmar Intención de Compra',
          textAlign: TextAlign.center,
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Por favor verifica los datos antes de autorizar el cargo a tu cuenta:',
            ),
            const SizedBox(height: 14),
            _FilaResumenModal(etiqueta: 'Producto:', valor: widget.producto.nombre),
            _FilaResumenModal(etiqueta: 'Servicio:', valor: widget.producto.servicio),
            _FilaResumenModal(
              etiqueta: widget.producto.esRecarga ? 'Teléfono destino:' : 'Referencia:',
              valor: destino,
            ),
            _FilaResumenModal(etiqueta: 'Cuenta de cargo:', valor: cuenta),
            _FilaResumenModal(
              etiqueta: 'Importe total:',
              valor: '\$${monto.toStringAsFixed(2)} MXN',
              destacar: true,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.pop(ctx, true),
            icon: const Icon(Icons.lock_open_rounded),
            label: const Text('Confirmar y Pagar'),
          ),
        ],
      ),
    );

    if (confirmar != true || !mounted) return;

    setState(() => _procesando = true);
    final resultado = await _service.procesarCompra(
      producto: widget.producto,
      numeroCuenta: cuenta,
      monto: monto,
      destino: destino,
      codigo2FA: _codigo2faController.text.trim(),
      simular: _simularProveedor,
    );

    if (!mounted) return;
    if (resultado.exitoso && resultado.nuevoSaldoCuenta != null) {
      widget.sessionController.actualizarSaldoLocal(resultado.nuevoSaldoCuenta);
    }
    setState(() {
      _procesando = false;
      _resultadoOperacion = resultado;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          _resultadoOperacion == null
              ? 'Realizar Compra GestoPago'
              : 'Resultado de la Operación',
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 580),
              child: _resultadoOperacion != null
                  ? PurchaseResultReceiptCard(
                      resultado: _resultadoOperacion!,
                      onNuevaOperacion: () => Navigator.pop(context, _resultadoOperacion),
                      onIrAHistorial: widget.onVerHistorial == null
                          ? null
                          : () {
                              Navigator.pop(context, _resultadoOperacion);
                              widget.onVerHistorial!();
                            },
                    )
                  : _construirFormularioCompra(context),
            ),
          ),
        ),
      ),
    );
  }

  Widget _construirFormularioCompra(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final saldoActual = widget.sessionController.sesionActual?.saldoCuenta;
    final precioFijo = widget.producto.precio > 0 && widget.producto.esRecarga;

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: colorScheme.primaryContainer.withValues(alpha: 0.55),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 24,
                      backgroundColor: colorScheme.primary,
                      child: Icon(
                        widget.producto.esRecarga
                            ? Icons.phone_android_rounded
                            : Icons.receipt_long_rounded,
                        color: colorScheme.onPrimary,
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.producto.nombre,
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                          Text(
                            '${widget.producto.servicio} • ID #${widget.producto.idProducto}',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ],
                      ),
                    ),
                    if (saldoActual != null)
                      Chip(
                        label: Text('Saldo: \$${saldoActual.toStringAsFixed(2)}'),
                        visualDensity: VisualDensity.compact,
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              AuthTextField(
                controller: _destinoController,
                label: widget.producto.esRecarga
                    ? 'Número telefónico (10 dígitos)'
                    : 'Referencia de pago del servicio',
                hintText: widget.producto.esRecarga ? '5512345678' : 'Referencia de recibo',
                icon: widget.producto.esRecarga
                    ? Icons.phone_iphone_rounded
                    : Icons.qr_code_rounded,
                keyboardType: widget.producto.esRecarga
                    ? TextInputType.phone
                    : TextInputType.text,
                maxLength: widget.producto.esRecarga ? 10 : 40,
                validator: (val) {
                  final text = (val ?? '').trim();
                  if (text.isEmpty) {
                    return widget.producto.esRecarga
                        ? 'Ingresa el número telefónico de 10 dígitos.'
                        : 'Ingresa la referencia del servicio.';
                  }
                  if (widget.producto.esRecarga && !RegExp(r'^[0-9]{10}$').hasMatch(text)) {
                    return 'El teléfono debe tener exactamente 10 dígitos numéricos.';
                  }
                  if (!widget.producto.esRecarga && text.length < 4) {
                    return 'La referencia debe contener al menos 4 caracteres.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              AuthTextField(
                controller: _montoController,
                label: 'Importe de la operación (MXN)',
                icon: Icons.attach_money_rounded,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                validator: (val) {
                  final parsed = double.tryParse((val ?? '').trim());
                  if (parsed == null || parsed <= 0) {
                    return 'Ingresa un monto válido mayor a \$0.00.';
                  }
                  if (precioFijo && (parsed - widget.producto.precio).abs() > 0.01) {
                    return 'El monto para este paquete es fijo de \$${widget.producto.precio.toStringAsFixed(2)}.';
                  }
                  if (saldoActual != null && parsed > saldoActual) {
                    return 'Saldo insuficiente en la cuenta (\$${saldoActual.toStringAsFixed(2)} disponibles).';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              AuthTextField(
                controller: _cuentaController,
                label: 'Cuenta bancaria de cargo',
                icon: Icons.credit_card_rounded,
                keyboardType: TextInputType.number,
                validator: (val) {
                  final text = (val ?? '').trim();
                  if (text.isEmpty) {
                    return 'Ingresa el número de cuenta bancaria.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: AuthTextField(
                      controller: _codigo2faController,
                      label: 'Código 2FA OTP (6 dígitos)',
                      hintText: 'Solicita o ingresa tu OTP',
                      icon: Icons.security_rounded,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                    ),
                  ),
                  const SizedBox(width: 10),
                  SizedBox(
                    height: 54,
                    child: FilledButton.tonalIcon(
                      onPressed: _solicitandoOtp ? null : _generarCodigo2FA,
                      icon: _solicitandoOtp
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.key_rounded),
                      label: const Text('Generar OTP'),
                    ),
                  ),
                ],
              ),
              if (_otpGeneradoServidor != null) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.check_circle_rounded, size: 16, color: colorScheme.primary),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        'Código OTP generado en el servidor y autocompletado: $_otpGeneradoServidor',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: colorScheme.primary,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              SwitchListTile.adaptive(
                contentPadding: EdgeInsets.zero,
                title: const Text(
                  'Modo evaluación bancaria (Evitar bloqueo de IP de GestoPago)',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                ),
                subtitle: const Text(
                  'Ejecuta validación real en BD, 2FA, descuento atómico de saldo y registro de transacción.',
                  style: TextStyle(fontSize: 12),
                ),
                value: _simularProveedor,
                onChanged: (val) => setState(() => _simularProveedor = val),
              ),
              if (_errorFormulario != null) ...[
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: colorScheme.errorContainer,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    _errorFormulario!,
                    style: TextStyle(color: colorScheme.onErrorContainer),
                  ),
                ),
              ],
              const SizedBox(height: 20),
              ElevatedButton.icon(
                onPressed: _procesando ? null : _solicitarConfirmacionYComprar,
                icon: _procesando
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.payments_outlined),
                label: Text(
                  _procesando ? 'Procesando en GestoPago...' : 'Continuar a Confirmación',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class PurchaseResultReceiptCard extends StatelessWidget {
  const PurchaseResultReceiptCard({
    required this.resultado,
    required this.onNuevaOperacion,
    this.onIrAHistorial,
    super.key,
  });

  final PurchaseTransactionResult resultado;
  final VoidCallback onNuevaOperacion;
  final VoidCallback? onIrAHistorial;

  @override
  Widget build(BuildContext context) {
    final config = _EstiloEstadoResultado.desde(resultado.estadoVisual, Theme.of(context).colorScheme);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(22),
        side: BorderSide(color: config.colorBorde, width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.all(26),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            CircleAvatar(
              radius: 36,
              backgroundColor: config.colorFondoIcono,
              child: Icon(config.icono, size: 40, color: config.colorIcono),
            ),
            const SizedBox(height: 16),
            Text(
              config.tituloEstado,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: config.colorIcono,
                  ),
            ),
            const SizedBox(height: 6),
            Text(
              resultado.mensaje,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.4),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  _FilaDetalleComprobante(
                    etiqueta: 'Estado de la operación',
                    valor: config.etiquetaCorta,
                  ),
                  _FilaDetalleComprobante(
                    etiqueta: 'Producto adquirido',
                    valor: resultado.nombreProducto ?? 'Producto #${resultado.idProducto ?? "-"}',
                  ),
                  _FilaDetalleComprobante(
                    etiqueta: 'Importe de la operación',
                    valor: '\$${resultado.monto.toStringAsFixed(2)} MXN',
                  ),
                  _FilaDetalleComprobante(
                    etiqueta: 'Destino / Referencia',
                    valor: resultado.destinoMostrar,
                  ),
                  _FilaDetalleComprobante(
                    etiqueta: 'Folio UPC',
                    valor: resultado.upc,
                  ),
                  if (resultado.idTx != null)
                    _FilaDetalleComprobante(
                      etiqueta: 'Referencia GestoPago (idTx)',
                      valor: resultado.idTx!,
                    ),
                  if (resultado.numAutorizacion != null)
                    _FilaDetalleComprobante(
                      etiqueta: 'Número de Autorización',
                      valor: resultado.numAutorizacion!,
                    ),
                  _FilaDetalleComprobante(
                    etiqueta: 'Fecha y hora',
                    valor: resultado.fechaMostrar,
                  ),
                  if (resultado.numeroCuenta != null)
                    _FilaDetalleComprobante(
                      etiqueta: 'Cuenta bancaria',
                      valor: resultado.numeroCuenta!,
                    ),
                  if (resultado.nuevoSaldoCuenta != null)
                    _FilaDetalleComprobante(
                      etiqueta: 'Nuevo saldo disponible',
                      valor: '\$${resultado.nuevoSaldoCuenta!.toStringAsFixed(2)} MXN',
                    ),
                  if (resultado.pin != null && resultado.pin!.isNotEmpty) ...[
                    const Divider(height: 18),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            'PIN / Código: ${resultado.pin}',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ),
                        IconButton(
                          onPressed: () {
                            Clipboard.setData(ClipboardData(text: resultado.pin!));
                          },
                          icon: const Icon(Icons.copy_rounded, size: 18),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 22),
            if (resultado.exitoso && onIrAHistorial != null) ...[
              FilledButton.icon(
                onPressed: onIrAHistorial,
                icon: const Icon(Icons.receipt_long_rounded),
                label: const Text('Ver en Mi Historial de Compras Exitosas'),
              ),
              const SizedBox(height: 10),
            ],
            OutlinedButton.icon(
              onPressed: onNuevaOperacion,
              icon: const Icon(Icons.arrow_back_rounded),
              label: const Text('Volver al Catálogo'),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilaResumenModal extends StatelessWidget {
  const _FilaResumenModal({
    required this.etiqueta,
    required this.valor,
    this.destacar = false,
  });

  final String etiqueta;
  final String valor;
  final bool destacar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(etiqueta, style: const TextStyle(fontSize: 13)),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              valor,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontSize: destacar ? 15 : 13,
                fontWeight: destacar ? FontWeight.w800 : FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FilaDetalleComprobante extends StatelessWidget {
  const _FilaDetalleComprobante({
    required this.etiqueta,
    required this.valor,
  });

  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            etiqueta,
            style: TextStyle(
              fontSize: 13,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              valor,
              textAlign: TextAlign.end,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}

class _EstiloEstadoResultado {
  const _EstiloEstadoResultado({
    required this.tituloEstado,
    required this.etiquetaCorta,
    required this.icono,
    required this.colorIcono,
    required this.colorFondoIcono,
    required this.colorBorde,
  });

  final String tituloEstado;
  final String etiquetaCorta;
  final IconData icono;
  final Color colorIcono;
  final Color colorFondoIcono;
  final Color colorBorde;

  static _EstiloEstadoResultado desde(EstadoCompraResultado estado, ColorScheme scheme) {
    switch (estado) {
      case EstadoCompraResultado.exitosa:
        return _EstiloEstadoResultado(
          tituloEstado: '¡Compra Exitosa!',
          etiquetaCorta: 'EXITOSA (Confirmada)',
          icono: Icons.check_circle_rounded,
          colorIcono: Colors.green.shade800,
          colorFondoIcono: Colors.green.withValues(alpha: 0.16),
          colorBorde: Colors.green.shade400,
        );
      case EstadoCompraResultado.rechazada:
        return _EstiloEstadoResultado(
          tituloEstado: 'Compra Rechazada',
          etiquetaCorta: 'RECHAZADA (Sin cargo)',
          icono: Icons.cancel_rounded,
          colorIcono: scheme.error,
          colorFondoIcono: scheme.errorContainer,
          colorBorde: scheme.error,
        );
      case EstadoCompraResultado.pendiente:
        return _EstiloEstadoResultado(
          tituloEstado: 'Compra Pendiente de Conciliación',
          etiquetaCorta: 'PENDIENTE (En verificación)',
          icono: Icons.hourglass_top_rounded,
          colorIcono: Colors.amber.shade900,
          colorFondoIcono: Colors.amber.withValues(alpha: 0.2),
          colorBorde: Colors.amber.shade700,
        );
      case EstadoCompraResultado.errorComunicacion:
        return _EstiloEstadoResultado(
          tituloEstado: 'Error de Comunicación',
          etiquetaCorta: 'ERROR DE COMUNICACIÓN',
          icono: Icons.wifi_off_rounded,
          colorIcono: Colors.deepOrange.shade800,
          colorFondoIcono: Colors.deepOrange.withValues(alpha: 0.16),
          colorBorde: Colors.deepOrange.shade400,
        );
    }
  }
}
