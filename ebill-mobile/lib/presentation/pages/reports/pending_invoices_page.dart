import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date;
import '../../../domain/entities/pending_invoice.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/report/report_bloc.dart';
import '../../widgets/date_range_filter.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';

class PendingInvoicesPage extends StatefulWidget {
  const PendingInvoicesPage({super.key});

  @override
  State<PendingInvoicesPage> createState() => _PendingInvoicesPageState();
}

class _PendingInvoicesPageState extends State<PendingInvoicesPage> {
  late DateTime _dateFrom;
  late DateTime _dateTo;
  int? _companyId;
  bool _companyMissing = false;
  String? _statusFilter;

  static const _statusOptions = <String, String>{
    '': 'Todos',
    'PENDIENTE_ENVIO': 'Pendiente envío',
    'RECHAZADA_SRI': 'Rechazada SRI',
    'CADUCADA': 'Caducada',
  };

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
          LoadPendingInvoicesEvent(
            idCompany: _companyId!,
            dateFrom: _dateFrom,
            dateTo: _dateTo,
            statusFilter: _statusFilter,
          ),
        );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Facturas pendientes')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
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
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 4, 12, 4),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: _StatusFilterChips(
                  options: _statusOptions,
                  selected: _statusFilter ?? '',
                  onChanged: (v) {
                    setState(() => _statusFilter = v.isEmpty ? null : v);
                    _search();
                  },
                ),
              ),
            ),
          ),
          Expanded(
            child: BlocBuilder<ReportBloc, ReportState>(
              buildWhen: (_, current) =>
                  current is PendingInvoicesLoading ||
                  current is PendingInvoicesLoaded ||
                  current is PendingInvoicesError,
              builder: (context, state) {
                if (state is PendingInvoicesLoading) {
                  return const LoadingWidget();
                }
                if (state is PendingInvoicesError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _search,
                  );
                }
                if (state is PendingInvoicesLoaded) {
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

class _StatusFilterChips extends StatelessWidget {
  const _StatusFilterChips({
    required this.options,
    required this.selected,
    required this.onChanged,
  });

  final Map<String, String> options;
  final String selected;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 38,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: options.entries.map((e) {
          final isSelected = e.key == selected;
          return Padding(
            padding: const EdgeInsets.only(right: 6),
            child: FilterChip(
              label: Text(e.value),
              selected: isSelected,
              onSelected: (_) => onChanged(e.key),
              showCheckmark: false,
              visualDensity: VisualDensity.compact,
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({required this.report, required this.theme});

  final PendingInvoicesReport report;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _PendingBanner(total: report.totalPending, theme: theme),
        Expanded(
          child: Card(
            margin: const EdgeInsets.fromLTRB(12, 4, 12, 16),
            clipBehavior: Clip.antiAlias,
            child: report.items.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.check_circle_outline,
                            size: 56,
                            color: theme.colorScheme.primary,
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'No hay facturas pendientes',
                            style: theme.textTheme.bodyLarge?.copyWith(
                              color: theme.colorScheme.outline,
                            ),
                          ),
                        ],
                      ),
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: report.items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 4),
                    itemBuilder: (context, index) {
                      final item = report.items[index];
                      return _PendingCard(item: item, theme: theme);
                    },
                  ),
          ),
        ),
      ],
    );
  }
}

class _PendingBanner extends StatelessWidget {
  const _PendingBanner({required this.total, required this.theme});

  final int total;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.fromLTRB(12, 4, 12, 4),
      color: total > 0
          ? theme.colorScheme.tertiary.withValues(alpha: 0.12)
          : theme.colorScheme.primary.withValues(alpha: 0.12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              total > 0
                  ? Icons.warning_amber_rounded
                  : Icons.check_circle_outline,
              color: total > 0 ? theme.colorScheme.tertiary : theme.colorScheme.primary,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                total > 0
                    ? '$total factura${total == 1 ? '' : 's'} pendiente${total == 1 ? '' : 's'}'
                    : 'Sin facturas pendientes',
                style: theme.textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PendingCard extends StatelessWidget {
  const _PendingCard({required this.item, required this.theme});

  final PendingInvoiceItem item;
  final ThemeData theme;

  Color _statusColor(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final status = item.authorizationStatusSri.toUpperCase();
    if (status.contains('RECHAZ')) return cs.error;
    if (status.contains('CADUC')) return cs.outline;
    return cs.tertiary;
  }

  String _statusLabel() {
    final auth = item.authorizationStatusSri;
    final rec = item.receptionStatusSri;
    if (auth.isNotEmpty) return auth;
    if (rec.isNotEmpty) return rec;
    return 'Pendiente';
  }

  @override
  Widget build(BuildContext context) {
    final color = _statusColor(context);
    final hasError =
        item.receptionError.isNotEmpty || item.authorizationError.isNotEmpty;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 16,
                  backgroundColor: color.withValues(alpha: 0.15),
                  child: Icon(
                      Icons.receipt_long_outlined,
                      size: 16,
                      color: color,
                    ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.invoiceNumber.isNotEmpty
                            ? item.invoiceNumber
                            : '—',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        item.clientName,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: color.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        _statusLabel(),
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: color,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      app_date.DateUtils.formatDate(item.issueDate),
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            if (hasError) ...[
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: theme.colorScheme.error.withValues(alpha: 0.06),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  item.authorizationError.isNotEmpty
                      ? item.authorizationError
                      : item.receptionError,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.error,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
            if (item.retryCount > 0) ...[
              const SizedBox(height: 4),
              Text(
                'Reintentos: ${item.retryCount}',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
