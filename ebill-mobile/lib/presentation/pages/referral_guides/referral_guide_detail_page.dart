import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/repositories/referral_guide_repository.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/referral_guide/referral_guide_bloc.dart';
import '../../mixins/document_preview_mixin.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/loading_widget.dart';
import '../../widgets/status_chip.dart';

class ReferralGuideDetailPage extends StatefulWidget {
  const ReferralGuideDetailPage({
    super.key,
    required this.guideId,
  });

  final int guideId;

  @override
  State<ReferralGuideDetailPage> createState() =>
      _ReferralGuideDetailPageState();
}

class _ReferralGuideDetailPageState extends State<ReferralGuideDetailPage>
    with DocumentPreviewMixin {
  bool _companyMissing = false;
  bool _didRequestLoad = false;

  @override
  void initState() {
    super.initState();
    _didRequestLoad = false;
  }

  @override
  void dispose() {
    disposePdfResources();
    super.dispose();
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
    context.read<ReferralGuideBloc>().add(
          LoadReferralGuideByIdEvent(widget.guideId, idCompany: idCompany),
        );
  }

  Future<void> _onPopped() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    final user = auth is AuthAuthenticated ? auth.user : null;
    final idCompany = await resolveCompanyId(storage, user);
    if (!mounted) return;
    context.read<ReferralGuideBloc>().add(
          LoadReferralGuidesEvent(idCompany: idCompany),
        );
  }

  @override
  Widget build(BuildContext context) {
    final repo = getIt<ReferralGuideRepository>();
    return PopScope(
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) await _onPopped();
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Guía de remisión'),
          actionsIconTheme: IconThemeData(
            color: Theme.of(context).colorScheme.primary,
            size: 24,
          ),
          actions: [
            buildPdfPreviewButton(
              () => repo.downloadPdf(widget.guideId),
              title: 'Guía de remisión ${widget.guideId}',
              pdfFileName: 'guia_remision_${widget.guideId}.pdf',
              downloadXml: () => repo.downloadXml(widget.guideId),
              xmlFileName: 'guia_remision_${widget.guideId}.xml',
            ),
            buildSharePopupMenu(
              downloadPdf: () => repo.downloadPdf(widget.guideId),
              downloadXml: () => repo.downloadXml(widget.guideId),
              pdfFileName: 'guia_remision_${widget.guideId}.pdf',
              xmlFileName: 'guia_remision_${widget.guideId}.xml',
            ),
          ],
        ),
        body: BlocBuilder<ReferralGuideBloc, ReferralGuideState>(
          buildWhen: (_, current) =>
              current is ReferralGuideDetailLoading ||
              current is ReferralGuideDetailLoaded ||
              current is ReferralGuideDetailError,
          builder: (context, state) {
            if (state is ReferralGuideDetailLoading) {
              return const LoadingWidget();
            }
            if (state is ReferralGuideDetailError) {
              return app_error.ErrorWidget(
                message: state.message,
                onRetry: _load,
              );
            }
            if (state is ReferralGuideDetailLoaded && state.guide.id == widget.guideId) {
              final g = state.guide;
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
                            const SizedBox(height: 12),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.center,
                              children: [
                                Expanded(
                                  child: Text(
                                    'Guía #${g.guideNumber.isEmpty ? '—' : g.guideNumber}',
                                    style: Theme.of(context)
                                        .textTheme
                                        .headlineSmall,
                                  ),
                                ),
                                StatusChip(status: g.status),
                              ],
                            ),
                            const Divider(),
                            _DetailRow(
                              label: 'Fecha emisión',
                              value: app_date_utils.DateUtils.formatDate(
                                g.issueDate,
                              ),
                            ),
                            _DetailRow(
                              label: 'Inicio transporte',
                              value: app_date_utils.DateUtils.formatDate(
                                g.startDate,
                              ),
                            ),
                            _DetailRow(
                              label: 'Cliente',
                              value: g.clientName ?? '—',
                            ),
                            _DetailRow(
                              label: 'Origen',
                              value: g.originAddress.isEmpty
                                  ? '—'
                                  : g.originAddress,
                            ),
                            _DetailRow(
                              label: 'Destino',
                              value: g.destinationAddress.isEmpty
                                  ? '—'
                                  : g.destinationAddress,
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
                            Text(
                              'Transporte',
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
                            _DetailRow(
                              label: 'Transportista',
                              value: g.carrierName.isEmpty
                                  ? '—'
                                  : g.carrierName,
                            ),
                            _DetailRow(
                              label: 'RUC transportista',
                              value: g.carrierRuc.isEmpty
                                  ? '—'
                                  : g.carrierRuc,
                            ),
                            _DetailRow(
                              label: 'Placa',
                              value: g.licensePlate.isEmpty
                                  ? '—'
                                  : g.licensePlate,
                            ),
                          ],
                        ),
                      ),
                    ),
                    if (g.authorizationNumber != null &&
                        g.authorizationNumber!.isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
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
                              const SizedBox(height: 12),
                              SelectableText(g.authorizationNumber!),
                              if (g.authorizationDate != null)
                                Text(
                                  'Fecha: ${app_date_utils.DateUtils.formatDate(g.authorizationDate!)}',
                                  style: TextStyle(
                                    color: Theme.of(context)
                                        .colorScheme
                                        .onSurfaceVariant,
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
            if (_companyMissing) {
              return const Center(
                child: Text('No se pudo determinar la compañía.'),
              );
            }
            if (state is! ReferralGuideDetailLoading) {
              if (!_didRequestLoad) {
                _didRequestLoad = true;
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (mounted) _load();
                });
              }
            }
            return const LoadingWidget();
          },
        ),
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;

  const _DetailRow({
    required this.label,
    required this.value,
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
              fontSize: 15,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(width: 16),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style:
                  const TextStyle(fontWeight: FontWeight.w500, fontSize: 15),
            ),
          ),
        ],
      ),
    );
  }
}
