import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date;
import '../../../domain/entities/sales_by_period.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/report/report_bloc.dart';
import '../../widgets/date_range_filter.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';

class SalesByPeriodPage extends StatefulWidget {
  const SalesByPeriodPage({super.key});

  @override
  State<SalesByPeriodPage> createState() => _SalesByPeriodPageState();
}

class _SalesByPeriodPageState extends State<SalesByPeriodPage> {
  late DateTime _dateFrom;
  late DateTime _dateTo;
  int? _companyId;
  bool _companyMissing = false;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _dateFrom = DateTime(now.year, now.month, 1);
    _dateTo = now;
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    final user = auth is AuthAuthenticated ? auth.user : null;
    _companyId = await resolveCompanyId(storage, user);
    if (!mounted) return;
    if (_companyId == null) {
      setState(() => _companyMissing = true);
      return;
    }
    _search();
  }

  void _search() {
    if (_companyId == null) return;
    context.read<ReportBloc>().add(
          LoadSalesByPeriodEvent(
            idCompany: _companyId!,
            dateFrom: _dateFrom,
            dateTo: _dateTo,
          ),
        );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Ventas por período')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
            child: DateRangeFilter(
              dateFrom: _dateFrom,
              dateTo: _dateTo,
              onChanged: (from, to) {
                setState(() {
                  _dateFrom = from;
                  _dateTo = to;
                });
              },
              onSearch: _search,
            ),
          ),
          Expanded(
            child: BlocBuilder<ReportBloc, ReportState>(
              buildWhen: (_, current) =>
                  current is SalesByPeriodLoading ||
                  current is SalesByPeriodLoaded ||
                  current is SalesByPeriodError,
              builder: (context, state) {
                if (state is SalesByPeriodLoading) {
                  return const LoadingWidget();
                }
                if (state is SalesByPeriodError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _search,
                  );
                }
                if (state is SalesByPeriodLoaded) {
                  return _Content(report: state.report, theme: theme);
                }
                if (_companyMissing) {
                  return const Center(
                    child: Text('No se pudo determinar la compañía.'),
                  );
                }
                return const LoadingWidget();
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({required this.report, required this.theme});

  final SalesByPeriodReport report;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _MetricsBanner(metrics: report.metrics, theme: theme),
        Expanded(
          child: Card(
            margin: const EdgeInsets.fromLTRB(12, 4, 12, 16),
            clipBehavior: Clip.antiAlias,
            child: report.items.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: Text(
                        'No hay facturas en el período seleccionado',
                        style: theme.textTheme.bodyLarge?.copyWith(
                          color: theme.colorScheme.outline,
                        ),
                      ),
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: report.items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 4),
                    itemBuilder: (context, index) {
                      final item = report.items[index];
                      return _InvoiceCard(item: item, theme: theme);
                    },
                  ),
          ),
        ),
      ],
    );
  }
}

class _MetricsBanner extends StatelessWidget {
  const _MetricsBanner({required this.metrics, required this.theme});

  final SalesByPeriodMetrics metrics;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.fromLTRB(12, 4, 12, 4),
      color: theme.colorScheme.primaryContainer.withValues(alpha: 0.4),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            _MetricItem(
              label: 'Total',
              value: app_date.DateUtils.formatCurrency(metrics.totalInvoiced),
              theme: theme,
            ),
            _divider(theme),
            _MetricItem(
              label: 'Comprobantes',
              value: '${metrics.totalComprobantes}',
              theme: theme,
            ),
            _divider(theme),
            _MetricItem(
              label: 'Promedio',
              value: app_date.DateUtils.formatCurrency(metrics.averageTicket),
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  Widget _divider(ThemeData theme) {
    return Container(
      width: 1,
      height: 32,
      margin: const EdgeInsets.symmetric(horizontal: 8),
      color: theme.colorScheme.outline.withValues(alpha: 0.2),
    );
  }
}

class _MetricItem extends StatelessWidget {
  const _MetricItem({
    required this.label,
    required this.value,
    required this.theme,
  });

  final String label;
  final String value;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(
            value,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.bold,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
            maxLines: 1,
          ),
        ],
      ),
    );
  }
}

class _InvoiceCard extends StatelessWidget {
  const _InvoiceCard({required this.item, required this.theme});

  final SalesByPeriodItem item;
  final ThemeData theme;

  Color _statusColor(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    switch (item.authorizationStatusSri.toUpperCase()) {
      case 'AUTORIZADO':
        return cs.primary;
      case 'RECHAZADO':
        return cs.error;
      default:
        return cs.tertiary;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: ListTile(
        dense: true,
        leading: CircleAvatar(
          radius: 16,
          backgroundColor: _statusColor(context).withValues(alpha: 0.15),
          child: Icon(Icons.receipt_outlined, size: 16, color: _statusColor(context)),
        ),
        title: Text(
          item.invoiceNumber.isNotEmpty ? item.invoiceNumber : '—',
          style: theme.textTheme.bodyMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        subtitle: Text(
          '${item.clientName} · ${app_date.DateUtils.formatDate(item.issueDate)}',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Text(
          app_date.DateUtils.formatCurrency(item.total),
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}
