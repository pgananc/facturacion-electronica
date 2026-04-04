import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date;
import '../../../domain/entities/sales_by_product.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/report/report_bloc.dart';
import '../../widgets/date_range_filter.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';

class SalesByProductPage extends StatefulWidget {
  const SalesByProductPage({super.key});

  @override
  State<SalesByProductPage> createState() => _SalesByProductPageState();
}

class _SalesByProductPageState extends State<SalesByProductPage> {
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
          LoadSalesByProductEvent(
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
      appBar: AppBar(title: const Text('Ventas por producto')),
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
                  current is SalesByProductLoading ||
                  current is SalesByProductLoaded ||
                  current is SalesByProductError,
              builder: (context, state) {
                if (state is SalesByProductLoading) {
                  return const LoadingWidget();
                }
                if (state is SalesByProductError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _search,
                  );
                }
                if (state is SalesByProductLoaded) {
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

  final SalesByProductReport report;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    if (report.items.isEmpty) {
      return SizedBox.expand(
        child: Card(
          margin: const EdgeInsets.fromLTRB(12, 4, 12, 16),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Center(
              child: Text(
                'No hay datos de productos en el período',
                style: theme.textTheme.bodyLarge?.copyWith(
                  color: theme.colorScheme.outline,
                ),
              ),
            ),
          ),
        ),
      );
    }

    final maxInvoiced = report.items.fold(
      0.0,
      (prev, item) => item.totalInvoiced > prev ? item.totalInvoiced : prev,
    );

    return Card(
      margin: const EdgeInsets.fromLTRB(12, 4, 12, 16),
      clipBehavior: Clip.antiAlias,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: report.items.length,
        separatorBuilder: (_, __) => const SizedBox(height: 4),
        itemBuilder: (context, index) {
          final item = report.items[index];
          return _ProductCard(
            item: item,
            theme: theme,
            maxInvoiced: maxInvoiced,
            rank: index + 1,
          );
        },
      ),
    );
  }
}

class _ProductCard extends StatelessWidget {
  const _ProductCard({
    required this.item,
    required this.theme,
    required this.maxInvoiced,
    required this.rank,
  });

  final SalesByProductItem item;
  final ThemeData theme;
  final double maxInvoiced;
  final int rank;

  @override
  Widget build(BuildContext context) {
    final ratio = maxInvoiced > 0 ? item.totalInvoiced / maxInvoiced : 0.0;

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
                  backgroundColor: theme.colorScheme.tertiaryContainer,
                  child: Text(
                    '$rank',
                    style: theme.textTheme.labelMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.tertiary,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.productName.isNotEmpty ? item.productName : '—',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (item.productCode.isNotEmpty)
                        Text(
                          'Cód: ${item.productCode}',
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      app_date.DateUtils.formatCurrency(item.totalInvoiced),
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '${item.quantitySold} uds.',
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 8),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: ratio,
                minHeight: 4,
                backgroundColor:
                    theme.colorScheme.surfaceContainerHighest,
                color: theme.colorScheme.tertiary,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'P. unitario: ${app_date.DateUtils.formatCurrency(item.unitPrice)}',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
