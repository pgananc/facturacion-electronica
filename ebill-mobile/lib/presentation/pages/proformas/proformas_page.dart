import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/proforma/proforma_bloc.dart';
import '../../widgets/skeleton_loader.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/status_chip.dart';

class ProformasPage extends StatefulWidget {
  const ProformasPage({super.key});

  @override
  State<ProformasPage> createState() => _ProformasPageState();
}

class _ProformasPageState extends State<ProformasPage> {
  bool _companyMissing = false;
  ProformaListFilter _filter = ProformaListFilter.all;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadProformas());
  }

  Future<void> _loadProformas({bool forceRefresh = false}) async {
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
          LoadProformasEvent(
            idCompany: idCompany,
            filter: _filter,
            forceRefresh: forceRefresh,
          ),
        );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Proformas')),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nueva proforma',
        onPressed: () => context.push('/proformas/new'),
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
                  _filterChip('Todas', ProformaListFilter.all),
                  const SizedBox(width: 8),
                  _filterChip('Vigentes', ProformaListFilter.vigentes),
                  const SizedBox(width: 8),
                  _filterChip('Vencidas', ProformaListFilter.vencidas),
                  const SizedBox(width: 8),
                  _filterChip('Convertidas', ProformaListFilter.convertidas),
                ],
              ),
            ),
          ),
          Expanded(
            child: BlocBuilder<ProformaBloc, ProformaState>(
              buildWhen: (_, current) =>
                  current is ProformaLoading ||
                  current is ProformaLoaded ||
                  current is ProformaListError,
              builder: (context, state) {
                if (state is ProformaLoading) return const SkeletonList();
                if (state is ProformaListError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _loadProformas,
                  );
                }
                if (state is ProformaLoaded) {
                  if (state.proformas.isEmpty) {
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.description_outlined, size: 64, color: cs.outlineVariant),
                          const SizedBox(height: 16),
                          Text('No hay proformas', style: Theme.of(context).textTheme.titleMedium),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: () => context.push('/proformas/new'),
                            icon: const Icon(Icons.add),
                            label: const Text('Crear proforma'),
                          ),
                        ],
                      ),
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () async => _loadProformas(forceRefresh: true),
                    child: ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: state.proformas.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final p = state.proformas[index];
                        return Card(
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () => context.push('/proformas/${p.id}'),
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Row(
                                children: [
                                  Container(
                                    width: 44,
                                    height: 44,
                                    decoration: BoxDecoration(
                                      color: cs.secondaryContainer,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Icon(Icons.description_outlined, color: cs.secondary, size: 22),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          'Proforma #${p.proformaNumber}',
                                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          p.clientName ?? app_date_utils.DateUtils.formatDate(p.issueDate),
                                          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ],
                                    ),
                                  ),
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.end,
                                    children: [
                                      Text(
                                        app_date_utils.DateUtils.formatCurrency(p.totalAmount),
                                        style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      const SizedBox(height: 6),
                                      StatusChip(status: p.status),
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
                  return const Center(child: Text('No se pudo determinar la compañía.'));
                }
                return const SkeletonList();
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _filterChip(String label, ProformaListFilter filter) {
    return ChoiceChip(
      label: Text(label),
      selected: _filter == filter,
      onSelected: (_) {
        setState(() => _filter = filter);
        _loadProformas(forceRefresh: true);
      },
    );
  }
}
