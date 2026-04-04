import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../domain/entities/product.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/product/product_bloc.dart';
import '../../widgets/loading_widget.dart';

class ProductFormPage extends StatefulWidget {
  final int? productId;

  const ProductFormPage({super.key, this.productId});

  @override
  State<ProductFormPage> createState() => _ProductFormPageState();
}

class _ProductFormPageState extends State<ProductFormPage> {
  final _formKey = GlobalKey<FormState>();
  final _mainCodeController = TextEditingController();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _unitPriceController = TextEditingController();
  bool _hasIce = false;
  bool _hasTourismIva = false;
  bool _isActive = true;
  int? _idTaxType;

  bool _submitting = false;
  bool _submitted = false;
  int? _idCompany;

  bool get isEditing => widget.productId != null;

  bool get _isDirty =>
      _submitting ||
      (!_submitted &&
          (_mainCodeController.text.isNotEmpty ||
              _nameController.text.isNotEmpty ||
              _unitPriceController.text.isNotEmpty ||
              _descriptionController.text.isNotEmpty));

  Future<bool> _confirmDiscard() async {
    if (!_isDirty) return true;
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Descartar cambios'),
        content: const Text(
          '¿Seguro que deseas salir? Se perderán los datos ingresados.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Descartar'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  @override
  void initState() {
    super.initState();
    _resolveCompany();
    if (isEditing) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          context.read<ProductBloc>().add(LoadProductByIdEvent(widget.productId!));
        }
      });
    }
  }

  Future<void> _resolveCompany() async {
    final storage = getIt<StorageService>();
    final authState = context.read<AuthBloc>().state;
    final user = authState is AuthAuthenticated ? authState.user : null;
    final cid = await resolveCompanyId(storage, user);
    if (mounted) setState(() => _idCompany = cid);
  }

  @override
  void dispose() {
    _mainCodeController.dispose();
    _nameController.dispose();
    _descriptionController.dispose();
    _unitPriceController.dispose();
    super.dispose();
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
          title: Text(isEditing ? 'Editar Producto' : 'Nuevo Producto'),
        ),
        body: BlocListener<ProductBloc, ProductState>(
        listener: (context, state) {
          if (!mounted) return;
          if (isEditing && state is ProductDetailLoaded && state.product.id == widget.productId) {
            _loadProductData(state.product);
          }
          if (state is ProductCreated || state is ProductUpdated) {
            Navigator.of(context).pop();
          } else if (state is ProductError && _submitting) {
            _submitted = false;
            setState(() => _submitting = false);
          }
        },
        child: BlocBuilder<ProductBloc, ProductState>(
          builder: (context, state) {
            if (isEditing && state is ProductDetailLoading) {
              return const LoadingWidget();
            }
            if (isEditing &&
                (state is ProductError || state is ProductDetailError)) {
              final message = state is ProductError
                  ? state.message
                  : (state as ProductDetailError).message;
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        message,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                      const SizedBox(height: 16),
                      FilledButton(
                        onPressed: () => context.read<ProductBloc>().add(
                              LoadProductByIdEvent(widget.productId!),
                            ),
                        child: const Text('Reintentar'),
                      ),
                    ],
                  ),
                ),
              );
            }
            return SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _FormSection(
                      title: 'Información del Producto',
                      children: [
                        TextFormField(
                          controller: _mainCodeController,
                          decoration: const InputDecoration(
                            labelText: 'Código Principal *',
                            hintText: 'Código del producto',
                            prefixIcon: Icon(Icons.qr_code),
                          ),
                          keyboardType: TextInputType.text,
                          textInputAction: TextInputAction.next,
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'El código es requerido';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _nameController,
                          decoration: const InputDecoration(
                            labelText: 'Nombre *',
                            hintText: 'Nombre del producto',
                            prefixIcon: Icon(Icons.inventory_2_outlined),
                          ),
                          keyboardType: TextInputType.name,
                          textInputAction: TextInputAction.next,
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'El nombre es requerido';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _unitPriceController,
                          decoration: const InputDecoration(
                            labelText: 'Precio Unitario *',
                            hintText: '0.00',
                            prefixIcon: Icon(Icons.attach_money),
                          ),
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          textInputAction: TextInputAction.next,
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'El precio es requerido';
                            }
                            final price = double.tryParse(value);
                            if (price == null || price < 0) {
                              return 'Ingrese un precio válido';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _descriptionController,
                          decoration: const InputDecoration(
                            labelText: 'Descripción',
                            hintText: 'Descripción del producto',
                            prefixIcon: Icon(Icons.description_outlined),
                          ),
                          textInputAction: TextInputAction.done,
                          maxLines: 3,
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _FormSection(
                      title: 'Configuración',
                      children: [
                        SwitchListTile(
                          title: const Text('Tiene ICE'),
                          value: _hasIce,
                          onChanged: (v) => setState(() => _hasIce = v),
                          contentPadding: EdgeInsets.zero,
                        ),
                        SwitchListTile(
                          title: const Text('Tiene IVA Turismo'),
                          value: _hasTourismIva,
                          onChanged: (v) => setState(() => _hasTourismIva = v),
                          contentPadding: EdgeInsets.zero,
                        ),
                        SwitchListTile(
                          title: const Text('Activo'),
                          value: _isActive,
                          onChanged: (v) => setState(() => _isActive = v),
                          contentPadding: EdgeInsets.zero,
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    SizedBox(
                      height: 52,
                      child: FilledButton(
                        onPressed: _submitting ? null : _submitForm,
                        child: _submitting
                            ? const SizedBox(
                                width: 24,
                                height: 24,
                                child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white),
                              )
                            : const Text('Guardar'),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
          ),
        ),
      ),
    );
  }

  void _loadProductData(Product product) {
    _mainCodeController.text = product.mainCode;
    _nameController.text = product.name;
    _unitPriceController.text = product.unitPrice.toString();
    _descriptionController.text = product.description ?? '';
    _hasIce = product.hasIce;
    _hasTourismIva = product.hasTourismIva;
    _isActive = product.isActive;
    _idTaxType = product.idTaxType;
  }

  void _submitForm() {
    if (_submitting) return;
    FocusScope.of(context).unfocus();
    setState(() => _submitting = true);
    if (_idCompany == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No se pudo determinar la compañía.')),
      );
      setState(() => _submitting = false);
      return;
    }
    if (!_formKey.currentState!.validate()) {
      setState(() => _submitting = false);
      return;
    }
    final price = double.tryParse(_unitPriceController.text.trim());
    if (price == null) {
      setState(() => _submitting = false);
      return;
    }

    _submitted = true;
    final product = Product(
      id: widget.productId ?? 0,
      mainCode: _mainCodeController.text.trim(),
      name: _nameController.text.trim(),
      unitPrice: price,
      description: _descriptionController.text.trim().isEmpty
          ? null
          : _descriptionController.text.trim(),
      hasIce: _hasIce,
      hasTourismIva: _hasTourismIva,
      idTaxType: _idTaxType,
      isActive: _isActive,
    );

    if (isEditing) {
      context.read<ProductBloc>().add(
            UpdateProductEvent(widget.productId!, product, idCompany: _idCompany!),
          );
    } else {
      context.read<ProductBloc>().add(CreateProductEvent(product, idCompany: _idCompany!));
    }
  }
}

class _FormSection extends StatelessWidget {
  const _FormSection({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: Theme.of(context).colorScheme.primary,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            const SizedBox(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }
}

