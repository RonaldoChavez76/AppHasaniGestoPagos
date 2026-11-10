import 'dart:async';

import 'package:flutter/material.dart';

import '../state/session_controller.dart';
import 'catalog_screen.dart';
import 'login_screen.dart';

class HomeShellScreen extends StatefulWidget {
  const HomeShellScreen({
    required this.sessionController,
    super.key,
  });

  static const routeName = '/home';

  final SessionController sessionController;

  @override
  State<HomeShellScreen> createState() => _HomeShellScreenState();
}

class _HomeShellScreenState extends State<HomeShellScreen> {
  int _selectedIndex = 0;
  Timer? _sessionMonitorTimer;

  @override
  void initState() {
    super.initState();
    widget.sessionController.addListener(_onSessionChange);
    _sessionMonitorTimer = Timer.periodic(const Duration(seconds: 15), (_) {
      widget.sessionController.verificarVigenciaSesion();
    });
  }

  void _onSessionChange() {
    if (!mounted) return;
    if (!widget.sessionController.estaAutenticado) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
          builder: (_) => LoginScreen(sessionController: widget.sessionController),
        ),
        (route) => false,
      );
    } else {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _sessionMonitorTimer?.cancel();
    widget.sessionController.removeListener(_onSessionChange);
    super.dispose();
  }

  Future<void> _confirmarCierreSesion() async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar sesión segura'),
        content: const Text(
          '¿Deseas finalizar tu sesión activa en este dispositivo? Esto liberará tu cuenta para iniciar sesión en Web o Móvil.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.pop(ctx, true),
            icon: const Icon(Icons.logout_rounded),
            label: const Text('Cerrar sesión'),
          ),
        ],
      ),
    );

    if (confirmar == true && mounted) {
      await widget.sessionController.cerrarSesion();
    }
  }

  @override
  Widget build(BuildContext context) {
    final sesion = widget.sessionController.sesionActual;
    if (sesion == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final esPantallaAncha = constraints.maxWidth >= 860;
        final colorScheme = Theme.of(context).colorScheme;

        final paginas = <Widget>[
          _PanelResumenSesion(
            sessionController: widget.sessionController,
            onIrACatalogo: () => setState(() => _selectedIndex = 1),
            onIrAHistorial: () => setState(() => _selectedIndex = 2),
          ),
          CatalogScreen(
            sessionController: widget.sessionController,
            onProductoSeleccionado: (producto) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(
                    'Producto seleccionado: ${producto.nombre} (\$${producto.precio.toStringAsFixed(2)} MXN)',
                  ),
                  behavior: SnackBarBehavior.floating,
                ),
              );
            },
          ),
          const _PlaceholderModulo(
            icono: Icons.receipt_long_outlined,
            titulo: 'Historial de Compras Exitosas',
            descripcion: 'Módulo conectado a /api/gestopago/transacciones/historial/exitosas.',
          ),
        ];

        return Scaffold(
          appBar: AppBar(
            title: Row(
              children: [
                const Icon(Icons.account_balance_wallet_outlined),
                const SizedBox(width: 10),
                const Flexible(
                  child: Text(
                    'Ecosistema Digital GestoPago',
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const SizedBox(width: 12),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: colorScheme.primaryContainer,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        sesion.plataforma == 'WEB' ? Icons.computer_rounded : Icons.smartphone_rounded,
                        size: 15,
                        color: colorScheme.onPrimaryContainer,
                      ),
                      const SizedBox(width: 5),
                      Text(
                        'Sesión ${sesion.plataforma}',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: colorScheme.onPrimaryContainer,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            actions: [
              IconButton(
                tooltip: 'Verificar vigencia de sesión en el servidor',
                onPressed: () async {
                  final activa = await widget.sessionController.verificarVigenciaSesion();
                  if (!context.mounted) return;
                  if (activa) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Sesión única verificada y activa en el servidor.'),
                        behavior: SnackBarBehavior.floating,
                      ),
                    );
                  }
                },
                icon: const Icon(Icons.sync_lock_rounded),
              ),
              IconButton(
                tooltip: 'Cerrar sesión',
                onPressed: _confirmarCierreSesion,
                icon: const Icon(Icons.logout_rounded),
              ),
              const SizedBox(width: 8),
            ],
          ),
          body: Row(
            children: [
              if (esPantallaAncha)
                NavigationRail(
                  extended: constraints.maxWidth >= 1120,
                  selectedIndex: _selectedIndex,
                  onDestinationSelected: (idx) {
                    setState(() => _selectedIndex = idx);
                    widget.sessionController.verificarVigenciaSesion();
                  },
                  destinations: const [
                    NavigationRailDestination(
                      icon: Icon(Icons.verified_user_outlined),
                      selectedIcon: Icon(Icons.verified_user),
                      label: Text('Mi Cuenta y Sesión'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.storefront_outlined),
                      selectedIcon: Icon(Icons.storefront),
                      label: Text('Catálogo y Compras'),
                    ),
                    NavigationRailDestination(
                      icon: Icon(Icons.receipt_long_outlined),
                      selectedIcon: Icon(Icons.receipt_long),
                      label: Text('Historial Exitoso'),
                    ),
                  ],
                ),
              if (esPantallaAncha) const VerticalDivider(width: 1),
              Expanded(child: paginas[_selectedIndex]),
            ],
          ),
          bottomNavigationBar: esPantallaAncha
              ? null
              : NavigationBar(
                  selectedIndex: _selectedIndex,
                  onDestinationSelected: (idx) {
                    setState(() => _selectedIndex = idx);
                    widget.sessionController.verificarVigenciaSesion();
                  },
                  destinations: const [
                    NavigationDestination(
                      icon: Icon(Icons.verified_user_outlined),
                      selectedIcon: Icon(Icons.verified_user),
                      label: 'Mi Sesión',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.storefront_outlined),
                      selectedIcon: Icon(Icons.storefront),
                      label: 'Catálogo',
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.receipt_long_outlined),
                      selectedIcon: Icon(Icons.receipt_long),
                      label: 'Historial',
                    ),
                  ],
                ),
        );
      },
    );
  }
}

class _PanelResumenSesion extends StatelessWidget {
  const _PanelResumenSesion({
    required this.sessionController,
    required this.onIrACatalogo,
    required this.onIrAHistorial,
  });

  final SessionController sessionController;
  final VoidCallback onIrACatalogo;
  final VoidCallback onIrAHistorial;

  @override
  Widget build(BuildContext context) {
    final sesion = sessionController.sesionActual!;
    final colorScheme = Theme.of(context).colorScheme;
    final saldoTexto = sesion.saldoCuenta != null
        ? '\$${sesion.saldoCuenta!.toStringAsFixed(2)} MXN'
        : 'Sincronizando...';

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 880),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      colorScheme.primary,
                      colorScheme.primary.withValues(alpha: 0.82),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(22),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Flexible(
                          child: Text(
                            sesion.nombreCliente ?? sesion.correo,
                            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                        ),
                        Chip(
                          label: Text('2FA Verificado • ${sesion.plataforma}'),
                          backgroundColor: Colors.white,
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Saldo Disponible',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.85)),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      saldoTexto,
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.w800,
                          ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Cuenta: ${sesion.numeroCuenta ?? "4229215075268914"} • Correo: ${sesion.correo}',
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.9)),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                  side: BorderSide(color: colorScheme.outlineVariant),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.security_rounded, color: colorScheme.primary),
                          const SizedBox(width: 10),
                          Text(
                            'Estado de Sesión Única Activa',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Text(
                        'Tu sesión se encuentra protegida con JWT y vinculada exclusivamente a la plataforma ${sesion.plataforma}. '
                        'Si intentas iniciar sesión desde otro dispositivo, el servidor detectará esta sesión activa y solicitará confirmación antes de reemplazarla.',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: colorScheme.onSurfaceVariant,
                            ),
                      ),
                      const SizedBox(height: 16),
                      Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: [
                          FilledButton.icon(
                            onPressed: onIrACatalogo,
                            icon: const Icon(Icons.storefront_outlined),
                            label: const Text('Explorar Catálogo de Productos'),
                          ),
                          OutlinedButton.icon(
                            onPressed: onIrAHistorial,
                            icon: const Icon(Icons.receipt_long_outlined),
                            label: const Text('Consultar Historial de Compras'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PlaceholderModulo extends StatelessWidget {
  const _PlaceholderModulo({
    required this.icono,
    required this.titulo,
    required this.descripcion,
  });

  final IconData icono;
  final String titulo;
  final String descripcion;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icono, size: 64, color: colorScheme.primary),
            const SizedBox(height: 16),
            Text(
              titulo,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              descripcion,
              textAlign: TextAlign.center,
              style: TextStyle(color: colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}
