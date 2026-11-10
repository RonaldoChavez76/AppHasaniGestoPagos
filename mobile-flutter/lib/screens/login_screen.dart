import 'package:flutter/material.dart';

import '../core/config/api_config.dart';
import '../core/network/api_client.dart';
import '../models/auth_models.dart';
import '../state/session_controller.dart';
import '../widgets/auth_text_field.dart';
import 'register_screen.dart';
import 'two_factor_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({
    this.sessionController,
    super.key,
  });

  static const routeName = '/login';

  final SessionController? sessionController;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _correoController = TextEditingController();
  final _passwordController = TextEditingController();

  late final SessionController _controller;
  bool _ownsController = false;
  bool _isPasswordObscured = true;
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    if (widget.sessionController != null) {
      _controller = widget.sessionController!;
    } else {
      _controller = SessionController();
      _ownsController = true;
    }
    _controller.addListener(_onControllerChanged);
  }

  void _onControllerChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerChanged);
    if (_ownsController) {
      _controller.dispose();
    }
    _correoController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _llenarUsuarioPrueba() {
    setState(() {
      _correoController.text = 'ana.lopez.prueba@example.com';
      _passwordController.text = 'Prueba2026!';
      _errorMensaje = null;
    });
  }

  Future<void> _mostrarDialogoConflictoSesion(ConflictoSesion conflicto) async {
    final colorScheme = Theme.of(context).colorScheme;
    final confirmar = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        icon: Icon(
          Icons.warning_amber_rounded,
          size: 46,
          color: colorScheme.error,
        ),
        title: Text(
          'Sesión activa en ${conflicto.plataformaActiva}',
          textAlign: TextAlign.center,
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'El sistema detectó que tu cuenta ya tiene una sesión activa en la plataforma ${conflicto.plataformaActiva}.',
            ),
            const SizedBox(height: 12),
            const Text(
              'Por políticas de seguridad bancaria, solo puedes mantener una sesión activa a la vez. ¿Deseas finalizar la sesión anterior para ingresar aquí?',
              style: TextStyle(fontWeight: FontWeight.w500),
            ),
            if (conflicto.fechaInicio != null) ...[
              const SizedBox(height: 12),
              Text(
                'Inicio de sesión previa: ${conflicto.fechaInicio}',
                style: Theme.of(ctx).textTheme.bodySmall,
              ),
            ],
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton.icon(
            onPressed: () => Navigator.pop(ctx, true),
            icon: const Icon(Icons.phonelink_erase_outlined),
            label: const Text('Cerrar sesión anterior y continuar'),
          ),
        ],
      ),
    );

    if (confirmar == true && mounted) {
      await _iniciarAutenticacion(forzarCierreSesion: true);
    }
  }

  Future<void> _iniciarAutenticacion({bool forzarCierreSesion = false}) async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _errorMensaje = null);
    _controller.limpiarAlertaSesion();

    final plataforma = ApiConfig.plataformaActual;
    final correo = _correoController.text.trim();
    final password = _passwordController.text;

    try {
      final challenge = await _controller.iniciarPreLogin(
        correo: correo,
        password: password,
        plataforma: plataforma,
        forzarCierreSesion: forzarCierreSesion,
      );

      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => TwoFactorScreen(
            sessionController: _controller,
            challenge: challenge,
            correo: correo,
            password: password,
            plataforma: plataforma,
            forzarCierreSesion: forzarCierreSesion,
          ),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.statusCode == 409) {
        final conflicto = e.conflictoSesion ??
            ConflictoSesion(
              sesionActivaExistente: true,
              plataformaActiva: plataforma == 'WEB' ? 'MOVIL' : 'WEB',
              accionRequerida: e.message,
            );
        await _mostrarDialogoConflictoSesion(conflicto);
      } else {
        setState(() => _errorMensaje = e.message);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final cargando = _controller.cargando;
    final alertaSesion = _controller.mensajeAlertaSesion;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _AuthLogo(colorScheme: colorScheme),
                    const SizedBox(height: 20),
                    Text(
                      'Bienvenido a GestoPago',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Comercio Digital con Autenticación 2FA y Sesión Única',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                    ),
                    const SizedBox(height: 20),
                    _ConfiguracionPlataformaBar(),
                    if (alertaSesion != null) ...[
                      const SizedBox(height: 16),
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: colorScheme.tertiaryContainer,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.info_outline, color: colorScheme.onTertiaryContainer),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                alertaSesion,
                                style: TextStyle(
                                  color: colorScheme.onTertiaryContainer,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    AuthTextField(
                      controller: _correoController,
                      label: 'Correo electrónico',
                      hintText: 'ana.lopez.prueba@example.com',
                      icon: Icons.email_outlined,
                      keyboardType: TextInputType.emailAddress,
                      validator: (value) {
                        final text = (value ?? '').trim();
                        if (text.isEmpty) {
                          return 'Ingresa tu correo electrónico.';
                        }
                        if (!text.contains('@') || !text.contains('.')) {
                          return 'Ingresa un correo electrónico válido.';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    AuthTextField(
                      controller: _passwordController,
                      label: 'Contraseña',
                      icon: Icons.lock_outline,
                      obscureText: _isPasswordObscured,
                      onToggleVisibility: () {
                        setState(() => _isPasswordObscured = !_isPasswordObscured);
                      },
                      validator: (value) {
                        if (value == null || value.isEmpty) {
                          return 'Ingresa tu contraseña.';
                        }
                        return null;
                      },
                    ),
                    if (_errorMensaje != null) ...[
                      const SizedBox(height: 14),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: colorScheme.errorContainer,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.error_outline, color: colorScheme.onErrorContainer),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                _errorMensaje!,
                                style: TextStyle(color: colorScheme.onErrorContainer),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                    const SizedBox(height: 22),
                    ElevatedButton.icon(
                      onPressed: cargando ? null : () => _iniciarAutenticacion(),
                      icon: cargando
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.login_rounded),
                      label: Text(
                        cargando ? 'Validando credenciales...' : 'Iniciar Sesión con 2FA',
                      ),
                    ),
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: cargando ? null : _llenarUsuarioPrueba,
                      icon: const Icon(Icons.bolt_rounded, size: 18),
                      label: const Text('Cargar credenciales de prueba (Ana)'),
                    ),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: () =>
                          Navigator.pushNamed(context, RegisterScreen.routeName),
                      child: const Text('¿No tienes cuenta? Regístrate aquí'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ConfiguracionPlataformaBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: colorScheme.outlineVariant),
      ),
      child: Column(
        children: [
          ValueListenableBuilder<String>(
            valueListenable: ApiConfig.plataformaActivaNotifier,
            builder: (context, plataforma, _) {
              return Wrap(
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 8,
                runSpacing: 8,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        plataforma == 'WEB'
                            ? Icons.computer_rounded
                            : Icons.smartphone_rounded,
                        size: 18,
                        color: colorScheme.primary,
                      ),
                      const SizedBox(width: 6),
                      const Text(
                        'Plataforma:',
                        style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                  SegmentedButton<String>(
                    showSelectedIcon: false,
                    segments: const [
                      ButtonSegment(value: 'MOVIL', label: Text('MÓVIL')),
                      ButtonSegment(value: 'WEB', label: Text('WEB')),
                    ],
                    selected: {plataforma},
                    onSelectionChanged: (selection) {
                      ApiConfig.plataformaActivaNotifier.value = selection.first;
                    },
                    style: const ButtonStyle(
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                ],
              );
            },
          ),
          const Divider(height: 14),
          ValueListenableBuilder<BackendEnvironment>(
            valueListenable: ApiConfig.environmentNotifier,
            builder: (context, env, _) {
              return Row(
                children: [
                  Icon(Icons.cloud_done_outlined, size: 18, color: colorScheme.primary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<BackendEnvironment>(
                        isExpanded: true,
                        isDense: true,
                        value: env,
                        items: BackendEnvironment.values
                            .map(
                              (item) => DropdownMenuItem(
                                value: item,
                                child: Text(
                                  item.etiqueta,
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (nuevo) {
                          if (nuevo != null) {
                            ApiConfig.environmentNotifier.value = nuevo;
                          }
                        },
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class _AuthLogo extends StatelessWidget {
  const _AuthLogo({required this.colorScheme});

  final ColorScheme colorScheme;

  @override
  Widget build(BuildContext context) {
    return CircleAvatar(
      radius: 38,
      backgroundColor: colorScheme.primaryContainer,
      child: Icon(
        Icons.lock_person_outlined,
        size: 40,
        color: colorScheme.onPrimaryContainer,
      ),
    );
  }
}
