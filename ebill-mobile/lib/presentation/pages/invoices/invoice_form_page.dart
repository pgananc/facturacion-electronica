import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../core/utils/invoice_tax_calculator.dart';
import '../../../domain/entities/customer.dart';
import '../../../domain/entities/invoice.dart';
import '../../../domain/entities/invoice_item.dart';
import '../../../domain/entities/product.dart';
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/customer/customer_bloc.dart';
import '../../bloc/invoice/invoice_bloc.dart';
import '../../bloc/product/product_bloc.dart';
import '../../widgets/loading_widget.dart';

class InvoiceFormPage extends StatefulWidget {
  const InvoiceFormPage({super.key});

  @override
  State<InvoiceFormPage> createState() => _InvoiceFormPageState();
}

class _InvoiceFormPageState extends State<InvoiceFormPage> {
  bool _submitting = false;
  bool _showClientError = false;
  bool _showProductsError = false;
  int _currentStep = 0;
  Customer? _selectedCustomer;
  int _idPaymentMethod = 1;
  final List<InvoiceItem> _items = [];
  int? _idCompany;
  bool _companyResolveComplete = false;
  final _customerSearchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _initCompany();
  }

  @override
  void dispose() {
    _customerSearchController.dispose();
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
    setState(() {
      _idCompany = cid;
      _companyResolveComplete = true;
    });
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

  bool get _step1Complete => _selectedCustomer != null;
  bool get _step2Complete => _items.isNotEmpty;

  void _onStepContinue() {
    if (_currentStep == 0 && !_step1Complete) {
      setState(() => _showClientError = true);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Seleccione un cliente para continuar')),
      );
      return;
    }
    if (_currentStep == 1 && !_step2Complete) {
      setState(() => _showProductsError = true);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Agregue al menos un producto')),
      );
      return;
    }
    if (_currentStep < 2) {
      setState(() => _currentStep += 1);
    }
  }

  void _onStepCancel() {
    if (_currentStep > 0) {
      setState(() => _currentStep -= 1);
    }
  }

  void _onStepTapped(int index) {
    if (index < _currentStep) {
      setState(() => _currentStep = index);
      return;
    }
    if (index == 1 && _step1Complete) {
      setState(() => _currentStep = index);
    } else if (index == 2 && _step1Complete && _step2Complete) {
      setState(() => _currentStep = index);
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
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        title: const Text('Nueva Factura'),
        actions: [
          TextButton(
            onPressed: _items.isEmpty || _idCompany == null || _selectedCustomer == null
                ? null
                : _saveDraft,
            child: const Text('Borrador'),
          ),
        ],
      ),
      body: BlocListener<InvoiceBloc, InvoiceState>(
        listener: (context, state) {
          if (state is InvoiceCreated) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text('Factura creada exitosamente'),
                backgroundColor: Theme.of(context).colorScheme.primary,
              ),
            );
            context.read<InvoiceBloc>().add(
                  LoadInvoicesEvent(idCompany: _idCompany),
                );
            context.go('/invoices');
          } else if (state is InvoiceDraftSaved) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Borrador guardado')),
            );
            context.pushReplacement('/invoices/drafts');
          } else if (state is InvoiceError) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Theme.of(context).colorScheme.error),
            );
          }
        },
        child: !_companyResolveComplete
            ? const Center(child: LoadingWidget())
            : _idCompany == null
                ? Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.business_outlined,
                            size: 48,
                            color: Theme.of(context).colorScheme.error,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'No se pudo determinar la compañía. Seleccione una compañía en ajustes.',
                            style: Theme.of(context).textTheme.titleMedium,
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 24),
                          FilledButton(
                            onPressed: () => context.push('/select-company'),
                            child: const Text('Seleccionar compañía'),
                          ),
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: _initCompany,
                            child: const Text('Reintentar'),
                          ),
                        ],
                      ),
                    ),
                  )
                : Stepper(
                type: StepperType.horizontal,
                currentStep: _currentStep,
                onStepContinue: _currentStep < 2 ? _onStepContinue : null,
                onStepCancel: _onStepCancel,
                onStepTapped: _onStepTapped,
                controlsBuilder: (context, details) {
                  if (_currentStep == 2) {
                    return Padding(
                      padding: const EdgeInsets.only(top: 16),
                      child: Row(
                        children: [
                          Expanded(
                            child: OutlinedButton(
                              onPressed: details.onStepCancel,
                              child: const Text('Anterior'),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: SizedBox(
                              height: 52,
                              child: FilledButton(
                                onPressed: _submitting ? null : _submitForm,
                                child: _submitting
                                    ? SizedBox(
                                        height: 24,
                                        width: 24,
                                        child: CircularProgressIndicator(
                                          strokeWidth: 2,
                                          color: Theme.of(context).colorScheme.onPrimary,
                                        ),
                                      )
                                    : const Row(
                                        mainAxisAlignment: MainAxisAlignment.center,
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Icon(Icons.send),
                                          SizedBox(width: 8),
                                          Text('Emitir Factura'),
                                        ],
                                      ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }
                  return Padding(
                    padding: const EdgeInsets.only(top: 16),
                    child: Row(
                      children: [
                        if (_currentStep > 0) ...[
                          Expanded(
                            child: OutlinedButton(
                              onPressed: details.onStepCancel,
                              child: const Text('Anterior'),
                            ),
                          ),
                          const SizedBox(width: 12),
                        ],
                        Expanded(
                          child: SizedBox(
                            height: 52,
                            child: FilledButton(
                              onPressed: details.onStepContinue,
                              child: const Text('Siguiente'),
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
                steps: [
                  Step(
                    title: const Text('Cliente'),
                    isActive: _currentStep >= 0,
                    state: _step1Complete
                        ? StepState.complete
                        : (_currentStep == 0 ? StepState.editing : StepState.indexed),
                    content: _buildStep1Client(),
                  ),
                  Step(
                    title: const Text('Productos'),
                    isActive: _currentStep >= 1,
                    state: _step2Complete
                        ? StepState.complete
                        : (_currentStep == 1 ? StepState.editing : StepState.indexed),
                    content: _buildStep2Products(),
                  ),
                  Step(
                    title: const Text('Resumen'),
                    isActive: _currentStep >= 2,
                    state: _currentStep == 2 ? StepState.editing : StepState.indexed,
                    content: _buildStep3Summary(),
                  ),
                ],
              ),
      ),
    ),
    );
  }

  Widget _buildStep1Client() {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return SingleChildScrollView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      padding: EdgeInsets.only(bottom: bottomInset + 16),
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
            if (state is CustomerListError || state is CustomerError) {
              final msg = state is CustomerListError
                  ? state.message
                  : (state as CustomerError).message;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(Icons.error_outline, size: 48, color: Theme.of(context).colorScheme.error),
                  const SizedBox(height: 8),
                  Text(msg, textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () {
                      if (_idCompany != null) {
                        final ruc = getIt<StorageService>().getCompanyIdentification();
                        context.read<CustomerBloc>().add(
                              LoadCustomersEvent(
                                idCompany: _idCompany,
                                companyIdentification: ruc,
                                pageSize: 200,
                              ),
                            );
                      }
                    },
                    child: const Text('Reintentar'),
                  ),
                ],
              );
            }
            if (state is CustomersLoaded) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Autocomplete<Customer>(
                    displayStringForOption: (c) =>
                        '${c.name} (${c.identification})',
                    optionsBuilder: (TextEditingValue v) {
                      final q = v.text.toLowerCase();
                      if (q.isEmpty) return state.customers.take(50);
                      return state.customers
                          .where(
                            (c) =>
                                c.name.toLowerCase().contains(q) ||
                                c.identification.contains(q),
                          )
                          .take(50);
                    },
                    onSelected: (c) => setState(() {
                      _selectedCustomer = c;
                      _showClientError = false;
                    }),
                    fieldViewBuilder: (context, controller, focusNode, onSubmit) {
                      return TextFormField(
                        controller: controller,
                        focusNode: focusNode,
                        decoration: InputDecoration(
                          labelText: 'Cliente *',
                          hintText: 'Buscar por nombre o RUC',
                          prefixIcon: const Icon(Icons.person_outline),
                          suffixIcon: _selectedCustomer != null
                              ? Icon(Icons.check_circle, color: Theme.of(context).colorScheme.primary)
                              : null,
                        ),
                      );
                    },
                  ),
                  if (_showClientError && _selectedCustomer == null)
                    Padding(
                      padding: const EdgeInsets.only(top: 4, left: 12),
                      child: Text(
                        'Seleccione un cliente',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  if (_selectedCustomer != null) ...[
                    const SizedBox(height: 12),
                    Card(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Row(
                          children: [
                            Icon(Icons.person, color: Theme.of(context).colorScheme.primary),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _selectedCustomer!.name,
                                    style: const TextStyle(fontWeight: FontWeight.bold),
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  Text(
                                    _selectedCustomer!.identification,
                                    style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                                  ),
                                ],
                              ),
                            ),
                            IconButton(
                              tooltip: 'Quitar cliente',
                              icon: const Icon(Icons.close, size: 20),
                              onPressed: () => setState(() {
                                _selectedCustomer = null;
                                _showClientError = false;
                              }),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ],
              );
            }
            return const LoadingWidget();
          },
        ),
        const SizedBox(height: 20),
        DropdownButtonFormField<int>(
          decoration: const InputDecoration(
            labelText: 'Método de Pago *',
            prefixIcon: Icon(Icons.payment),
          ),
          initialValue: _idPaymentMethod,
          items: const [
            DropdownMenuItem(value: 1, child: Text('Efectivo')),
            DropdownMenuItem(value: 2, child: Text('Tarjeta')),
            DropdownMenuItem(value: 3, child: Text('Transferencia')),
          ],
          onChanged: (v) => setState(() => _idPaymentMethod = v ?? 1),
        ),
      ],
      ),
    );
  }

  Widget _buildStep2Products() {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return SingleChildScrollView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      padding: EdgeInsets.only(bottom: bottomInset + 16),
      child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '${_items.length} producto(s)',
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
            ),
            FilledButton.tonalIcon(
              onPressed: _showAddProductSheet,
              icon: const Icon(Icons.add),
              label: const Text('Agregar'),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (_items.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                children: [
                  Icon(Icons.inventory_2_outlined, size: 48, color: Theme.of(context).colorScheme.outlineVariant),
                  const SizedBox(height: 12),
                  Text(
                    'No hay productos agregados',
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Toque "Agregar" para seleccionar productos',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          )
        else
          ..._items.asMap().entries.map((entry) => _buildItemCard(entry.key, entry.value)),
        if (_showProductsError && _items.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 8, left: 12),
            child: Text(
              'Agregue al menos un producto',
              style: TextStyle(
                color: Theme.of(context).colorScheme.error,
                fontSize: 12,
              ),
            ),
          ),
        if (_items.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            color: Theme.of(context).colorScheme.secondaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Subtotal parcial', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    app_date_utils.DateUtils.formatCurrency(_totals.totalAmount),
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
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

  Widget _buildStep3Summary() {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return SingleChildScrollView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      padding: EdgeInsets.only(bottom: bottomInset + 16),
      child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Cliente', style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 4),
                Text(
                  _selectedCustomer?.name ?? '—',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  _selectedCustomer?.identification ?? '',
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                ),
                const Divider(height: 24),
                Text('Método de pago', style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(height: 4),
                Text(
                  _paymentMethodLabel(_idPaymentMethod),
                  style: const TextStyle(fontWeight: FontWeight.w500),
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
                  'Productos (${_items.length})',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 8),
                ..._items.map(
                  (item) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            item.product.name,
                            style: const TextStyle(fontSize: 14),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          '${item.quantity} × ${app_date_utils.DateUtils.formatCurrency(item.product.unitPrice)}',
                          style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant, fontSize: 14),
                        ),
                      ],
                    ),
                  ),
                ),
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
      ],
      ),
    );
  }

  String _paymentMethodLabel(int id) {
    switch (id) {
      case 1:
        return 'Efectivo';
      case 2:
        return 'Tarjeta';
      case 3:
        return 'Transferencia';
      default:
        return 'Otro';
    }
  }

  Widget _buildItemCard(int index, InvoiceItem item) {
    final line = InvoiceTaxCalculator.compute([item]);
    return Dismissible(
      key: ValueKey('item_${item.product.id}_$index'),
      direction: DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 16),
        color: Theme.of(context).colorScheme.error,
        child: Icon(Icons.delete, color: Theme.of(context).colorScheme.onError),
      ),
      confirmDismiss: (_) async {
        return await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Eliminar producto'),
            content: Text('¿Eliminar "${item.product.name}" de la factura?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: const Text('Cancelar'),
              ),
              TextButton(
                onPressed: () => Navigator.pop(ctx, true),
                style: TextButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
                child: const Text('Eliminar'),
              ),
            ],
          ),
        ) ?? false;
      },
      onDismissed: (_) => setState(() => _items.removeAt(index)),
      child: Card(
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          leading: CircleAvatar(
            backgroundColor: Theme.of(context).colorScheme.primaryContainer,
            child: Text('${item.quantity}'),
          ),
          title: Text(
            item.product.name,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          subtitle: Text(
            '${item.quantity} × ${app_date_utils.DateUtils.formatCurrency(item.product.unitPrice)}',
          ),
          trailing: Text(
            app_date_utils.DateUtils.formatCurrency(line.totalAmount),
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
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
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
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
                            onPressed: _idCompany == null
                                ? null
                                : () => context.read<ProductBloc>().add(
                                      LoadProductsEvent(idCompany: _idCompany!),
                                    ),
                            child: const Text('Reintentar'),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }
              if (state is ProductError) {
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
                    _showProductsError = false;
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

  void _saveDraft() {
    if (_submitting) return;
    setState(() => _submitting = true);
    FocusScope.of(context).unfocus();
    if (_idCompany == null || _selectedCustomer == null) {
      setState(() => _submitting = false);
      return;
    }
    final inv = Invoice(
      id: 0,
      invoiceNumber: '',
      issueDate: DateTime.now(),
      idClient: _selectedCustomer!.id,
      idCompany: _idCompany!,
      subtotalIva: _totals.subtotalIva,
      subtotalZero: _totals.subtotalZero,
      subtotalIce: _totals.subtotalIce,
      totalAmount: _totals.totalAmount,
      idPaymentMethod: _idPaymentMethod,
    );
    context.read<InvoiceBloc>().add(SaveDraftEvent(inv, List.from(_items)));
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
    if (_selectedCustomer == null || _items.isEmpty || _idCompany == null) {
      setState(() => _submitting = false);
      final msg = _selectedCustomer == null
          ? 'Seleccione un cliente.'
          : _items.isEmpty
              ? 'Agregue al menos un producto.'
              : 'No se pudo determinar la compañía.';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(msg)),
      );
      return;
    }
    final inv = Invoice(
      id: 0,
      invoiceNumber: '',
      issueDate: DateTime.now(),
      idClient: _selectedCustomer!.id,
      idCompany: _idCompany!,
      subtotalIva: _totals.subtotalIva,
      subtotalZero: _totals.subtotalZero,
      subtotalIce: _totals.subtotalIce,
      totalAmount: _totals.totalAmount,
      idPaymentMethod: _idPaymentMethod,
    );
    HapticFeedback.mediumImpact();
    context.read<InvoiceBloc>().add(
          CreateInvoiceEvent(inv, List.from(_items), emission),
        );
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
                    subtitle: Text(app_date_utils.DateUtils.formatCurrency(p.unitPrice)),
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
              prefixIcon: Icon(Icons.numbers),
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
