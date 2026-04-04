import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/repositories/credit_note_repository.dart';
import '../../bloc/credit_note/credit_note_bloc.dart';
import '../../mixins/document_preview_mixin.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';
import '../../widgets/status_chip.dart';

class CreditNoteDetailPage extends StatefulWidget {
  const CreditNoteDetailPage({
    super.key,
    required this.creditNoteId,
  });

  final int creditNoteId;

  @override
  State<CreditNoteDetailPage> createState() => _CreditNoteDetailPageState();
}

class _CreditNoteDetailPageState extends State<CreditNoteDetailPage>
    with DocumentPreviewMixin {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<CreditNoteBloc>().add(
              LoadCreditNoteByIdEvent(widget.creditNoteId),
            );
      }
    });
  }

  @override
  void dispose() {
    disposePdfResources();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final repo = getIt<CreditNoteRepository>();
    final colorScheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Nota de crédito'),
        actions: [
          IconButtonTheme(
            data: IconButtonThemeData(
              style: IconButton.styleFrom(
                foregroundColor: colorScheme.onPrimaryContainer,
                backgroundColor: colorScheme.primaryContainer,
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                buildPdfPreviewButton(
                  () => repo.downloadPdf(widget.creditNoteId),
                  title: 'Nota de crédito ${widget.creditNoteId}',
                  pdfFileName: 'nota_credito_${widget.creditNoteId}.pdf',
                  downloadXml: () => repo.downloadXml(widget.creditNoteId),
                  xmlFileName: 'nota_credito_${widget.creditNoteId}.xml',
                ),
                buildSharePopupMenu(
                  downloadPdf: () => repo.downloadPdf(widget.creditNoteId),
                  downloadXml: () => repo.downloadXml(widget.creditNoteId),
                  pdfFileName: 'nota_credito_${widget.creditNoteId}.pdf',
                  xmlFileName: 'nota_credito_${widget.creditNoteId}.xml',
                ),
              ],
            ),
          ),
        ],
      ),
      body: BlocBuilder<CreditNoteBloc, CreditNoteState>(
        buildWhen: (_, current) =>
            current is CreditNoteDetailLoading ||
            current is CreditNoteDetailLoaded ||
            current is CreditNoteDetailError,
        builder: (context, state) {
          if (state is CreditNoteDetailLoading) {
            return const LoadingWidget();
          }
          if (state is CreditNoteDetailError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: () => context.read<CreditNoteBloc>().add(
                    LoadCreditNoteByIdEvent(widget.creditNoteId),
                  ),
            );
          }
          if (state is CreditNoteDetailLoaded && state.creditNote.id == widget.creditNoteId) {
            final cn = state.creditNote;
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
                                  'N/C #${cn.creditNoteNumber.isEmpty ? '—' : cn.creditNoteNumber}',
                                  style: Theme.of(context)
                                      .textTheme
                                      .headlineSmall,
                                ),
                              ),
                              const SizedBox(width: 12),
                              StatusChip(status: cn.status),
                            ],
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'Información general',
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
                          const SizedBox(height: 8),
                          _DetailRow(
                            label: 'Fecha',
                            value: app_date_utils.DateUtils.formatDate(
                              cn.issueDate,
                            ),
                          ),
                          _DetailRow(
                            label: 'Cliente',
                            value: cn.clientName ?? '—',
                          ),
                          _DetailRow(
                            label: 'Factura original',
                            value: cn.invoiceRefNumber ??
                                'ID: ${cn.idInvoiceRef}',
                          ),
                          _DetailRow(
                            label: 'Motivo',
                            value: cn.reason,
                          ),
                          if (cn.authorizationNumber != null &&
                              cn.authorizationNumber!.isNotEmpty) ...[
                            const SizedBox(height: 8),
                            const Divider(),
                            const SizedBox(height: 8),
                            Text(
                              'Autorización SRI',
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
                            const SizedBox(height: 8),
                            Align(
                              alignment: Alignment.centerLeft,
                              child: SelectableText(
                                cn.authorizationNumber!,
                                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                      fontWeight: FontWeight.w500,
                                    ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Card(
                    color: Theme.of(context).colorScheme.secondaryContainer,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
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
                          const SizedBox(height: 8),
                          _DetailRow(
                            label: 'Subtotal IVA',
                            value: app_date_utils.DateUtils.formatCurrency(
                              cn.subtotalIva,
                            ),
                          ),
                          _DetailRow(
                            label: 'Subtotal 0%',
                            value: app_date_utils.DateUtils.formatCurrency(
                              cn.subtotalZero,
                            ),
                          ),
                          const Divider(),
                          _DetailRow(
                            label: 'Total',
                            value: app_date_utils.DateUtils.formatCurrency(
                              cn.totalAmount,
                            ),
                            isTotal: true,
                          ),
                        ],
                      ),
                    ),
                  ),
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
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(
              fontWeight: isTotal ? FontWeight.bold : FontWeight.normal,
              fontSize: isTotal ? 18 : 15,
              color: isTotal
                  ? null
                  : Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(width: 16),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontWeight: isTotal ? FontWeight.bold : FontWeight.w500,
                fontSize: isTotal ? 18 : 15,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
