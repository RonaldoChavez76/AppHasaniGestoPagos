import 'package:flutter/material.dart';

import 'screens/home_shell_screen.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';
import 'state/session_controller.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const AuthShellApp());
}

class AuthShellApp extends StatefulWidget {
  const AuthShellApp({
    this.sessionController,
    super.key,
  });

  final SessionController? sessionController;

  @override
  State<AuthShellApp> createState() => _AuthShellAppState();
}

class _AuthShellAppState extends State<AuthShellApp> {
  late final SessionController _sessionController;

  @override
  void initState() {
    super.initState();
    _sessionController = widget.sessionController ?? SessionController();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'GestoPago Comercio Digital',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      initialRoute: LoginScreen.routeName,
      routes: {
        LoginScreen.routeName: (_) => LoginScreen(sessionController: _sessionController),
        RegisterScreen.routeName: (_) => const RegisterScreen(),
        HomeShellScreen.routeName: (_) => HomeShellScreen(sessionController: _sessionController),
      },
    );
  }
}
