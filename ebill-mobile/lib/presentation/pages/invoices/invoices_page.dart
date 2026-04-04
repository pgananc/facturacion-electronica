import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../domain/entities/invoice.dart';
import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/invoice/invoice_bloc.dart';
import '../../widgets/skeleton_loader.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/status_chip.dart';

String _clientLine(Invoice inv) {
  final name = inv.clientName?.trim();
  if (name != null && name.isNotEmpty) {
    return 'Cliente: $name';
  }
  return 'Cliente: —';
}

class InvoicesPage extends StatefulWidget {
  const InvoicesPage({super.key});

  @override
  State<InvoicesPage> createState() => _InvoicesPageState();
}

class _InvoicesPageState extends State<InvoicesPage> {
  bool _companyMissing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadInvoices());
  }

  Future<void> _loadInvoices({bool forceRefresh = false}) async {
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
    context.read<InvoiceBloc>().add(
          LoadInvoicesEvent(idCompany: idCompany, forceRefresh: forceRefresh),
        );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Facturas'),
        actions: const [],
      ),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nueva factura',
        onPressed: () => context.push('/invoices/new'),
        child: const Icon(Icons.add),
      ),
      body: BlocBuilder<InvoiceBloc, InvoiceState>(
        buildWhen: (_, current) =>
            current is InvoiceLoading ||
            current is InvoiceLoaded ||
            current is InvoiceListError,
        builder: (context, state) {
          if (state is InvoiceLoading) return const SkeletonList();
          if (state is InvoiceListError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: _loadInvoices,
            );
          }
          if (state is InvoiceLoaded) {
            if (state.invoices.isEmpty) {
              return _EmptyState(
                icon: Icons.receipt_long,
                message: 'No hay facturas',
                actionLabel: 'Crear factura',
                onAction: () => context.push('/invoices/new'),
              );
            }
            return RefreshIndicator(
              onRefresh: () async => _loadInvoices(forceRefresh: true),
              child: ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 80),
                itemCount: state.invoices.length,
                separatorBuilder: (_, __) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final inv = state.invoices[index];
                  return Card(
                    child: InkWell(
                      borderRadius: BorderRadius.circular(12),
                      onTap: () => context.push(
                        '/invoices/${inv.id}',
                        extra: inv.status,
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            Container(
                              width: 44,
                              height: 44,
                              decoration: BoxDecoration(
                                color: cs.primaryContainer,
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Icon(
                                Icons.receipt_long,
                                color: cs.primary,
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    inv.invoiceNumber.isNotEmpty
                                        ? 'Factura #${inv.invoiceNumber}'
                                        : 'Factura (sin número)',
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodyMedium
                                        ?.copyWith(fontWeight: FontWeight.w600),
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    'Fecha de emisión: ${app_date_utils.DateUtils.formatDate(inv.issueDate)}',
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodySmall
                                        ?.copyWith(color: cs.onSurfaceVariant),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    _clientLine(inv),
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodySmall
                                        ?.copyWith(color: cs.onSurface),
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  app_date_utils.DateUtils.formatCurrency(
                                    inv.totalAmount,
                                  ),
                                  style: Theme.of(context)
                                      .textTheme
                                      .titleSmall
                                      ?.copyWith(fontWeight: FontWeight.w700),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                const SizedBox(height: 6),
                                StatusChip(status: inv.status),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
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
                    Text(
                      'No se pudo determinar la compañía.',
                      style: Theme.of(context).textTheme.titleMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: () => context.push('/select-company'),
                      child: const Text('Seleccionar compañía'),
                    ),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: () => _loadInvoices(),
                      child: const Text('Reintentar'),
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

class _EmptyState extends StatelessWidget {
  const _EmptyState({
    required this.icon,
    required this.message,
    required this.actionLabel,
    required this.onAction,
  });

  final IconData icon;
  final String message;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 64, color: Theme.of(context).colorScheme.outlineVariant),
          const SizedBox(height: 16),
          Text(message, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: onAction,
            icon: const Icon(Icons.add),
            label: Text(actionLabel),
          ),
        ],
      ),
    );
  }
}
