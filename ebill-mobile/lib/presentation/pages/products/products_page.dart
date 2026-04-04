import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/product/product_bloc.dart';
import '../../widgets/skeleton_loader.dart';
import '../../widgets/error_widget.dart' as app_error;

class ProductsPage extends StatefulWidget {
  const ProductsPage({super.key});

  @override
  State<ProductsPage> createState() => _ProductsPageState();
}

class _ProductsPageState extends State<ProductsPage> {
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
    if (!mounted) return;
    context.read<ProductBloc>().add(
          LoadProductsEvent(
            idCompany: cid,
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
        title: const Text('Productos'),
        actions: const [],
      ),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nuevo producto',
        onPressed: () => context.push('/products/new'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Buscar productos...',
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
            child: BlocConsumer<ProductBloc, ProductState>(
              listenWhen: (_, current) =>
                  current is ProductCreated ||
                  current is ProductUpdated ||
                  current is ProductDeleted ||
                  current is ProductError,
              listener: (context, state) {
                if (state is ProductCreated) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: const Text('Producto creado exitosamente'), backgroundColor: cs.primary),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is ProductUpdated) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: const Text('Producto actualizado exitosamente'), backgroundColor: cs.primary),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is ProductDeleted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: const Text('Producto eliminado exitosamente'), backgroundColor: cs.primary),
                  );
                  _loadWithCompany(forceRefresh: true);
                } else if (state is ProductError) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(state.message), backgroundColor: cs.error),
                  );
                }
              },
              buildWhen: (_, current) =>
                  current is ProductLoading ||
                  current is ProductsLoaded ||
                  current is ProductListError ||
                  current is ProductError,
              builder: (context, state) {
                if (state is ProductLoading) return const SkeletonList();
                if (state is ProductError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: () => _loadWithCompany(forceRefresh: true),
                  );
                }
                if (state is ProductListError) {
                  return app_error.ErrorWidget(
                    message: state.message,
                    onRetry: () => _loadWithCompany(),
                  );
                }
                if (state is ProductsLoaded) {
                  if (state.products.isEmpty) {
                    return Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.inventory_2_outlined, size: 64, color: cs.outlineVariant),
                          const SizedBox(height: 16),
                          Text('No hay productos', style: Theme.of(context).textTheme.titleMedium),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: () => context.push('/products/new'),
                            icon: const Icon(Icons.add),
                            label: const Text('Crear producto'),
                          ),
                        ],
                      ),
                    );
                  }
                  return RefreshIndicator(
                    onRefresh: () async => _loadWithCompany(forceRefresh: true),
                    child: ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
                      itemCount: state.products.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final p = state.products[index];
                        return Card(
                          child: InkWell(
                            borderRadius: BorderRadius.circular(12),
                            onTap: () => context.push('/products/${p.id}'),
                            child: Padding(
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                children: [
                                  Container(
                                    width: 44,
                                    height: 44,
                                    decoration: BoxDecoration(
                                      color: cs.tertiaryContainer,
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Icon(Icons.inventory_2_outlined, color: cs.tertiary, size: 22),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          p.name,
                                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          'Código: ${p.mainCode}',
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
                                        app_date_utils.DateUtils.formatCurrency(p.unitPrice),
                                        style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
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
                                            context.push('/products/${p.id}/edit');
                                          } else if (value == 'delete') {
                                            _showDeleteDialog(context, p.id, p.name);
                                          }
                                        },
                                      ),
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
        title: const Text('Eliminar Producto'),
        content: Text('¿Estás seguro de que deseas eliminar $name?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () {
              context.read<ProductBloc>().add(DeleteProductEvent(id));
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
