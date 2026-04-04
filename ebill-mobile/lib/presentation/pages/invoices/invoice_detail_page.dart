import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/repositories/invoice_repository.dart';
import '../../bloc/invoice/invoice_bloc.dart';
import '../../mixins/document_preview_mixin.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';
import '../../widgets/status_chip.dart';

class InvoiceDetailPage extends StatefulWidget {
  const InvoiceDetailPage({
    super.key,
    required this.invoiceId,
    this.knownStatus,
  });

  final int invoiceId;
  final String? knownStatus;

  @override
  State<InvoiceDetailPage> createState() => _InvoiceDetailPageState();
}

class _InvoiceDetailPageState extends State<InvoiceDetailPage>
    with DocumentPreviewMixin {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<InvoiceBloc>().add(LoadInvoiceByIdEvent(widget.invoiceId));
      }
    });
  }

  @override
  void dispose() {
    disposePdfResources();
    super.dispose();
  }

  Widget _sectionTitle(BuildContext context, String title) {
    return Text(
      title,
      style: Theme.of(context).textTheme.titleSmall?.copyWith(
            color: Theme.of(context).colorScheme.primary,
            fontWeight: FontWeight.w600,
          ),
    );
  }

  Widget _buildPdfAction(InvoiceRepository repo) {
    return Padding(
      padding: const EdgeInsets.only(right: 4),
      child: FilledButton.icon(
        onPressed: docLoadingPdf
            ? null
            : () => loadAndPreviewPdf(
                  () => repo.downloadPdf(widget.invoiceId),
                  title: 'Factura ${widget.invoiceId}',
                  pdfFileName: 'factura_${widget.invoiceId}.pdf',
                  downloadXml: () => repo.downloadXml(widget.invoiceId),
                  xmlFileName: 'factura_${widget.invoiceId}.xml',
                ),
        icon: docLoadingPdf
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : const Icon(Icons.picture_as_pdf_outlined, size: 20),
        label: const Text('PDF'),
      ),
    );
  }

  Widget _buildShareAction(InvoiceRepository repo) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: MenuAnchor(
        builder: (context, controller, child) {
          return OutlinedButton.icon(
            onPressed: () {
              if (controller.isOpen) {
                controller.close();
              } else {
                controller.open();
              }
            },
            icon: const Icon(Icons.share_outlined, size: 20),
            label: const Text('Compartir'),
          );
        },
        menuChildren: [
          MenuItemButton(
            onPressed: () async {
              final r = await repo.downloadPdf(widget.invoiceId);
              if (!mounted) return;
              r.fold(
                (f) => ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(f.message)),
                ),
                (bytes) {
                  if (bytes.isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('El archivo PDF está vacío.')),
                    );
                  } else {
                    shareDocumentFile(
                      bytes,
                      'factura_${widget.invoiceId}.pdf',
                      'application/pdf',
                    );
                  }
                },
              );
            },
            child: const Text('Compartir PDF'),
          ),
          MenuItemButton(
            onPressed: () async {
              final r = await repo.downloadXml(widget.invoiceId);
              if (!mounted) return;
              r.fold(
                (f) => ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(f.message)),
                ),
                (bytes) {
                  if (bytes.isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('El archivo XML está vacío.')),
                    );
                  } else {
                    shareDocumentFile(
                      bytes,
                      'factura_${widget.invoiceId}.xml',
                      'application/xml',
                    );
                  }
                },
              );
            },
            child: const Text('Compartir XML'),
          ),
        ],
      ),
    );
  }

  String? _resolveStatus(InvoiceState state) {
    if (state is InvoiceDetailLoaded) {
      final detailStatus = state.invoice.status;
      if (widget.knownStatus != null && widget.knownStatus!.isNotEmpty) {
        return widget.knownStatus;
      }
      return detailStatus;
    }
    return widget.knownStatus;
  }

  bool _canShowDocuments(InvoiceState state) {
    if (state is InvoiceDetailLoaded) {
      final inv = state.invoice;
      if (inv.invoiceNumber.isEmpty) return false;
      final s = (_resolveStatus(state) ?? '').toUpperCase();
      if (s == 'DEVUELTA' || s == 'RECHAZADO' || s == 'NO AUTORIZADO') {
        return false;
      }
      return true;
    }
    return false;
  }

  @override
  Widget build(BuildContext context) {
    final repo = getIt<InvoiceRepository>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle de Factura'),
        actions: [
          BlocBuilder<InvoiceBloc, InvoiceState>(
            buildWhen: (_, current) =>
                current is InvoiceDetailLoading ||
                current is InvoiceDetailLoaded ||
                current is InvoiceDetailError,
            builder: (context, state) {
              if (!_canShowDocuments(state)) return const SizedBox.shrink();
              return Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _buildPdfAction(repo),
                  _buildShareAction(repo),
                ],
              );
            },
          ),
        ],
      ),
      body: BlocBuilder<InvoiceBloc, InvoiceState>(
        buildWhen: (_, current) =>
            current is InvoiceDetailLoading ||
            current is InvoiceDetailLoaded ||
            current is InvoiceDetailError,
        builder: (context, state) {
          if (state is InvoiceDetailLoading) {
            return const LoadingWidget();
          } else if (state is InvoiceDetailError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: () {
                context.read<InvoiceBloc>().add(
                      LoadInvoiceByIdEvent(widget.invoiceId),
                    );
              },
            );
          } else if (state is InvoiceDetailLoaded && state.invoice.id == widget.invoiceId) {
            final invoice = state.invoice;
            return SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: [
                              Expanded(
                                child: Text(
                                  invoice.invoiceNumber.isNotEmpty
                                      ? 'Factura #${invoice.invoiceNumber}'
                                      : 'Factura (sin número)',
                                  style: Theme.of(context).textTheme.headlineSmall,
                                ),
                              ),
                              const SizedBox(width: 12),
                              StatusChip(status: _resolveStatus(state)),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _sectionTitle(context, 'Fechas'),
                          const SizedBox(height: 12),
                          _DetailRow(
                            label: 'Fecha de emisión',
                            value: app_date_utils.DateUtils.formatDate(
                              invoice.issueDate,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _sectionTitle(context, 'Importes'),
                          const SizedBox(height: 12),
                          _DetailRow(
                            label: 'Subtotal IVA (base)',
                            value: app_date_utils.DateUtils.formatCurrency(
                              invoice.subtotalIva,
                            ),
                          ),
                          _DetailRow(
                            label: 'Subtotal 0%',
                            value: app_date_utils.DateUtils.formatCurrency(
                              invoice.subtotalZero,
                            ),
                          ),
                          if (invoice.subtotalIce > 0)
                            _DetailRow(
                              label: 'ICE',
                              value: app_date_utils.DateUtils.formatCurrency(
                                invoice.subtotalIce,
                              ),
                            ),
                          _DetailRow(
                            label: 'Total',
                            value: app_date_utils.DateUtils.formatCurrency(
                              invoice.totalAmount,
                            ),
                            isTotal: true,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _sectionTitle(context, 'Cliente'),
                          const SizedBox(height: 12),
                          if (invoice.clientName != null)
                            _DetailRow(
                              label: 'Nombre',
                              value: invoice.clientName!,
                            ),
                          _DetailRow(
                            label: 'Identificación',
                            value: invoice.clientIdentification ?? '${invoice.idClient}',
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (invoice.authorizationNumber != null) ...[
                    const SizedBox(height: 12),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _sectionTitle(context, 'Autorización SRI'),
                            const SizedBox(height: 12),
                            _DetailRow(
                              label: 'Número de Autorización',
                              value: invoice.authorizationNumber!,
                            ),
                            if (invoice.authorizationDate != null)
                              _DetailRow(
                                label: 'Fecha de Autorización',
                                value: app_date_utils.DateUtils.formatDate(
                                  invoice.authorizationDate!,
                                ),
                              ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            );
          }
          return const LoadingWidget();
        },
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;
  final bool isTotal;

  const _DetailRow({
    required this.label,
    required this.value,
    this.isTotal = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontWeight: isTotal ? FontWeight.bold : FontWeight.normal,
              fontSize: isTotal ? 18 : 16,
            ),
          ),
          const SizedBox(width: 16),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontWeight: isTotal ? FontWeight.bold : FontWeight.normal,
                fontSize: isTotal ? 18 : 16,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
