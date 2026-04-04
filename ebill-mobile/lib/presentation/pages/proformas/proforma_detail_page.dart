import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/proforma/proforma_bloc.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';
import '../../widgets/status_chip.dart';

class ProformaDetailPage extends StatefulWidget {
  const ProformaDetailPage({
    super.key,
    required this.proformaId,
  });

  final int proformaId;

  @override
  State<ProformaDetailPage> createState() => _ProformaDetailPageState();
}

class _ProformaDetailPageState extends State<ProformaDetailPage> {
  bool _companyMissing = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    final user = auth is AuthAuthenticated ? auth.user : null;
    final idCompany = await resolveCompanyId(storage, user);
    if (!mounted) return;
    if (idCompany == null) {
      setState(() => _companyMissing = true);
      return;
    }
    context.read<ProformaBloc>().add(
          LoadProformaByIdEvent(widget.proformaId, idCompany),
        );
  }

  bool _canConvert(String? status) {
    return status == 'DRAFT' || status == 'SENT';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle Proforma'),
      ),
      body: BlocConsumer<ProformaBloc, ProformaState>(
        buildWhen: (_, current) =>
            current is ProformaDetailLoading ||
            current is ProformaDetailLoaded ||
            current is ProformaDetailError ||
            current is ProformaFormError ||
            current is ProformaLoading,
        listenWhen: (_, current) => current is ProformaConvertedToInvoice,
        listener: (context, state) {
          if (state is ProformaConvertedToInvoice) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text('Factura borrador creada desde la proforma'),
                backgroundColor: Theme.of(context).colorScheme.primary,
              ),
            );
            context.push('/invoices/${state.invoiceId}');
          }
        },
        builder: (context, state) {
          if (state is ProformaDetailLoading) {
            return const LoadingWidget();
          }
          if (state is ProformaDetailError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: _load,
            );
          }
          if (state is ProformaFormError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: _load,
            );
          }
          if (state is ProformaLoading) {
            return const LoadingWidget();
          }
          if (state is ProformaDetailLoaded && state.proforma.id == widget.proformaId) {
            final p = state.proforma;
            return SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          'N.º ${p.proformaNumber}',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ),
                      const SizedBox(width: 12),
                      StatusChip(status: p.status),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            'Fechas',
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .primary,
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                          const SizedBox(height: 12),
                          _Row(
                            'Emisión',
                            app_date_utils.DateUtils.formatDate(p.issueDate),
                          ),
                          if (p.expiryDate != null)
                            _Row(
                              'Válida hasta',
                              app_date_utils.DateUtils.formatDate(
                                p.expiryDate!,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                  if (p.clientName != null && p.clientName!.isNotEmpty) ...[
                    const SizedBox(height: 16),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Text(
                              'Cliente',
                              style: Theme.of(context)
                                  .textTheme
                                  .titleSmall
                                  ?.copyWith(
                                    color: Theme.of(context)
                                        .colorScheme
                                        .primary,
                                    fontWeight: FontWeight.w600,
                                  ),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              p.clientName!,
                              style: Theme.of(context).textTheme.bodyLarge,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            'Importes',
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .primary,
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                          const SizedBox(height: 12),
                          _Row(
                            'Subtotal IVA',
                            app_date_utils.DateUtils.formatCurrency(
                              p.subtotalIva,
                            ),
                          ),
                          _Row(
                            'Subtotal 0%',
                            app_date_utils.DateUtils.formatCurrency(
                              p.subtotalZero,
                            ),
                          ),
                          const Divider(height: 24),
                          _Row(
                            'Total',
                            app_date_utils.DateUtils.formatCurrency(
                              p.totalAmount,
                            ),
                            bold: true,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  if (_canConvert(p.status))
                    FilledButton.icon(
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 14,
                        ),
                        minimumSize: const Size.fromHeight(48),
                      ),
                      onPressed: () async {
                        final storage = getIt<StorageService>();
                        final auth = context.read<AuthBloc>().state;
                        final user =
                            auth is AuthAuthenticated ? auth.user : null;
                        final idCompany =
                            await resolveCompanyId(storage, user);
                        if (!mounted || idCompany == null) return;
                        if (!context.mounted) return;
                        context.read<ProformaBloc>().add(
                              ConvertToInvoiceEvent(
                                proformaId: widget.proformaId,
                                idCompany: idCompany,
                              ),
                            );
                      },
                      icon: const Icon(Icons.receipt_long),
                      label: const Text('Convertir a factura'),
                    ),
                ],
              ),
            );
          }
          if (_companyMissing) {
            return const Center(
              child: Text('No se pudo determinar la compañía.'),
            );
          }
          return const LoadingWidget();
        },
      ),
    );
  }
}

class _Row extends StatelessWidget {
  const _Row(this.label, this.value, {this.bold = false});

  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontWeight: bold ? FontWeight.bold : FontWeight.normal,
            ),
          ),
          const SizedBox(width: 16),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontWeight: bold ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
