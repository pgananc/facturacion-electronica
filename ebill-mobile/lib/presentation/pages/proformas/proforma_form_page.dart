import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../core/utils/invoice_tax_calculator.dart';
import '../../../domain/entities/customer.dart';
import '../../../domain/entities/invoice_item.dart';
import '../../../domain/entities/proforma.dart';
import '../../../domain/entities/product.dart';
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/proforma/proforma_bloc.dart';
import '../../bloc/customer/customer_bloc.dart';
import '../../bloc/product/product_bloc.dart';
import '../../widgets/loading_widget.dart';

class ProformaFormPage extends StatefulWidget {
  const ProformaFormPage({super.key});

  @override
  State<ProformaFormPage> createState() => _ProformaFormPageState();
}

class _ProformaFormPageState extends State<ProformaFormPage> {
  bool _submitting = false;
  final _formKey = GlobalKey<FormState>();
  Customer? _selectedCustomer;
  int _idPaymentMethod = 1;
  final List<InvoiceItem> _items = [];
  DateTime? _validUntil;
  int? _idCompany;

  @override
  void initState() {
    super.initState();
    _initCompany();
  }

  Future<void> _initCompany() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    User? user;
    if (auth is AuthAuthenticated) user = auth.user;
    final cid = await resolveCompanyId(storage, user);
    final ruc =
        storage.getCompanyIdentification() ?? user?.companyIdentification;
    if (!mounted) return;
    setState(() => _idCompany = cid);
    if (cid != null) {
      context.read<CustomerBloc>().add(
            LoadCustomersEvent(
              idCompany: cid,
              companyIdentification: ruc,
              pageSize: 200,
            ),
          );
      context.read<ProductBloc>().add(LoadProductsEvent(idCompany: cid));
    }
  }

  Future<void> _pickValidUntil() async {
    final initial = _validUntil ?? DateTime.now().add(const Duration(days: 30));
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
    );
    if (!mounted) return;
    if (picked != null) {
      setState(() => _validUntil = picked);
    }
  }

  bool get _isDirty => _submitting || _selectedCustomer != null || _items.isNotEmpty;

  Future<bool> _confirmDiscard() async {
    if (!_isDirty) return true;
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Descartar cambios'),
        content: const Text('¿Seguro que deseas salir? Se perderán los datos ingresados.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Descartar')),
        ],
      ),
    );
    return result ?? false;
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;
        if (await _confirmDiscard()) {
          if (context.mounted) Navigator.of(context).pop();
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Nueva Proforma'),
        ),
        body: BlocListener<ProformaBloc, ProformaState>(
        listener: (context, state) {
          if (state is ProformaCreated) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text('Proforma creada exitosamente'),
                backgroundColor: Theme.of(context).colorScheme.primary,
              ),
            );
            context.go('/proformas');
          } else if (state is ProformaFormError) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Theme.of(context).colorScheme.error,
              ),
            );
          }
        },
        child: SingleChildScrollView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(16.0),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                BlocBuilder<CustomerBloc, CustomerState>(
                  buildWhen: (prev, curr) =>
                      curr is CustomerLoading ||
                      curr is CustomersLoaded ||
                      curr is CustomerListError ||
                      curr is CustomerError,
                  builder: (context, state) {
                    if (state is CustomersLoaded) {
                      return DropdownButtonFormField<Customer>(
                        decoration: const InputDecoration(
                          labelText: 'Cliente *',
                          prefixIcon: Icon(Icons.person_outline),
                        ),
                        initialValue: _selectedCustomer,
                        items: state.customers.map((customer) {
                          return DropdownMenuItem<Customer>(
                            value: customer,
                            child: Text(
                              '${customer.name} - ${customer.identification}',
                            ),
                          );
                        }).toList(),
                        onChanged: (customer) {
                          setState(() {
                            _selectedCustomer = customer;
                          });
                        },
                        validator: (value) {
                          if (value == null) {
                            return 'Seleccione un cliente';
                          }
                          return null;
                        },
                      );
                    }
                    if (state is CustomerListError || state is CustomerError) {
                      final msg = state is CustomerListError
                          ? state.message
                          : (state as CustomerError).message;
                      return Column(
                        children: [
                          Text(
                            msg,
                            style: TextStyle(
                              color:
                                  Theme.of(context).colorScheme.error,
                            ),
                          ),
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: _initCompany,
                            child: const Text('Reintentar'),
                          ),
                        ],
                      );
                    }
                    return const Text('Cargando clientes...');
                  },
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<int>(
                  decoration: const InputDecoration(
                    labelText: 'Método de Pago *',
                    prefixIcon: Icon(Icons.payment_outlined),
                  ),
                  initialValue: _idPaymentMethod,
                  items: const [
                    DropdownMenuItem(value: 1, child: Text('Efectivo')),
                    DropdownMenuItem(value: 2, child: Text('Tarjeta')),
                    DropdownMenuItem(value: 3, child: Text('Transferencia')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _idPaymentMethod = value!;
                    });
                  },
                ),
                const SizedBox(height: 16),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Válida hasta (opcional)'),
                  subtitle: Text(
                    _validUntil == null
                        ? 'Sin fecha de vencimiento'
                        : app_date_utils.DateUtils.formatDate(_validUntil!),
                  ),
                  trailing: IconButton(
                    icon: const Icon(Icons.calendar_today),
                    tooltip: 'Seleccionar fecha',
                    onPressed: _pickValidUntil,
                  ),
                ),
                TextButton(
                  onPressed: () => setState(() => _validUntil = null),
                  child: const Text('Quitar fecha de vencimiento'),
                ),
                const SizedBox(height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Productos',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    TextButton.icon(
                      onPressed: _showAddProductDialog,
                      icon: const Icon(Icons.add),
                      label: const Text('Agregar Producto'),
                    ),
                  ],
                ),
                if (_items.isEmpty)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Center(
                        child: Text(
                          'No hay productos agregados',
                          style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                        ),
                      ),
                    ),
                  )
                else
                  for (int i = 0; i < _items.length; i++)
                    _buildItemCard(_items[i], i),
                const SizedBox(height: 24),
                Card(
                  color: Theme.of(context).colorScheme.secondaryContainer,
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      children: [
                        _TotalRow(
                          label: 'Subtotal IVA',
                          value: _totals.subtotalIva,
                        ),
                        _TotalRow(
                          label: 'Subtotal 0%',
                          value: _totals.subtotalZero,
                        ),
                        const Divider(),
                        _TotalRow(
                          label: 'Total',
                          value: _totals.totalAmount,
                          isTotal: true,
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                SizedBox(
                  height: 52,
                  child: FilledButton(
                    onPressed: (_items.isEmpty || _submitting) ? null : _submitForm,
                    child: _submitting
                        ? SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Theme.of(context).colorScheme.onPrimary,
                            ),
                          )
                        : const Text('Crear Proforma'),
                  ),
                ),
              ],
            ),
          ),
        ),
        ),
      ),
    );
  }

  Widget _buildItemCard(InvoiceItem item, int index) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(item.product.name),
        subtitle: Text(
          'Cantidad: ${item.quantity} x ${app_date_utils.DateUtils.formatCurrency(item.product.unitPrice)}',
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              app_date_utils.DateUtils.formatCurrency(
                item.quantity * item.product.unitPrice,
              ),
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            IconButton(
              icon: Icon(Icons.delete, color: Theme.of(context).colorScheme.error),
              tooltip: 'Eliminar',
              onPressed: () {
                setState(() {
                  _items.removeAt(index);
                });
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showAddProductDialog() {
    showDialog(
      context: context,
      builder: (context) => _AddProductDialog(
        idCompany: _idCompany,
        onAdd: (product, quantity) {
          setState(() {
            _items.add(InvoiceItem(product: product, quantity: quantity));
          });
        },
      ),
    );
  }

  InvoiceTotals get _totals => InvoiceTaxCalculator.compute(_items);

  Future<void> _submitForm() async {
    if (_submitting) return;
    FocusScope.of(context).unfocus();
    setState(() => _submitting = true);
    if (_idCompany == null) {
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text(
            'No hay compañía en la sesión. Seleccione una en Ajustes o vuelva a iniciar sesión.',
          ),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
      return;
    }

    if (_formKey.currentState!.validate() &&
        _selectedCustomer != null &&
        _items.isNotEmpty) {
      final emission = getIt<StorageService>().getEmissionSession();
      if (emission == null) {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text(
              'Seleccione un punto de emisión en el flujo de emisión antes de crear la proforma.',
            ),
            backgroundColor: Theme.of(context).colorScheme.tertiary,
          ),
        );
        return;
      }

      final proforma = Proforma(
        id: 0,
        proformaNumber: '',
        issueDate: DateTime.now(),
        expiryDate: _validUntil,
        idClient: _selectedCustomer!.id,
        idCompany: _idCompany!,
        subtotalIva: _totals.subtotalIva,
        subtotalZero: _totals.subtotalZero,
        totalAmount: _totals.totalAmount,
        idPaymentMethod: _idPaymentMethod,
      );

      context.read<ProformaBloc>().add(
            CreateProformaEvent(proforma, _items, emission),
          );
    } else {
      setState(() => _submitting = false);
    }
  }
}

class _TotalRow extends StatelessWidget {
  final String label;
  final double value;
  final bool isTotal;

  const _TotalRow({
    required this.label,
    required this.value,
    this.isTotal = false,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: TextStyle(
              fontWeight: isTotal ? FontWeight.bold : FontWeight.normal,
              fontSize: isTotal ? 18 : 16,
            ),
          ),
          Text(
            app_date_utils.DateUtils.formatCurrency(value),
            style: TextStyle(
              fontWeight: isTotal ? FontWeight.bold : FontWeight.normal,
              fontSize: isTotal ? 18 : 16,
            ),
          ),
        ],
      ),
    );
  }
}

class _AddProductDialog extends StatefulWidget {
  final int? idCompany;
  final void Function(Product, int) onAdd;

  const _AddProductDialog({required this.onAdd, this.idCompany});

  @override
  State<_AddProductDialog> createState() => _AddProductDialogState();
}

class _AddProductDialogState extends State<_AddProductDialog> {
  Product? _selectedProduct;
  final _quantityController = TextEditingController(text: '1');

  @override
  void dispose() {
    _quantityController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Agregar Producto'),
      content: BlocBuilder<ProductBloc, ProductState>(
        buildWhen: (prev, curr) =>
            curr is ProductLoading ||
            curr is ProductsLoaded ||
            curr is ProductListError ||
            curr is ProductError,
        builder: (context, state) {
          if (state is ProductsLoaded) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<Product>(
                  decoration: const InputDecoration(
                    labelText: 'Producto *',
                    prefixIcon: Icon(Icons.inventory_2_outlined),
                  ),
                  initialValue: _selectedProduct,
                  items: state.products.where((p) => p.isActive).map((product) {
                    return DropdownMenuItem<Product>(
                      value: product,
                      child: Text(
                        '${product.name} - ${app_date_utils.DateUtils.formatCurrency(product.unitPrice)}',
                      ),
                    );
                  }).toList(),
                  onChanged: (product) {
                    setState(() {
                      _selectedProduct = product;
                    });
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _quantityController,
                  decoration: const InputDecoration(
                    labelText: 'Cantidad *',
                    border: OutlineInputBorder(),
                  ),
                  keyboardType: TextInputType.number,
                  textInputAction: TextInputAction.done,
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Ingrese la cantidad';
                    }
                    final qty = int.tryParse(value);
                    if (qty == null || qty <= 0) {
                      return 'Cantidad inválida';
                    }
                    return null;
                  },
                ),
              ],
            );
          }
          if (state is ProductListError) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  state.message,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.error,
                  ),
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: widget.idCompany == null
                      ? null
                      : () => context.read<ProductBloc>().add(
                            LoadProductsEvent(idCompany: widget.idCompany!),
                          ),
                  child: const Text('Reintentar'),
                ),
              ],
            );
          }
          if (state is ProductError) {
            return Text(
              state.message,
              style: TextStyle(
                color: Theme.of(context).colorScheme.error,
              ),
            );
          }
          return const LoadingWidget();
        },
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () {
            if (_selectedProduct != null &&
                _quantityController.text.isNotEmpty) {
              final quantity = int.tryParse(_quantityController.text);
              if (quantity != null && quantity > 0) {
                widget.onAdd(_selectedProduct!, quantity);
                Navigator.of(context).pop();
              }
            }
          },
          child: const Text('Agregar'),
        ),
      ],
    );
  }
}
