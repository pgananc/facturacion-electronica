import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date;
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/report/report_bloc.dart';
class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadSummary(forceRefresh: false));
  }

  /// [forceRefresh] true al deslizar: el bloc debe volver a pedir datos aunque ya hubiera [ReportLoaded].
  Future<void> _loadSummary({required bool forceRefresh}) async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    final user = auth is AuthAuthenticated ? auth.user : null;
    final idCompany = await resolveCompanyId(storage, user);
    if (!mounted || idCompany == null) return;

    final bloc = context.read<ReportBloc>();
    if (!forceRefresh && bloc.state is ReportLoaded) {
      return;
    }

    final done = bloc.stream.firstWhere(
      (s) => s is ReportLoaded || s is ReportError,
    );
    bloc.add(
      LoadSalesSummaryEvent(
        period: 'DAY',
        idCompany: idCompany,
        forceRefresh: forceRefresh,
      ),
    );
    await done.timeout(const Duration(seconds: 60));
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return BlocSelector<AuthBloc, AuthState, String>(
      selector: (state) => state is AuthAuthenticated
          ? (state.user.firstName ?? state.user.username)
          : 'Usuario',
      builder: (context, username) {
        return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => _loadSummary(forceRefresh: true),
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(child: _WelcomeHeader(username: username, cs: cs, tt: tt)),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
              sliver: SliverToBoxAdapter(
                child: BlocBuilder<ReportBloc, ReportState>(
                  buildWhen: (_, current) =>
                      current is ReportInitial ||
                      current is ReportLoading ||
                      current is ReportLoaded ||
                      current is ReportError,
                  builder: (context, state) {
                    if (state is ReportLoading || state is ReportInitial) {
                      return _SummaryCards(
                        todayTotal: 0,
                        invoiceCount: 0,
                        pendingCount: 0,
                        cs: cs,
                        tt: tt,
                        isLoading: true,
                      );
                    }
                    if (state is ReportError) {
                      return _SummaryCards(
                        todayTotal: 0,
                        invoiceCount: 0,
                        pendingCount: 0,
                        cs: cs,
                        tt: tt,
                        errorMessage: state.message,
                        errorHttpStatus: state.httpStatus,
                        onRetry: () => _loadSummary(forceRefresh: true),
                      );
                    }
                    if (state is ReportLoaded) {
                      return _SummaryCards(
                        todayTotal: state.summary.todayTotal > 0
                            ? state.summary.todayTotal
                            : state.summary.totalSales,
                        invoiceCount: state.summary.totalInvoices,
                        pendingCount: 0,
                        cs: cs,
                        tt: tt,
                      );
                    }
                    return _SummaryCards(
                      todayTotal: 0,
                      invoiceCount: 0,
                      pendingCount: 0,
                      cs: cs,
                      tt: tt,
                    );
                  },
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              sliver: SliverToBoxAdapter(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _NewInvoiceCard(cs: cs, tt: tt),
                    const SizedBox(height: 12),
                    _QuickAccessGrid(cs: cs, tt: tt),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              sliver: SliverToBoxAdapter(
                child: BlocBuilder<ReportBloc, ReportState>(
                  buildWhen: (_, current) =>
                      current is ReportLoaded ||
                      current is ReportLoading ||
                      current is ReportError ||
                      current is ReportInitial,
                  builder: (context, state) {
                    if (state is ReportLoaded &&
                        state.summary.topProducts.isNotEmpty) {
                      return _RecentActivity(
                        products: state.summary.topProducts
                            .take(3)
                            .map((p) => _RecentItem(
                                  title: p.name.isEmpty ? p.code : p.name,
                                  subtitle: '${p.quantitySold} vendidos',
                                  amount: p.totalSales,
                                ),)
                            .toList(),
                        cs: cs,
                        tt: tt,
                      );
                    }
                    if (state is ReportLoading) {
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 24),
                        child: Center(
                          child: SizedBox(
                            width: 24,
                            height: 24,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: cs.primary,
                            ),
                          ),
                        ),
                      );
                    }
                    return const SizedBox.shrink();
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
      },
    );
  }
}

class _WelcomeHeader extends StatelessWidget {
  const _WelcomeHeader({
    required this.username,
    required this.cs,
    required this.tt,
  });

  final String username;
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    final today = DateFormat('EEEE, d MMMM', 'es').format(DateTime.now());
    final colorScheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final gradient = LinearGradient(
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
      colors: isDark
          ? [colorScheme.surface, colorScheme.surfaceContainerHighest]
          : const [Color(0xFF001255), Color(0xFF0D2B7A)],
    );
    return Container(
      padding: EdgeInsets.fromLTRB(
        20,
        MediaQuery.of(context).padding.top + 24,
        20,
        32,
      ),
      decoration: BoxDecoration(
        gradient: gradient,
        borderRadius: const BorderRadius.vertical(bottom: Radius.circular(24)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              ColorFiltered(
                colorFilter: const ColorFilter.mode(Colors.white, BlendMode.srcIn),
                child: Image.asset(
                  'assets/images/logo-wallparisoft.png',
                  width: 52,
                  height: 52,
                ),
              ),
              const Spacer(),
              Semantics(
                button: true,
                label: 'Perfil',
                child: Tooltip(
                  message: 'Perfil',
                  child: SizedBox(
                    width: 48,
                    height: 48,
                    child: Center(
                      child: GestureDetector(
                        onTap: () => context.push('/profile'),
                        child: CircleAvatar(
                          radius: 20,
                          backgroundColor: Colors.white.withValues(alpha: 0.15),
                          child: const Icon(
                            Icons.person_outline,
                            color: Colors.white,
                            size: 20,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Semantics(
                button: true,
                label: 'Ajustes',
                child: Tooltip(
                  message: 'Ajustes',
                  child: SizedBox(
                    width: 48,
                    height: 48,
                    child: Center(
                      child: GestureDetector(
                        onTap: () => context.push('/settings'),
                        child: CircleAvatar(
                          radius: 20,
                          backgroundColor: Colors.white.withValues(alpha: 0.15),
                          child: const Icon(
                            Icons.settings_outlined,
                            color: Colors.white,
                            size: 20,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Text(
            'Bienvenido,',
            style: tt.bodyMedium?.copyWith(
              color: Colors.white70,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            username,
            style: tt.headlineSmall?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.w700,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
          Text(
            today,
            style: tt.bodySmall?.copyWith(color: Colors.white60),
          ),
        ],
      ),
    );
  }
}

class _SummaryCards extends StatelessWidget {
  const _SummaryCards({
    required this.todayTotal,
    required this.invoiceCount,
    required this.pendingCount,
    required this.cs,
    required this.tt,
    this.isLoading = false,
    this.errorMessage,
    this.errorHttpStatus,
    this.onRetry,
  });

  final double todayTotal;
  final int invoiceCount;
  final int pendingCount;
  final ColorScheme cs;
  final TextTheme tt;
  final bool isLoading;
  final String? errorMessage;
  final int? errorHttpStatus;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    if (errorMessage != null) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.warning_amber_rounded, color: cs.error),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'No se pudieron cargar las métricas. Deslice hacia abajo o pulse reintentar.',
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        if (errorHttpStatus != null) ...[
                          const SizedBox(height: 6),
                          Text(
                            'HTTP $errorHttpStatus',
                            style: Theme.of(context).textTheme.labelSmall?.copyWith(
                                  color: cs.onSurfaceVariant,
                                  fontFamily: 'monospace',
                                ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
              if (onRetry != null) ...[
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerRight,
                  child: FilledButton.tonal(
                    onPressed: onRetry,
                    child: const Text('Reintentar'),
                  ),
                ),
              ],
            ],
          ),
        ),
      );
    }
    // Tres columnas del mismo ancho → alineado visualmente con la rejilla 3×N de Acceso rápido.
    return Row(
      children: [
        Expanded(
          child: _KpiTile(
            label: 'Ventas del Día',
            value: isLoading ? '...' : app_date.DateUtils.formatCurrency(todayTotal),
            icon: Icons.trending_up,
            color: cs.tertiary,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _KpiTile(
            label: 'Facturas',
            value: isLoading ? '...' : '$invoiceCount',
            icon: Icons.receipt_long,
            color: cs.primary,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _KpiTile(
            label: 'Pendientes',
            value: isLoading ? '...' : '$pendingCount',
            icon: Icons.pending_actions,
            color: cs.secondary,
          ),
        ),
      ],
    );
  }
}

class _KpiTile extends StatelessWidget {
  const _KpiTile({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 20, color: color),
            const SizedBox(height: 8),
            Text(
              value,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

class _NewInvoiceCard extends StatelessWidget {
  const _NewInvoiceCard({required this.cs, required this.tt});
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: cs.primary,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => context.push('/invoices/new'),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.add, color: Colors.white, size: 24),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Nueva Factura',
                      style: tt.titleMedium?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Crear factura electrónica',
                      style: tt.bodySmall?.copyWith(
                        color: Colors.white70,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.arrow_forward_ios, color: Colors.white70, size: 18),
            ],
          ),
        ),
      ),
    );
  }
}

class _QuickAccessGrid extends StatelessWidget {
  const _QuickAccessGrid({required this.cs, required this.tt});
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Acceso Rápido',
          style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 4),
        LayoutBuilder(
          builder: (context, constraints) {
            final w = constraints.maxWidth;
            final crossAxisCount = w < 360
                ? 2
                : w < 600
                    ? 3
                    : 4;
            return GridView.count(
              crossAxisCount: crossAxisCount,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: EdgeInsets.zero,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 1.02,
              children: [
            _QuickTile(
              icon: Icons.people_outline,
              label: 'Clientes',
              color: cs.primary,
              onTap: () => context.go('/customers'),
            ),
            _QuickTile(
              icon: Icons.inventory_2_outlined,
              label: 'Productos',
              color: cs.tertiary,
              onTap: () => context.go('/products'),
            ),
            _QuickTile(
              icon: Icons.bar_chart,
              label: 'Reportes',
              color: cs.secondary,
              onTap: () => context.push('/reports'),
            ),
            _QuickTile(
              icon: Icons.note_alt_outlined,
              label: 'Notas C.',
              color: cs.error,
              onTap: () => context.push('/credit-notes'),
            ),
            _QuickTile(
              icon: Icons.description_outlined,
              label: 'Proformas',
              color: cs.secondary,
              onTap: () => context.push('/proformas'),
            ),
            _QuickTile(
              icon: Icons.local_shipping_outlined,
              label: 'Guías',
              color: cs.tertiary,
              onTap: () => context.push('/referral-guides'),
            ),
            _QuickTile(
              icon: Icons.edit_note,
              label: 'Borradores',
              color: cs.outline,
              onTap: () => context.push('/invoices/drafts'),
            ),
            _QuickTile(
              icon: Icons.settings_outlined,
              label: 'Ajustes',
              color: cs.onSurfaceVariant,
              onTap: () => context.push('/settings'),
            ),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _QuickTile extends StatelessWidget {
  const _QuickTile({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, size: 22, color: color),
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      fontWeight: FontWeight.w500,
                    ),
                textAlign: TextAlign.center,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RecentItem {
  const _RecentItem({
    required this.title,
    required this.subtitle,
    required this.amount,
  });
  final String title;
  final String subtitle;
  final double amount;
}

class _RecentActivity extends StatelessWidget {
  const _RecentActivity({
    required this.products,
    required this.cs,
    required this.tt,
  });

  final List<_RecentItem> products;
  final ColorScheme cs;
  final TextTheme tt;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Últimos Movimientos',
          style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 12),
        ...products.map(
          (item) => Card(
            margin: const EdgeInsets.only(bottom: 8),
            child: ListTile(
              leading: CircleAvatar(
                radius: 20,
                backgroundColor: cs.primaryContainer,
                child: Icon(
                  Icons.receipt_long,
                  size: 20,
                  color: cs.primary,
                ),
              ),
              title: Text(
                item.title,
                style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              subtitle: Text(item.subtitle),
              trailing: Text(
                app_date.DateUtils.formatCurrency(item.amount),
                style: tt.titleSmall?.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
