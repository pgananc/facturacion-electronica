import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/invoice/invoice_bloc.dart';
import '../../widgets/loading_widget.dart';
import '../../widgets/error_widget.dart' as app_error;
import '../../widgets/status_chip.dart';

class DraftsPage extends StatefulWidget {
  const DraftsPage({super.key});

  @override
  State<DraftsPage> createState() => _DraftsPageState();
}

class _DraftsPageState extends State<DraftsPage> {
  bool _loadRequested = false;

  Future<void> _onRefreshDrafts() async {
    final bloc = context.read<InvoiceBloc>();
    bloc.add(const LoadDraftsEvent());
    await bloc.stream.firstWhere(
      (s) => s is InvoiceDraftsLoaded || s is InvoiceListError,
    );
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _loadRequested = true;
        context.read<InvoiceBloc>().add(const LoadDraftsEvent());
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (Navigator.of(context).canPop()) {
              Navigator.of(context).pop();
            } else {
              context.go('/dashboard');
            }
          },
        ),
        title: const Text('Borradores'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
            onPressed: () =>
                context.read<InvoiceBloc>().add(const LoadDraftsEvent()),
          ),
        ],
      ),
      body: BlocConsumer<InvoiceBloc, InvoiceState>(
        listener: (context, state) {
          if (state is InvoiceListError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Theme.of(context).colorScheme.error),
            );
          }
          if (state is InvoiceError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Theme.of(context).colorScheme.error),
            );
          }
          if (state is DraftDeleted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Borrador eliminado')),
            );
            context.read<InvoiceBloc>().add(const LoadDraftsEvent());
          }
        },
        builder: (context, state) {
          if (state is InvoiceLoading) {
            return const LoadingWidget();
          }
          if (state is InvoiceListError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: () =>
                  context.read<InvoiceBloc>().add(const LoadDraftsEvent()),
            );
          }
          if (state is InvoiceError) {
            return app_error.ErrorWidget(
              message: state.message,
              onRetry: () =>
                  context.read<InvoiceBloc>().add(const LoadDraftsEvent()),
            );
          }
          if (state is InvoiceDraftsLoaded) {
            final storage = getIt<StorageService>();
            return FutureBuilder<int?>(
              future: _companyId(storage),
              builder: (context, snap) {
                if (!snap.hasData) {
                  return const Center(child: CircularProgressIndicator());
                }
                final cid = snap.data;
                final drafts = state.drafts.where((d) {
                  if (cid == null) return true;
                  final dc = d['idCompany'];
                  final idC = dc is int ? dc : int.tryParse('$dc');
                  return idC == null || idC == cid;
                }).toList();

                if (drafts.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.drafts_outlined,
                            size: 64,
                            color: Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'No hay borradores guardados',
                            style: Theme.of(context).textTheme.titleMedium,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: () => context.push('/invoices/new'),
                            icon: const Icon(Icons.add),
                            label: const Text('Nueva factura'),
                          ),
                        ],
                      ),
                    ),
                  );
                }
                return RefreshIndicator(
                  onRefresh: _onRefreshDrafts,
                  child: ListView.separated(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
                  itemCount: drafts.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (context, i) {
                    final d = drafts[i];
                    final id = (d['idInvoice'] as num?)?.toInt() ?? 0;
                    final name = d['buyerName'] as String? ?? 'Cliente';
                    final rawTotal = d['totalAmount'];
                    final totalText = _formatDraftTotal(rawTotal);

                    Future<void> confirmDelete() async {
                      final ok = await showDialog<bool>(
                        context: context,
                        builder: (c) => AlertDialog(
                          title: const Text('Eliminar borrador'),
                          content: Text('¿Eliminar borrador #$id?'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(c, false),
                              child: const Text('Cancelar'),
                            ),
                            FilledButton(
                              onPressed: () => Navigator.pop(c, true),
                              child: const Text('Eliminar'),
                            ),
                          ],
                        ),
                      );
                      if (ok == true && context.mounted) {
                        context.read<InvoiceBloc>().add(DeleteDraftEvent(id));
                      }
                    }

                    return Card(
                      child: InkWell(
                        borderRadius: BorderRadius.circular(12),
                        onTap: () => context.push('/invoices/$id'),
                        onLongPress: confirmDelete,
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
                                  Icons.edit_note,
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
                                      'Borrador #$id',
                                      style: Theme.of(context)
                                          .textTheme
                                          .bodyMedium
                                          ?.copyWith(fontWeight: FontWeight.w600),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      name,
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
                                  if (totalText.isNotEmpty)
                                    Text(
                                      totalText,
                                      style: Theme.of(context)
                                          .textTheme
                                          .titleSmall
                                          ?.copyWith(fontWeight: FontWeight.w700),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  if (totalText.isNotEmpty)
                                    const SizedBox(height: 6),
                                  const StatusChip(status: 'BORRADOR'),
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
              },
            );
          }
          if (!_loadRequested) {
            _loadRequested = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (context.mounted) {
                context.read<InvoiceBloc>().add(const LoadDraftsEvent());
              }
            });
          }
          return const LoadingWidget();
        },
      ),
    );
  }

  Future<int?> _companyId(StorageService storage) async {
    final auth = context.read<AuthBloc>().state;
    User? u;
    if (auth is AuthAuthenticated) u = auth.user;
    return resolveCompanyId(storage, u);
  }

  static String _formatDraftTotal(Object? rawTotal) {
    if (rawTotal == null) return '';
    if (rawTotal is num) {
      return app_date_utils.DateUtils.formatCurrency(rawTotal.toDouble());
    }
    final parsed = double.tryParse('$rawTotal');
    if (parsed != null) {
      return app_date_utils.DateUtils.formatCurrency(parsed);
    }
    return '$rawTotal';
  }
}
