import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/entities/referral_guide.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/referral_guide/referral_guide_bloc.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/skeleton_loader.dart';
import '../../widgets/status_chip.dart';

enum _GuideFilter { all, authorized, pending, inTransit }

class ReferralGuidesPage extends StatefulWidget {
  const ReferralGuidesPage({super.key});

  @override
  State<ReferralGuidesPage> createState() => _ReferralGuidesPageState();
}

class _ReferralGuidesPageState extends State<ReferralGuidesPage> {
  bool _companyMissing = false;
  _GuideFilter _filter = _GuideFilter.all;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadGuides());
  }

  Future<void> _loadGuides({bool forceRefresh = false}) async {
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
    context.read<ReferralGuideBloc>().add(
          LoadReferralGuidesEvent(
            idCompany: idCompany,
            forceRefresh: forceRefresh,
          ),
        );
  }

  bool _matchesFilter(ReferralGuide g, _GuideFilter f) {
    final s = (g.status ?? '').toUpperCase().trim();
    switch (f) {
      case _GuideFilter.all:
        return true;
      case _GuideFilter.authorized:
        return s.contains('AUTORIZ') || s == 'AUTORIZADA' || s == 'AUTORIZADO';
      case _GuideFilter.pending:
        return s.contains('PEND') ||
            s.contains('BORRADOR') ||
            s == 'DRAFT' ||
            s.isEmpty;
      case _GuideFilter.inTransit:
        return s.contains('TRANSITO') ||
            s.contains('TRÁNSITO') ||
            s.contains('TRANSIT') ||
            s.contains('EN VIAJE');
    }
  }

  List<ReferralGuide> _visible(List<ReferralGuide> guides) {
    return guides.where((g) => _matchesFilter(g, _filter)).toList();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Guías de Remisión')),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nueva guía de remisión',
        onPressed: () => context.push('/referral-guides/new'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 8),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _chip('Todas', _GuideFilter.all),
                  const SizedBox(width: 8),
                  _chip('Autorizadas', _GuideFilter.authorized),
                  const SizedBox(width: 8),
                  _chip('Pendientes', _GuideFilter.pending),
                  const SizedBox(width: 8),
                  _chip('En tránsito', _GuideFilter.inTransit),
                ],
              ),
            ),
          ),
          Expanded(
            child: BlocBuilder<ReferralGuideBloc, ReferralGuideState>(
              buildWhen: (_, current) =>
                  current is ReferralGuideLoading ||
                  current is ReferralGuideLoaded ||
                  current is ReferralGuideListError,
              builder: (context, state) {
                if (state is ReferralGuideLoading) return const SkeletonList();
                if (state is ReferralGuideListError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _loadGuides,
                  );
                }
                if (state is ReferralGuideLoaded) {
                  final visible = _visible(state.guides);
                  if (visible.isEmpty) {
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.local_shipping_outlined, size: 64, color: cs.outlineVariant),
                          const SizedBox(height: 16),
                          Text(
                            state.guides.isEmpty
                                ? 'No hay guías de remisión'
                                : 'Ninguna guía coincide',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                        ],
                      ),
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () async => _loadGuides(forceRefresh: true),
                    child: ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: visible.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final guide = visible[index];
                        return Card(
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () => context.push('/referral-guides/${guide.id}'),
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
                                      Icons.local_shipping_outlined,
                                      color: cs.onPrimaryContainer,
                                      size: 22,
                                    ),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          guide.guideNumber.isEmpty
                                              ? 'Guía #${guide.id}'
                                              : 'Guía ${guide.guideNumber}',
                                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          guide.clientName?.isNotEmpty == true
                                              ? guide.clientName!
                                              : app_date_utils.DateUtils.formatDate(guide.issueDate),
                                          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ],
                                    ),
                                  ),
                                  StatusChip(status: guide.status),
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
                            onPressed: () => _loadGuides(),
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
          ),
        ],
      ),
    );
  }

  Widget _chip(String label, _GuideFilter filter) {
    return ChoiceChip(
      label: Text(label),
      selected: _filter == filter,
      onSelected: (_) => setState(() => _filter = filter),
    );
  }
}
