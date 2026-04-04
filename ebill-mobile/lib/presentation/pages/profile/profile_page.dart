import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/config/app_config.dart';
import '../../../core/di/injection.dart';
import '../../../domain/repositories/company_repository.dart';
import '../../../domain/entities/user.dart';
import '../../../domain/repositories/auth_repository.dart';
import '../../bloc/auth/auth_bloc.dart';
class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  bool _loadingRefresh = false;

  Future<void> _refreshProfile() async {
    final auth = context.read<AuthBloc>().state;
    if (auth is! AuthAuthenticated) return;
    setState(() => _loadingRefresh = true);
    final repo = getIt<AuthRepository>();
    final result = await repo.fetchUserProfile(auth.user.username);
    if (!mounted) return;
    setState(() => _loadingRefresh = false);
    result.fold(
      (f) => ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(f.message)),
      ),
      (user) => context.read<AuthBloc>().add(SessionUserUpdatedEvent(user)),
    );
  }

  void _confirmLogout(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar Sesión'),
        content: const Text('¿Estás seguro de que quieres cerrar sesión?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              context.read<AuthBloc>().add(const LogoutEvent());
            },
            style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
  }

  static String _initials(User u) {
    final fn = u.firstName?.trim() ?? '';
    final ln = u.lastName?.trim() ?? '';
    if (fn.isNotEmpty && ln.isNotEmpty) {
      return '${fn[0]}${ln[0]}'.toUpperCase();
    }
    if (fn.isNotEmpty) {
      return fn.length >= 2 ? fn.substring(0, 2).toUpperCase() : fn[0].toUpperCase();
    }
    final s = u.username;
    if (s.length >= 2) return s.substring(0, 2).toUpperCase();
    if (s.isNotEmpty) return s[0].toUpperCase();
    return '?';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Perfil'),
        actions: [
          IconButton(
            onPressed: _loadingRefresh ? null : _refreshProfile,
            icon: _loadingRefresh
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh),
            tooltip: 'Actualizar',
          ),
        ],
      ),
      body: BlocBuilder<AuthBloc, AuthState>(
        builder: (context, state) {
          if (state is! AuthAuthenticated) {
            return const Center(child: Text('No autenticado'));
          }
          final u = state.user;
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _ProfileHeader(user: u, initials: _initials(u)),
              const SizedBox(height: 20),
              _CompanyCard(user: u),
              const SizedBox(height: 8),
              _PersonalInfoCard(user: u),
              const SizedBox(height: 8),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: Icon(Icons.edit_outlined, color: theme.colorScheme.primary),
                      title: const Text('Editar Información Personal'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/profile/edit'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.lock_outline, color: theme.colorScheme.primary),
                      title: const Text('Cambiar Contraseña'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/profile/change-password'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.apartment, color: theme.colorScheme.primary),
                      title: const Text('Cambiar Compañía'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/select-company'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.point_of_sale, color: theme.colorScheme.primary),
                      title: const Text('Punto de Emisión Activo'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/emission-point'),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: Icon(Icons.settings_outlined, color: theme.colorScheme.primary),
                      title: const Text('Ajustes'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.push('/settings'),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              Card(
                child: ListTile(
                  leading: Icon(Icons.logout, color: theme.colorScheme.error),
                  title: Text(
                    'Cerrar Sesión',
                    style: TextStyle(color: theme.colorScheme.error, fontWeight: FontWeight.w500),
                  ),
                  onTap: () => _confirmLogout(context),
                ),
              ),
              const SizedBox(height: 24),
              Center(
                child: Text(
                  'eBill Mobile v${AppConfig.appVersion}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({required this.user, required this.initials});

  final User user;
  final String initials;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            CircleAvatar(
              radius: 40,
              backgroundColor: theme.colorScheme.primaryContainer,
              child: Text(
                initials,
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w600,
                  color: theme.colorScheme.onPrimaryContainer,
                ),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    user.fullName,
                    style: theme.textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Icon(
                        Icons.alternate_email,
                        size: 16,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          user.username,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  if (user.email.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Row(
                      children: [
                        Icon(
                          Icons.email_outlined,
                          size: 16,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            user.email,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ],
                  const SizedBox(height: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: user.isActive
                          ? theme.colorScheme.primary.withValues(alpha: 0.1)
                          : theme.colorScheme.error.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      user.isActive ? 'Activo' : 'Inactivo',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: user.isActive ? theme.colorScheme.primary : theme.colorScheme.error,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PersonalInfoCard extends StatelessWidget {
  const _PersonalInfoCard({required this.user});

  final User user;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasDetails = user.firstName != null ||
        user.lastName != null ||
        user.email.isNotEmpty ||
        user.phone != null;

    if (!hasDetails) return const SizedBox.shrink();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Información Personal',
              style: theme.textTheme.titleSmall?.copyWith(
                color: theme.colorScheme.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            if (user.firstName != null && user.firstName!.isNotEmpty)
              _InfoRow(icon: Icons.person_outline, label: 'Nombre', value: user.firstName!),
            if (user.lastName != null && user.lastName!.isNotEmpty)
              _InfoRow(icon: Icons.person_outline, label: 'Apellido', value: user.lastName!),
            if (user.email.isNotEmpty)
              _InfoRow(icon: Icons.email_outlined, label: 'Email', value: user.email),
            if (user.phone != null && user.phone!.isNotEmpty)
              _InfoRow(icon: Icons.phone_outlined, label: 'Teléfono', value: user.phone!),
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.icon, required this.label, required this.value});

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Icon(icon, size: 20, color: theme.colorScheme.onSurfaceVariant),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                Text(value, style: theme.textTheme.bodyMedium),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CompanyCard extends StatelessWidget {
  const _CompanyCard({required this.user});

  final User user;

  Future<String> _loadCompanyName(int companyId) async {
    final result = await getIt<CompanyRepository>().getCompanyDisplayName(companyId);
    return result.getOrElse(() => 'Compañía $companyId');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final id = user.companyId;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.business, size: 20, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  'Compañía',
                  style: theme.textTheme.titleSmall?.copyWith(
                    color: theme.colorScheme.primary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (id == null)
              Text(
                'Sin compañía seleccionada',
                style: theme.textTheme.bodyLarge,
              )
            else
              FutureBuilder<String>(
                future: _loadCompanyName(id),
                builder: (context, snapshot) {
                  return Text(
                    snapshot.data ?? 'Cargando…',
                    style: theme.textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w500,
                    ),
                  );
                },
              ),
            if (user.companyIdentification != null &&
                user.companyIdentification!.isNotEmpty) ...[
              const SizedBox(height: 4),
              Row(
                children: [
                  Icon(
                    Icons.badge_outlined,
                    size: 16,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    'RUC: ${user.companyIdentification}',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ],
            if (user.companyEstablishments.length > 1)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: () => context.push('/select-company'),
                  icon: const Icon(Icons.swap_horiz, size: 18),
                  label: const Text('Cambiar'),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
