import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/network/api_client.dart';
import '../models/auth_models.dart';
import '../state/session_controller.dart';
import '../widgets/auth_text_field.dart';
import 'home_shell_screen.dart';

class TwoFactorScreen extends StatefulWidget {
  const TwoFactorScreen({
    required this.sessionController,
    required this.challenge,
    required this.correo,
    required this.password,
    required this.plataforma,
    required this.forzarCierreSesion,
    super.key,
  });

  final SessionController sessionController;
  final Login2faChallenge challenge;
  final String correo;
  final String password;
  final String plataforma;
  final bool forzarCierreSesion;

  @override
  State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}

class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final _formKey = GlobalKey<FormState>();
  final _otpController = TextEditingController();
  late Login2faChallenge _currentChallenge;
  late int _segundosRestantes;
  Timer? _timer;
  String? _errorMensaje;

  @override
  void initState() {
    super.initState();
    _currentChallenge = widget.challenge;
    _segundosRestantes = _currentChallenge.expiraEnSegundos;
    _iniciarTemporizador();
  }

  void _iniciarTemporizador() {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      if (_segundosRestantes <= 0) {
        timer.cancel();
      } else {
        setState(() {
          _segundosRestantes--;
        });
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otpController.dispose();
    super.dispose();
  }

  String _formatearTiempo(int totalSegundos) {
    final min = (totalSegundos ~/ 60).toString().padLeft(2, '0');
    final seg = (totalSegundos % 60).toString().padLeft(2, '0');
    return '$min:$seg';
  }

  Future<void> _reenviarCodigo() async {
    setState(() => _errorMensaje = null);
    try {
      final nuevoReto = await widget.sessionController.iniciarPreLogin(
        correo: widget.correo,
        password: widget.password,
        plataforma: widget.plataforma,
        forzarCierreSesion: true,
      );
      if (!mounted) return;
      setState(() {
        _currentChallenge = nuevoReto;
        _segundosRestantes = nuevoReto.expiraEnSegundos;
      });
      _iniciarTemporizador();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Nuevo código OTP de 6 dígitos generado en el servidor.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _errorMensaje = e.message);
    }
  }

  Future<void> _verificarCodigo() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _errorMensaje = null);

    try {
      await widget.sessionController.completarLoginCon2FA(
        correo: widget.correo,
        password: widget.password,
        codigo2FA: _otpController.text.trim(),
        plataforma: widget.plataforma,
        forzarCierreSesion: widget.forzarCierreSesion,
      );
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
          builder: (_) => HomeShellScreen(sessionController: widget.sessionController),
        ),
        (route) => false,
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _errorMensaje = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final cargando = widget.sessionController.cargando;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Verificación de Doble Factor (2FA)'),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                  side: BorderSide(color: colorScheme.outlineVariant),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(28),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        CircleAvatar(
                          radius: 34,
                          backgroundColor: colorScheme.primaryContainer,
                          child: Icon(
                            Icons.verified_user_outlined,
                            size: 36,
                            color: colorScheme.onPrimaryContainer,
                          ),
                        ),
                        const SizedBox(height: 20),
                        Text(
                          'Código de Seguridad (OTP)',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Ingresa el código temporal de 6 dígitos validado por el servidor para activar tu sesión única en ${widget.plataforma}.',
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: colorScheme.onSurfaceVariant,
                              ),
                        ),
                        const SizedBox(height: 20),
                        if (_currentChallenge.codigoOtp.isNotEmpty)
                          Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: colorScheme.secondaryContainer.withValues(alpha: 0.55),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: colorScheme.secondary.withValues(alpha: 0.3)),
                            ),
                            child: Column(
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(Icons.key_outlined, size: 18, color: colorScheme.primary),
                                    const SizedBox(width: 8),
                                    Text(
                                      'Código OTP generado para cuenta ${_currentChallenge.numeroCuenta}:',
                                      style: Theme.of(context).textTheme.labelMedium,
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    SelectableText(
                                      _currentChallenge.codigoOtp,
                                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                            fontWeight: FontWeight.w800,
                                            letterSpacing: 6,
                                            color: colorScheme.primary,
                                          ),
                                    ),
                                    const SizedBox(width: 8),
                                    TextButton.icon(
                                      onPressed: () {
                                        _otpController.text = _currentChallenge.codigoOtp;
                                        Clipboard.setData(
                                          ClipboardData(text: _currentChallenge.codigoOtp),
                                        );
                                      },
                                      icon: const Icon(Icons.copy_rounded, size: 16),
                                      label: const Text('Usar código'),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        const SizedBox(height: 20),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.timer_outlined,
                              size: 18,
                              color: _segundosRestantes > 60 ? colorScheme.primary : colorScheme.error,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              _segundosRestantes > 0
                                  ? 'Expira en ${_formatearTiempo(_segundosRestantes)}'
                                  : 'El código ha expirado. Solicita uno nuevo.',
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                color: _segundosRestantes > 60 ? colorScheme.primary : colorScheme.error,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 20),
                        AuthTextField(
                          controller: _otpController,
                          label: 'Código de 6 dígitos',
                          hintText: 'Ej. 482910',
                          icon: Icons.pin_outlined,
                          keyboardType: TextInputType.number,
                          maxLength: 6,
                          validator: (value) {
                            final text = (value ?? '').trim();
                            if (text.isEmpty) {
                              return 'Ingresa el código OTP de 6 dígitos.';
                            }
                            if (!RegExp(r'^[0-9]{6}$').hasMatch(text)) {
                              return 'El código debe contener exactamente 6 dígitos numéricos.';
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
                        const SizedBox(height: 24),
                        ElevatedButton.icon(
                          onPressed: cargando || _segundosRestantes <= 0 ? null : _verificarCodigo,
                          icon: cargando
                              ? const SizedBox(
                                  width: 20,
                                  height: 20,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Icon(Icons.check_circle_outline),
                          label: Text(
                            cargando ? 'Verificando en el servidor...' : 'Confirmar y Acceder',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextButton.icon(
                          onPressed: cargando ? null : _reenviarCodigo,
                          icon: const Icon(Icons.refresh_rounded),
                          label: const Text('Solicitar un nuevo código OTP'),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

