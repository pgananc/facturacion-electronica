import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/entities/credit_note.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/credit_note/credit_note_bloc.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/skeleton_loader.dart';
import '../../widgets/status_chip.dart';

class CreditNotesPage extends StatefulWidget {
  const CreditNotesPage({super.key});

  @override
  State<CreditNotesPage> createState() => _CreditNotesPageState();
}

class _CreditNotesPageState extends State<CreditNotesPage> {
  bool _companyMissing = false;
  final _searchController = TextEditingController();
  String _searchQuery = '';
  String? _statusFilter;

  static const _statusOptions = <String?>[
    null,
    'AUTORIZADO',
    'BORRADOR',
    'EN_PROCESO',
    'DEVUELTA',
    'PENDIENTE_ENVIO',
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadCreditNotes());
    _searchController.addListener(() {
      setState(() => _searchQuery = _searchController.text.trim());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadCreditNotes({bool forceRefresh = false}) async {
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
    context.read<CreditNoteBloc>().add(
          LoadCreditNotesEvent(
            idCompany: idCompany,
            forceRefresh: forceRefresh,
          ),
        );
  }

  String _readableStatus(String raw) {
    const map = {
      'PENDIENTE_ENVIO': 'Pendiente de envío',
      'EN_PROCESO': 'En proceso',
      'ENVIADO': 'Enviado',
      'AUTORIZADO': 'Autorizado',
      'RECHAZADO': 'Rechazado',
      'ANULADO': 'Anulado',
      'BORRADOR': 'Borrador',
      'DEVUELTA': 'Devuelta',
    };
    final key = raw.toUpperCase();
    return map[key] ?? raw.replaceAll('_', ' ').toLowerCase();
  }

  List<CreditNote> _filtered(List<CreditNote> all) {
    var list = all;
    if (_statusFilter != null && _statusFilter!.isNotEmpty) {
      list = list
          .where(
            (e) =>
                (e.status ?? '').toUpperCase() == _statusFilter!.toUpperCase(),
          )
          .toList();
    }
    if (_searchQuery.isEmpty) return list;
    final q = _searchQuery.toLowerCase();
    return list.where((e) {
      return e.creditNoteNumber.toLowerCase().contains(q) ||
          (e.clientName ?? '').toLowerCase().contains(q) ||
          (e.invoiceRefNumber ?? '').toLowerCase().contains(q);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Notas de Crédito')),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nueva nota de crédito',
        onPressed: () => context.push('/credit-notes/new'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Buscar por número o cliente',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        tooltip: 'Limpiar búsqueda',
                        onPressed: () {
                          _searchController.clear();
                          setState(() => _searchQuery = '');
                        },
                      )
                    : null,
              ),
            ),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: _statusOptions.map((s) {
                final label = s == null ? 'Todos' : _readableStatus(s);
                final selected = _statusFilter == s;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(label),
                    selected: selected,
                    onSelected: (_) {
                      setState(() => _statusFilter = s);
                    },
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: BlocBuilder<CreditNoteBloc, CreditNoteState>(
              buildWhen: (_, current) =>
                  current is CreditNoteLoading ||
                  current is CreditNoteLoaded ||
                  current is CreditNoteListError,
              builder: (context, state) {
                if (state is CreditNoteLoading) return const SkeletonList();
                if (state is CreditNoteListError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: _loadCreditNotes,
                  );
                }
                if (state is CreditNoteLoaded) {
                  final items = _filtered(state.creditNotes);
                  if (items.isEmpty) {
                    final totallyEmpty = state.creditNotes.isEmpty;
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.note_alt_outlined, size: 64, color: cs.outlineVariant),
                          const SizedBox(height: 16),
                          Text(
                            totallyEmpty
                                ? 'No hay notas de crédito'
                                : 'Sin resultados',
                            style: Theme.of(context).textTheme.titleMedium,
                            textAlign: TextAlign.center,
                          ),
                          if (totallyEmpty) ...[
                            const SizedBox(height: 16),
                            FilledButton.icon(
                              onPressed: () => context.push('/credit-notes/new'),
                              icon: const Icon(Icons.add),
                              label: const Text('Crear nota de crédito'),
                            ),
                          ],
                        ],
                      ),
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () async => _loadCreditNotes(forceRefresh: true),
                    child: ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: items.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final cn = items[index];
                        return Card(
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () => context.push('/credit-notes/${cn.id}'),
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Row(
                                children: [
                                  Container(
                                    width: 44,
                                    height: 44,
                                    decoration: BoxDecoration(
                                      color: cs.errorContainer,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Icon(
                                      Icons.note_alt_outlined,
                                      color: cs.error,
                                      size: 22,
                                    ),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          'NC #${cn.creditNoteNumber}',
                                          style: Theme.of(context)
                                              .textTheme
                                              .bodyMedium
                                              ?.copyWith(fontWeight: FontWeight.w600),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 4),
                                        Text(
                                          cn.clientName ?? app_date_utils.DateUtils.formatDate(cn.issueDate),
                                          style: Theme.of(context)
                                              .textTheme
                                              .bodySmall
                                              ?.copyWith(color: cs.onSurfaceVariant),
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
                                        app_date_utils.DateUtils.formatCurrency(cn.totalAmount),
                                        style: Theme.of(context)
                                            .textTheme
                                            .titleSmall
                                            ?.copyWith(fontWeight: FontWeight.w700),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                      const SizedBox(height: 6),
                                      StatusChip(status: cn.status),
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
                            onPressed: () => _loadCreditNotes(),
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
}
