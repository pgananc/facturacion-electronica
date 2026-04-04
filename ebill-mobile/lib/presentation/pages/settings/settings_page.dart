import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/config/app_config.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/settings/settings_bloc.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  @override
  void initState() {
    super.initState();
    context
        .read<SettingsBloc>()
        .add(const SettingsCheckBiometricAvailability());
  }

  static const _termsUrl = 'https://wallparisoft.com';
  static const _privacyUrl = 'https://wallparisoft.com';

  Future<void> _openUrl(String url) async {
    final uri = Uri.parse(url);
    final ok = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!mounted) return;
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo abrir el enlace')),
      );
    }
  }

  Future<void> _clearLocalCache() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Borrar caché local'),
        content: const Text(
          'Se vaciará la caché de imágenes en memoria. No se cerrará la sesión.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Borrar'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    imageCache.clear();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: const Text('Caché local borrada'),
        backgroundColor: Theme.of(context).colorScheme.primary,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Ajustes')),
      body: BlocBuilder<SettingsBloc, SettingsState>(
        builder: (context, settingsState) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const _SectionHeader(title: 'APARIENCIA', icon: Icons.palette_outlined),
              const SizedBox(height: 8),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Tema de la aplicación',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 12),
                      SizedBox(
                        width: double.infinity,
                        child: SegmentedButton<ThemeMode>(
                          segments: const [
                            ButtonSegment(
                              value: ThemeMode.light,
                              label: Text('Claro'),
                              icon: Icon(Icons.light_mode, size: 18),
                            ),
                            ButtonSegment(
                              value: ThemeMode.dark,
                              label: Text('Oscuro'),
                              icon: Icon(Icons.dark_mode, size: 18),
                            ),
                            ButtonSegment(
                              value: ThemeMode.system,
                              label: Text('Sistema'),
                              icon: Icon(Icons.brightness_auto, size: 18),
                            ),
                          ],
                          selected: {settingsState.themeMode},
                          onSelectionChanged: (set) {
                            context
                                .read<SettingsBloc>()
                                .add(SettingsThemeModeChanged(set.first));
                          },
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              const _SectionHeader(title: 'NOTIFICACIONES', icon: Icons.notifications_outlined),
              const SizedBox(height: 8),
              Card(
                child: Column(
                  children: [
                    SwitchListTile(
                      title: const Text('Alertas y avisos'),
                      subtitle: const Text(
                        'Recibir notificaciones de autorizaciones SRI y alertas del sistema.',
                      ),
                      value: settingsState.notificationsEnabled,
                      onChanged: (v) {
                        context.read<SettingsBloc>().add(SettingsNotificationsChanged(v));
                      },
                    ),
                    const Divider(height: 1),
                    const SwitchListTile(
                      title: Text('Sonidos'),
                      subtitle: Text('Próximamente'),
                      value: false,
                      onChanged: null,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const _SectionHeader(title: 'SEGURIDAD', icon: Icons.fingerprint),
              const SizedBox(height: 8),
              Card(
                child: SwitchListTile(
                  secondary: Icon(
                    Icons.fingerprint,
                    color: theme.colorScheme.primary,
                  ),
                  title: const Text('Face ID / Huella'),
                  subtitle: Text(
                    settingsState.biometricAvailable
                        ? 'Usar biometría para desbloquear la app'
                        : 'No disponible en este dispositivo',
                  ),
                  value: settingsState.biometricEnabled,
                  onChanged: settingsState.biometricAvailable
                      ? (v) {
                          context
                              .read<SettingsBloc>()
                              .add(SettingsBiometricChanged(v));
                        }
                      : null,
                ),
              ),
              const SizedBox(height: 20),
              const _SectionHeader(title: 'CUENTA', icon: Icons.person_outline),
              const SizedBox(height: 8),
              Card(
                child: Column(
                  children: [
                    BlocBuilder<AuthBloc, AuthState>(
                      builder: (context, authState) {
                        final username = authState is AuthAuthenticated
                            ? authState.user.username
                            : '—';
                        return ListTile(
                          leading: Icon(
                            Icons.alternate_email,
                            color: theme.colorScheme.primary,
                          ),
                          title: const Text('Usuario'),
                          subtitle: Text(username),
                        );
                      },
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.person_outline, color: theme.colorScheme.primary),
                      title: const Text('Perfil de usuario'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/profile'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.lock_outline, color: theme.colorScheme.primary),
                      title: const Text('Cambiar contraseña'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/profile/change-password'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.apartment, color: theme.colorScheme.primary),
                      title: const Text('Cambiar compañía'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/select-company'),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const _SectionHeader(title: 'DATOS', icon: Icons.storage_outlined),
              const SizedBox(height: 8),
              Card(
                child: ListTile(
                  leading: Icon(Icons.delete_sweep_outlined, color: theme.colorScheme.primary),
                  title: const Text('Borrar caché local'),
                  subtitle: const Text('Caché de imágenes en memoria'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: _clearLocalCache,
                ),
              ),
              const SizedBox(height: 20),
              const _SectionHeader(title: 'ACERCA DE', icon: Icons.info_outline),
              const SizedBox(height: 8),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: Icon(Icons.phone_android, color: theme.colorScheme.primary),
                      title: const Text('Versión'),
                      subtitle: const Text('eBill Mobile ${AppConfig.appVersion}'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(
                        Icons.description_outlined,
                        color: theme.colorScheme.primary,
                      ),
                      title: const Text('Términos y condiciones'),
                      trailing: const Icon(Icons.open_in_new, size: 20),
                      onTap: () => _openUrl(_termsUrl),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(
                        Icons.privacy_tip_outlined,
                        color: theme.colorScheme.primary,
                      ),
                      title: const Text('Política de privacidad'),
                      trailing: const Icon(Icons.open_in_new, size: 20),
                      onTap: () => _openUrl(_privacyUrl),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 32),
            ],
          );
        },
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title, required this.icon});

  final String title;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Row(
        children: [
          Icon(icon, size: 18, color: theme.colorScheme.primary),
          const SizedBox(width: 8),
          Text(
            title,
            style: theme.textTheme.titleSmall?.copyWith(
              color: theme.colorScheme.primary,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }
}
