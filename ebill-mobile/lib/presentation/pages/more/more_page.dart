import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../bloc/auth/auth_bloc.dart';
class MorePage extends StatelessWidget {
  const MorePage({super.key});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Más'),
        actions: const [],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          _Section(
            title: 'Documentos',
            children: [
              _MenuItem(
                icon: Icons.note_alt_outlined,
                label: 'Notas de Crédito',
                onTap: () => context.push('/credit-notes'),
              ),
              _MenuItem(
                icon: Icons.description_outlined,
                label: 'Proformas',
                onTap: () => context.push('/proformas'),
              ),
              _MenuItem(
                icon: Icons.local_shipping_outlined,
                label: 'Guías de Remisión',
                onTap: () => context.push('/referral-guides'),
              ),
              _MenuItem(
                icon: Icons.edit_note,
                label: 'Borradores',
                onTap: () => context.push('/invoices/drafts'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _Section(
            title: 'Análisis',
            children: [
              _MenuItem(
                icon: Icons.bar_chart,
                label: 'Reportes',
                onTap: () => context.push('/reports'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _Section(
            title: 'Configuración',
            children: [
              _MenuItem(
                icon: Icons.point_of_sale,
                label: 'Punto de Emisión',
                onTap: () => context.push('/emission-point'),
              ),
              _MenuItem(
                icon: Icons.business,
                label: 'Cambiar Compañía',
                onTap: () => context.push('/select-company'),
              ),
              _MenuItem(
                icon: Icons.person_outline,
                label: 'Perfil',
                onTap: () => context.push('/profile'),
              ),
              _MenuItem(
                icon: Icons.settings_outlined,
                label: 'Ajustes',
                onTap: () => context.push('/settings'),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: OutlinedButton.icon(
              onPressed: () => _showLogoutDialog(context),
              icon: Icon(Icons.logout, color: cs.error),
              label: Text('Cerrar Sesión', style: TextStyle(color: cs.error)),
              style: OutlinedButton.styleFrom(
                side: BorderSide(color: cs.error.withValues(alpha: 0.3)),
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  void _showLogoutDialog(BuildContext context) {
    showDialog(
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
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
            ),
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
          child: Text(
            title.toUpperCase(),
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 1.2,
                ),
          ),
        ),
        Card(
          margin: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(children: children),
        ),
      ],
    );
  }
}

class _MenuItem extends StatelessWidget {
  const _MenuItem({
    required this.icon,
    required this.label,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right, size: 20),
      onTap: onTap,
    );
  }
}
