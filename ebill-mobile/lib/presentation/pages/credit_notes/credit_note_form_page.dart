import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../core/utils/invoice_tax_calculator.dart';
import '../../../domain/entities/credit_note.dart';
import '../../../domain/entities/customer.dart';
import '../../../domain/entities/invoice_item.dart';
import '../../../domain/entities/product.dart';
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/credit_note/credit_note_bloc.dart';
import '../../bloc/customer/customer_bloc.dart';
import '../../bloc/product/product_bloc.dart';
import '../../widgets/loading_widget.dart';

class CreditNoteFormPage extends StatefulWidget {
  const CreditNoteFormPage({super.key});

  @override
  State<CreditNoteFormPage> createState() => _CreditNoteFormPageState();
}

class _CreditNoteFormPageState extends State<CreditNoteFormPage> {
  bool _submitting = false;
  final _formKey = GlobalKey<FormState>();
  final _invoiceIdController = TextEditingController();
  final _reasonController = TextEditingController();

  Customer? _selectedCustomer;
  final List<InvoiceItem> _items = [];
  int? _idCompany;

  @override
  void initState() {
    super.initState();
    _initCompany();
  }

  @override
  void dispose() {
    _invoiceIdController.dispose();
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _initCompany() async {
    final storage = getIt<StorageService>();
    final auth = context.read<AuthBloc>().state;
    User? user;
    if (auth is AuthAuthenticated) user = auth.user;
    final cid = await resolveCompanyId(storage, user);
    final ruc = storage.getCompanyIdentification() ?? user?.companyIdentification;
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

  InvoiceTotals get _totals => InvoiceTaxCalculator.compute(_items);

  bool get _isDirty =>
      _submitting ||
      _selectedCustomer != null ||
      _items.isNotEmpty ||
      _invoiceIdController.text.isNotEmpty ||
      _reasonController.text.isNotEmpty;

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
        title: const Text('Nueva nota de crédito'),
      ),
      body: BlocListener<CreditNoteBloc, CreditNoteState>(
        listener: (context, state) {
          if (state is CreditNoteCreated) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text('Nota de crédito creada'),
                backgroundColor: Theme.of(context).colorScheme.primary,
              ),
            );
            context.go('/credit-notes');
          } else if (state is CreditNoteError) {
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
                if (_idCompany == null)
                  const Card(
                    child: Padding(
                      padding: EdgeInsets.all(12),
                      child: Text(
                        'No se pudo resolver la compañía. Vuelva a iniciar sesión.',
                      ),
                    ),
                  )
                else ...[
                  BlocBuilder<CustomerBloc, CustomerState>(
                    buildWhen: (prev, curr) =>
                        curr is CustomerLoading ||
                        curr is CustomersLoaded ||
                        curr is CustomerListError ||
                        curr is CustomerError,
                    builder: (context, state) {
                      if (state is CustomersLoaded) {
                        return Autocomplete<Customer>(
                          displayStringForOption: (c) =>
                              '${c.name} (${c.identification})',
                          optionsBuilder: (TextEditingValue v) {
                            final q = v.text.toLowerCase();
                            if (q.isEmpty) return state.customers.take(20);
                            return state.customers
                                .where(
                                  (c) =>
                                      c.name.toLowerCase().contains(q) ||
                                      c.identification.contains(q),
                                )
                                .take(30);
                          },
                          onSelected: (c) => setState(() => _selectedCustomer = c),
                          fieldViewBuilder:
                              (context, controller, focusNode, onSubmit) {
                            return TextFormField(
                              controller: controller,
                              focusNode: focusNode,
                              decoration: const InputDecoration(
                                labelText: 'Cliente *',
                                prefixIcon: Icon(Icons.person_outline),
                                hintText: 'Buscar por nombre o RUC',
                              ),
                              validator: (_) => _selectedCustomer == null
                                  ? 'Seleccione un cliente'
                                  : null,
                            );
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
                                color: Theme.of(context).colorScheme.error,
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
                      return const LoadingWidget();
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _invoiceIdController,
                    keyboardType: TextInputType.number,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      labelText: 'ID factura original *',
                      prefixIcon: Icon(Icons.receipt_long_outlined),
                      helperText: 'ID de la factura autorizada a modificar',
                    ),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) {
                        return 'Ingrese el ID de la factura';
                      }
                      final n = int.tryParse(v.trim());
                      if (n == null || n <= 0) {
                        return 'ID inválido';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _reasonController,
                    maxLines: 2,
                    textInputAction: TextInputAction.done,
                    decoration: const InputDecoration(
                      labelText: 'Motivo *',
                      prefixIcon: Icon(Icons.notes_outlined),
                    ),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) {
                        return 'Ingrese el motivo';
                      }
                      return null;
                    },
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
                        onPressed: _showAddProductSheet,
                        icon: const Icon(Icons.add),
                        label: const Text('Agregar'),
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
                            label: 'Subtotal IVA (base gravada)',
                            value: _totals.subtotalIva,
                          ),
                          _TotalRow(
                            label: 'Subtotal 0%',
                            value: _totals.subtotalZero,
                          ),
                          _TotalRow(
                            label: 'ICE',
                            value: _totals.subtotalIce,
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
                          : const Text('Crear nota de crédito'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    ),
    );
  }

  Widget _buildItemCard(InvoiceItem item, int index) {
    final line = InvoiceTaxCalculator.compute([item]);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(item.product.name),
        subtitle: Text(
          'Cantidad: ${item.quantity} × ${app_date_utils.DateUtils.formatCurrency(item.product.unitPrice)}',
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              app_date_utils.DateUtils.formatCurrency(line.totalAmount),
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            IconButton(
              icon: Icon(Icons.delete, color: Theme.of(context).colorScheme.error),
              tooltip: 'Eliminar',
              onPressed: () => setState(() => _items.removeAt(index)),
            ),
          ],
        ),
      ),
    );
  }

  void _showAddProductSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(ctx).viewInsets.bottom,
            left: 16,
            right: 16,
            top: 16,
          ),
          child: BlocBuilder<ProductBloc, ProductState>(
            buildWhen: (prev, curr) =>
                curr is ProductLoading ||
                curr is ProductsLoaded ||
                curr is ProductListError ||
                curr is ProductError,
            builder: (context, state) {
              if (state is ProductListError) {
                return SizedBox(
                  height: 200,
                  child: Center(
                    child: Text(
                      state.message,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ),
                );
              }
              if (state is! ProductsLoaded) {
                return const SizedBox(height: 200, child: LoadingWidget());
              }
              final products = state.products.where((p) => p.isActive).toList();
              return _ProductPicker(
                products: products,
                onPick: (p, qty) {
                  setState(() {
                    _items.add(InvoiceItem(product: p, quantity: qty));
                  });
                  Navigator.pop(ctx);
                },
              );
            },
          ),
        );
      },
    );
  }

  void _submitForm() {
    if (_submitting) return;
    FocusScope.of(context).unfocus();
    setState(() => _submitting = true);
    final emission = getIt<StorageService>().getEmissionSession();
    if (emission == null) {
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('Seleccione un punto de emisión en el menú principal.'),
          backgroundColor: Theme.of(context).colorScheme.tertiary,
        ),
      );
      context.push('/emission-point');
      return;
    }
    if (!_formKey.currentState!.validate() ||
        _selectedCustomer == null ||
        _items.isEmpty ||
        _idCompany == null) {
      setState(() => _submitting = false);
      if (_selectedCustomer == null || _items.isEmpty || _idCompany == null) {
        final msg = _selectedCustomer == null
            ? 'Seleccione un cliente.'
            : _items.isEmpty
                ? 'Agregue al menos un producto.'
                : 'No se pudo determinar la compañía.';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(msg)),
        );
      }
      return;
    }
    final idInv = int.parse(_invoiceIdController.text.trim());
    final cn = CreditNote(
      id: 0,
      creditNoteNumber: '',
      issueDate: DateTime.now(),
      idClient: _selectedCustomer!.id,
      idCompany: _idCompany!,
      idInvoiceRef: idInv,
      invoiceRefNumber: null,
      reason: _reasonController.text.trim(),
      subtotalIva: _totals.subtotalIva,
      subtotalZero: _totals.subtotalZero,
      subtotalIce: _totals.subtotalIce,
      totalAmount: _totals.totalAmount,
    );
    final details = List<Map<String, dynamic>>.from(_totals.invoiceDetails);
    context.read<CreditNoteBloc>().add(CreateCreditNoteEvent(cn, details));
  }
}

class _ProductPicker extends StatefulWidget {
  const _ProductPicker({
    required this.products,
    required this.onPick,
  });

  final List<Product> products;
  final void Function(Product p, int qty) onPick;

  @override
  State<_ProductPicker> createState() => _ProductPickerState();
}

class _ProductPickerState extends State<_ProductPicker> {
  Product? _selected;
  final _qty = TextEditingController(text: '1');
  String _filter = '';

  @override
  void dispose() {
    _qty.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = widget.products
        .where(
          (p) =>
              p.name.toLowerCase().contains(_filter.toLowerCase()) ||
              p.mainCode.toLowerCase().contains(_filter.toLowerCase()),
        )
        .take(80)
        .toList();
    return SizedBox(
      height: 420,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            decoration: const InputDecoration(
              labelText: 'Buscar producto',
              prefixIcon: Icon(Icons.search),
            ),
            onChanged: (v) => setState(() => _filter = v),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: RadioGroup<Product>(
              groupValue: _selected,
              onChanged: (v) => setState(() => _selected = v),
              child: ListView.builder(
                itemCount: filtered.length,
                itemBuilder: (context, i) {
                  final p = filtered[i];
                  return RadioListTile<Product>(
                    value: p,
                    title: Text(p.name),
                    subtitle:
                        Text(app_date_utils.DateUtils.formatCurrency(p.unitPrice)),
                  );
                },
              ),
            ),
          ),
          TextField(
            controller: _qty,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Cantidad',
              prefixIcon: Icon(Icons.format_list_numbered),
            ),
          ),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: () {
              final q = int.tryParse(_qty.text);
              if (_selected != null && q != null && q > 0) {
                widget.onPick(_selected!, q);
              }
            },
            child: const Text('Agregar'),
          ),
        ],
      ),
    );
  }
}

class _TotalRow extends StatelessWidget {
  const _TotalRow({
    required this.label,
    required this.value,
    this.isTotal = false,
  });

  final String label;
  final double value;
  final bool isTotal;

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
