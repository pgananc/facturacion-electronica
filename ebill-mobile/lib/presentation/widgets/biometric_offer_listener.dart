import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../core/di/injection.dart';
import '../../core/services/biometric_service.dart';
import '../../core/storage/storage_service.dart';
import '../bloc/auth/auth_bloc.dart';

/// Tras un login con contraseña correcta, ofrece activar Face ID / huella.
class BiometricOfferListener extends StatelessWidget {
  const BiometricOfferListener({required this.child, super.key});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthBloc, AuthState>(
      listenWhen: (prev, curr) {
        if (curr is! AuthAuthenticated) return false;
        return getIt<StorageService>().pendingBiometricOffer;
      },
      listener: (context, state) async {
        final storage = getIt<StorageService>();
        await storage.clearPendingBiometricOffer();
        final bio = getIt<BiometricService>();
        if (bio.isBiometricEnabled) return;
        if (!await bio.isAvailable()) return;
        if (!context.mounted) return;
        final accept = await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Inicio rápido'),
            content: const Text(
              '¿Desea usar Face ID o huella para desbloquear la aplicación la próxima vez?',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: const Text('Ahora no'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: const Text('Activar'),
              ),
            ],
          ),
        );
        if (accept == true) {
          await bio.setBiometricEnabled(true);
        }
      },
      child: child,
    );
  }
}
