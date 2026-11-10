import 'package:flutter/material.dart';

import '../widgets/auth_text_field.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  static const routeName = '/register';

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  bool _isPasswordObscured = true;
  bool _isConfirmationObscured = true;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta'), centerTitle: true),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(
                    Icons.person_add_alt_1_outlined,
                    size: 64,
                    color: colorScheme.primary,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Regístrate',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineMedium
                        ?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 28),
                  const AuthTextField(
                    label: 'Nombre',
                    icon: Icons.person_outline,
                  ),
                  const SizedBox(height: 16),
                  const AuthTextField(
                    label: 'Correo electrónico',
                    icon: Icons.email_outlined,
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  AuthTextField(
                    label: 'Contraseña',
                    icon: Icons.lock_outline,
                    obscureText: _isPasswordObscured,
                    onToggleVisibility: () {
                      setState(
                        () => _isPasswordObscured = !_isPasswordObscured,
                      );
                    },
                  ),
                  const SizedBox(height: 16),
                  AuthTextField(
                    label: 'Confirmar contraseña',
                    icon: Icons.lock_reset_outlined,
                    obscureText: _isConfirmationObscured,
                    onToggleVisibility: () {
                      setState(() {
                        _isConfirmationObscured = !_isConfirmationObscured;
                      });
                    },
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: () {
                      // Aquí se enviarán los datos de registro al backend.
                    },
                    child: const Text('Registrarse'),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text('Ya tengo una cuenta. Volver al Login'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
