import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date;
import '../../../domain/entities/sales_summary.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/report/report_bloc.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/skeleton_loader.dart';

class ReportsPage extends StatefulWidget {
  const ReportsPage({super.key});

  @override
  State<ReportsPage> createState() => _ReportsPageState();
}

class _ReportsPageState extends State<ReportsPage> {
  static const _periods = ['DAY', 'WEEK', 'MONTH'];
  static const _periodLabels = ['Hoy', 'Semana', 'Mes'];
  int _periodIndex = 2;
  bool _companyMissing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadSummary());
  }

  Future<void> _loadSummary() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    final user = auth is AuthAuthenticated ? auth.user : null;
    final idCompany = await resolveCompanyId(storage, user);
    if (!mounted) return;
    if (idCompany == null) {
      setState(() => _companyMissing = true);
      return;
    }
    setState(() => _companyMissing = false);
    context.read<ReportBloc>().add(
          LoadSalesSummaryEvent(
            period: _periods[_periodIndex],
            idCompany: idCompany,
          ),
        );
  }

  void _onPeriodChanged(int index) {
    if (index == _periodIndex) return;
    setState(() => _periodIndex = index);
    _loadSummary();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Reportes')),
      body: BlocBuilder<ReportBloc, ReportState>(
        buildWhen: (_, current) =>
            current is ReportInitial ||
            current is ReportLoading ||
            current is ReportLoaded ||
            current is ReportError,
        builder: (context, state) {
          if (state is ReportLoading) {
            return const SkeletonList();
          }
          if (state is ReportError) {
            return app_error.ErrorWidget(
              message: state.message,
              subtitle: state.httpStatus != null
                  ? 'HTTP ${state.httpStatus}'
                  : null,
              onRetry: _loadSummary,
            );
          }
          if (state is ReportLoaded) {
            return RefreshIndicator(
              onRefresh: _loadSummary,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _PeriodSelector(
                    labels: _periodLabels,
                    selectedIndex: _periodIndex,
                    onChanged: _onPeriodChanged,
                  ),
                  const SizedBox(height: 16),
                  _KpiCards(summary: state.summary, theme: theme),
                  const SizedBox(height: 20),
                  _SectionTitle(title: 'Tendencia de ventas', theme: theme),
                  const SizedBox(height: 8),
                  _DailyBarsChart(
                    dailySales: state.summary.dailySales,
                    theme: theme,
                  ),
                  const SizedBox(height: 20),
                  _SectionTitle(title: 'Top productos', theme: theme),
                  const SizedBox(height: 8),
                  _TopProductsList(
                    products: state.summary.topProducts,
                    theme: theme,
                  ),
                  const SizedBox(height: 20),
                  _SectionTitle(title: 'Clientes destacados', theme: theme),
                  const SizedBox(height: 8),
                  _TopClientsList(
                    clients: state.summary.topClients,
                    theme: theme,
                  ),
                  const SizedBox(height: 24),
                  _SectionTitle(title: 'Reportes detallados', theme: theme),
                  const SizedBox(height: 12),
                  _ReportNavGrid(theme: theme),
                  const SizedBox(height: 24),
                ],
              ),
            );
          }
          if (_companyMissing) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      Icons.business_outlined,
                      size: 64,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'No se pudo determinar la compañía.',
                      style: theme.textTheme.titleMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: () => context.push('/select-company'),
                      icon: const Icon(Icons.business),
                      label: const Text('Seleccionar compañía'),
                    ),
                    const SizedBox(height: 8),
                    OutlinedButton.icon(
                      onPressed: () => _loadSummary(),
                      icon: const Icon(Icons.refresh),
                      label: const Text('Reintentar'),
                    ),
                  ],
                ),
              ),
            );
          }
          return const SkeletonList();
        },
      ),
    );
  }
}

class _PeriodSelector extends StatelessWidget {
  const _PeriodSelector({
    required this.labels,
    required this.selectedIndex,
    required this.onChanged,
  });

  final List<String> labels;
  final int selectedIndex;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return SegmentedButton<int>(
      segments: List.generate(
        labels.length,
        (i) => ButtonSegment(value: i, label: Text(labels[i])),
      ),
      selected: {selectedIndex},
      onSelectionChanged: (s) => onChanged(s.first),
      showSelectedIcon: false,
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title, required this.theme});
  final String title;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Text(title, style: theme.textTheme.titleMedium);
  }
}

class _KpiCards extends StatelessWidget {
  const _KpiCards({required this.summary, required this.theme});

  final SalesSummary summary;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    final hasDashboard = summary.todayTotal > 0 ||
        summary.weekTotal > 0 ||
        summary.monthTotal > 0;

    final cards = <Widget>[
      _KpiCard(
        label: hasDashboard ? 'Hoy' : 'Total ventas',
        value: app_date.DateUtils.formatCurrency(
          hasDashboard ? summary.todayTotal : summary.totalSales,
        ),
        icon: Icons.payments_outlined,
        color: theme.colorScheme.primary,
      ),
      if (hasDashboard)
        _KpiCard(
          label: 'Semana',
          value: app_date.DateUtils.formatCurrency(summary.weekTotal),
          icon: Icons.date_range_outlined,
          color: theme.colorScheme.secondary,
        ),
      _KpiCard(
        label: hasDashboard ? 'Mes' : 'Facturas',
        value: hasDashboard
            ? app_date.DateUtils.formatCurrency(summary.monthTotal)
            : '${summary.totalInvoices}',
        icon: hasDashboard
            ? Icons.calendar_month_outlined
            : Icons.receipt_long_outlined,
        color: theme.colorScheme.tertiary,
      ),
      _KpiCard(
        label: 'Promedio',
        value: app_date.DateUtils.formatCurrency(summary.averagePerInvoice),
        icon: Icons.analytics_outlined,
        color: theme.colorScheme.outline,
      ),
    ];

    return LayoutBuilder(
      builder: (context, constraints) {
        final crossAxisCount = constraints.maxWidth > 600
            ? 4
            : constraints.maxWidth > 400
                ? 3
                : 2;
        return GridView.count(
          crossAxisCount: crossAxisCount,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisSpacing: 10,
          mainAxisSpacing: 10,
          childAspectRatio: 1.6,
          children: cards,
        );
      },
    );
  }
}

class _KpiCard extends StatelessWidget {
  const _KpiCard({
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
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: color),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    label,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              value,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
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

class _DailyBarsChart extends StatelessWidget {
  const _DailyBarsChart({required this.dailySales, required this.theme});

  final List<DailySales> dailySales;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    if (dailySales.isEmpty) {
      return Container(
        height: 140,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(
          'Sin datos para el período',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.outline,
          ),
        ),
      );
    }

    final amounts = dailySales.map((e) => e.amount).toList();
    final maxAmount = amounts.fold(0.0, (a, b) => math.max(a, b));
    const maxBarHeight = 100.0;

    return SizedBox(
      height: 150,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: dailySales.map((d) {
          final h =
              maxAmount > 0 ? (d.amount / maxAmount) * maxBarHeight : 0.0;
          final label = app_date.DateUtils.formatDate(d.date, format: 'dd/MM');
          return Expanded(
            child: Tooltip(
              message: app_date.DateUtils.formatCurrency(d.amount),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 2),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Text(
                      d.count > 0 ? '${d.count}' : '',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.outline,
                        fontSize: 9,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Container(
                      height: h.clamp(4.0, maxBarHeight),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.primary.withValues(alpha: 0.85),
                        borderRadius: const BorderRadius.vertical(
                          top: Radius.circular(6),
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      label,
                      style: theme.textTheme.labelSmall,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _TopProductsList extends StatelessWidget {
  const _TopProductsList({required this.products, required this.theme});

  final List<TopProduct> products;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    if (products.isEmpty) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Text(
          'Sin datos de productos',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.outline,
          ),
        ),
      );
    }

    return Column(
      children: products
          .take(5)
          .map(
            (p) => Card(
              margin: const EdgeInsets.only(bottom: 6),
              child: ListTile(
                dense: true,
                leading: CircleAvatar(
                  radius: 18,
                  backgroundColor:
                      theme.colorScheme.primaryContainer,
                  child: Icon(
                    Icons.inventory_2_outlined,
                    size: 18,
                    color: theme.colorScheme.primary,
                  ),
                ),
                title: Text(
                  p.name.isEmpty ? p.code : p.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text('${p.quantitySold} vendidos'),
                trailing: Text(
                  app_date.DateUtils.formatCurrency(p.totalSales),
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _TopClientsList extends StatelessWidget {
  const _TopClientsList({required this.clients, required this.theme});

  final List<TopClient> clients;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    if (clients.isEmpty) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Text(
          'Sin datos de clientes',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.outline,
          ),
        ),
      );
    }

    return Column(
      children: clients
          .take(5)
          .map(
            (c) => Card(
              margin: const EdgeInsets.only(bottom: 6),
              child: ListTile(
                dense: true,
                leading: CircleAvatar(
                  radius: 18,
                  backgroundColor:
                      theme.colorScheme.secondaryContainer,
                  child: Icon(
                    Icons.person_outline,
                    size: 18,
                    color: theme.colorScheme.secondary,
                  ),
                ),
                title: Text(
                  c.name.isEmpty ? '—' : c.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text('${c.invoiceCount} facturas'),
                trailing: Text(
                  app_date.DateUtils.formatCurrency(c.totalAmount),
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          )
          .toList(),
    );
  }
}

class _ReportNavGrid extends StatelessWidget {
  const _ReportNavGrid({required this.theme});
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final crossAxisCount = constraints.maxWidth > 600
            ? 4
            : constraints.maxWidth > 400
                ? 3
                : 2;
        return GridView.count(
          crossAxisCount: crossAxisCount,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisSpacing: 10,
          mainAxisSpacing: 10,
          childAspectRatio: 1.4,
          children: [
        _NavCard(
          icon: Icons.calendar_today_outlined,
          title: 'Ventas por\nperíodo',
          color: theme.colorScheme.primary,
          onTap: () => context.push('/reports/by-period'),
        ),
        _NavCard(
          icon: Icons.people_outline,
          title: 'Ventas por\ncliente',
          color: theme.colorScheme.secondary,
          onTap: () => context.push('/reports/by-client'),
        ),
        _NavCard(
          icon: Icons.inventory_2_outlined,
          title: 'Ventas por\nproducto',
          color: theme.colorScheme.tertiary,
          onTap: () => context.push('/reports/by-product'),
        ),
        _NavCard(
          icon: Icons.pending_actions_outlined,
          title: 'Facturas\npendientes',
          color: theme.colorScheme.error,
          onTap: () => context.push('/reports/pending'),
        ),
          ],
        );
      },
    );
  }
}

class _NavCard extends StatelessWidget {
  const _NavCard({
    required this.icon,
    required this.title,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 32, color: color),
              const SizedBox(height: 8),
              Text(
                title,
                textAlign: TextAlign.center,
                style: theme.textTheme.labelLarge,
                maxLines: 2,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
