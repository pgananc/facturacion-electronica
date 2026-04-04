import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/customer/customer_bloc.dart';
import '../../widgets/skeleton_loader.dart';
import '../../widgets/error_widget.dart' as app_error;

class CustomersPage extends StatefulWidget {
  const CustomersPage({super.key});

  @override
  State<CustomersPage> createState() => _CustomersPageState();
}

class _CustomersPageState extends State<CustomersPage> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _loadWithCompany();
  }

  Future<void> _loadWithCompany({
    String? search,
    bool forceRefresh = false,
  }) async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    User? user;
    if (auth is AuthAuthenticated) user = auth.user;
    final cid = await resolveCompanyId(storage, user);
    final ruc = storage.getCompanyIdentification() ?? user?.companyIdentification;
    if (!mounted) return;
    context.read<CustomerBloc>().add(
          LoadCustomersEvent(
            idCompany: cid,
            companyIdentification: ruc,
            search: search,
            forceRefresh: forceRefresh,
          ),
        );
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  void _onSearchChanged(String value) {
    setState(() {});
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      _loadWithCompany(search: value.isEmpty ? null : value, forceRefresh: true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Clientes'),
        actions: const [],
      ),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nuevo cliente',
        onPressed: () => context.push('/customers/new'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Buscar clientes...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        tooltip: 'Limpiar búsqueda',
                        onPressed: () {
                          _searchController.clear();
                          _onSearchChanged('');
                        },
                      )
                    : null,
              ),
              onChanged: _onSearchChanged,
            ),
          ),
          Expanded(
            child: BlocConsumer<CustomerBloc, CustomerState>(
              listenWhen: (_, current) =>
                  current is CustomerCreated ||
                  current is CustomerUpdated ||
                  current is CustomerDeleted ||
                  current is CustomerListError ||
                  current is CustomerError,
              listener: (context, state) {
                if (state is CustomerCreated) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: const Text('Cliente creado exitosamente'),
                      backgroundColor: cs.primary,
                    ),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is CustomerUpdated) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: const Text('Cliente actualizado exitosamente'),
                      backgroundColor: cs.primary,
                    ),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is CustomerDeleted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: const Text('Cliente eliminado exitosamente'),
                      backgroundColor: cs.primary,
                    ),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is CustomerListError) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(state.message),
                      backgroundColor: cs.error,
                    ),
                  );
                } else if (state is CustomerError) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(state.message),
                      backgroundColor: cs.error,
                    ),
                  );
                }
              },
              buildWhen: (_, current) =>
                  current is CustomerLoading ||
                  current is CustomersLoaded ||
                  current is CustomerListError ||
                  current is CustomerError,
              builder: (context, state) {
                if (state is CustomerLoading) return const SkeletonList();
                if (state is CustomerError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: () => _loadWithCompany(forceRefresh: true),
                  );
                }
                if (state is CustomerListError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: () => _loadWithCompany(),
                  );
                }
                if (state is CustomersLoaded) {
                  if (state.customers.isEmpty) {
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.people_outline, size: 64, color: cs.outlineVariant),
                          const SizedBox(height: 16),
                          Text('No hay clientes', style: Theme.of(context).textTheme.titleMedium),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: () => context.push('/customers/new'),
                            icon: const Icon(Icons.add),
                            label: const Text('Crear cliente'),
                          ),
                        ],
                      ),
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () async => _loadWithCompany(forceRefresh: true),
                    child: ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: state.customers.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final c = state.customers[index];
                        return Card(
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () => context.push('/customers/${c.id}'),
                            child: Padding(
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                children: [
                                  CircleAvatar(
                                    radius: 22,
                                    backgroundColor: cs.primaryContainer,
                                    child: Text(
                                      c.name.isNotEmpty ? c.name[0].toUpperCase() : '?',
                                      style: TextStyle(
                                        color: cs.onPrimaryContainer,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          c.name,
                                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          'Identificación: ${c.identification}',
                                          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        if (c.email != null)
                                          Text(
                                            c.email!,
                                            style: Theme.of(context).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                                            maxLines: 1,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                      ],
                                    ),
                                  ),
                                  PopupMenuButton(
                                    itemBuilder: (context) => [
                                      const PopupMenuItem(
                                        value: 'edit',
                                        child: Row(
                                          children: [
                                            Icon(Icons.edit, size: 20),
                                            SizedBox(width: 8),
                                            Text('Editar'),
                                          ],
                                        ),
                                      ),
                                      PopupMenuItem(
                                        value: 'delete',
                                        child: Row(
                                          children: [
                                            Icon(
                                              Icons.delete,
                                              size: 20,
                                              color: Theme.of(context).colorScheme.error,
                                            ),
                                            const SizedBox(width: 8),
                                            Text(
                                              'Eliminar',
                                              style: TextStyle(
                                                color: Theme.of(context).colorScheme.error,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                    onSelected: (value) {
                                      if (value == 'edit') {
                                        context.push('/customers/${c.id}/edit');
                                      } else if (value == 'delete') {
                                        _showDeleteDialog(context, c.id, c.name);
                                      }
                                    },
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
                return const SkeletonList();
              },
            ),
          ),
        ],
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, int id, String name) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Cliente'),
        content: Text('¿Estás seguro de que deseas eliminar a $name?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () {
              context.read<CustomerBloc>().add(DeleteCustomerEvent(id));
              Navigator.of(context).pop();
            },
            style: TextButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );
  }
}
