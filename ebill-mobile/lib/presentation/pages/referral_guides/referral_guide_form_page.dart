import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../core/di/injection.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/company_context.dart';
import '../../../core/utils/date_utils.dart' as app_date_utils;
import '../../../domain/entities/customer.dart';
import '../../../domain/entities/user.dart';
import '../../bloc/auth/auth_bloc.dart';
import '../../bloc/customer/customer_bloc.dart';
import '../../bloc/referral_guide/referral_guide_bloc.dart';

class ReferralGuideFormPage extends StatefulWidget {
  const ReferralGuideFormPage({super.key});

  @override
  State<ReferralGuideFormPage> createState() => _ReferralGuideFormPageState();
}

class _ReferralGuideFormPageState extends State<ReferralGuideFormPage> {
  bool _submitting = false;
  final _formKey = GlobalKey<FormState>();
  final _originController = TextEditingController();
  final _destinationController = TextEditingController();
  final _carrierNameController = TextEditingController();
  final _carrierRucController = TextEditingController();
  final _licensePlateController = TextEditingController();
  final _referenceInvoiceController = TextEditingController();
  final _referenceDocsController = TextEditingController();

  Customer? _selectedCustomer;
  DateTime _issueDate = DateTime.now();
  DateTime _startDate = DateTime.now();

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
    }
  }

  @override
  void dispose() {
    _originController.dispose();
    _destinationController.dispose();
    _carrierNameController.dispose();
    _carrierRucController.dispose();
    _licensePlateController.dispose();
    _referenceInvoiceController.dispose();
    _referenceDocsController.dispose();
    super.dispose();
  }

  Future<void> _pickIssueDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _issueDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365 * 2)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (!mounted) return;
    if (picked != null) {
      setState(() => _issueDate = picked);
    }
  }

  Future<void> _pickStartDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365 * 2)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (!mounted) return;
    if (picked != null) {
      setState(() => _startDate = picked);
    }
  }

  String _fmtDate(DateTime d) {
    final y = d.year.toString().padLeft(4, '0');
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '$y-$m-$day';
  }

  Future<void> _submit() async {
    if (_submitting) return;
    FocusScope.of(context).unfocus();
    setState(() => _submitting = true);
    if (_idCompany == null) {
      if (mounted) setState(() => _submitting = false);
      if (!mounted) return;
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

    if (!_formKey.currentState!.validate() || _selectedCustomer == null) {
      setState(() => _submitting = false);
      if (_selectedCustomer == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Seleccione un cliente'),
            backgroundColor: Theme.of(context).colorScheme.tertiary,
          ),
        );
      }
      return;
    }

    final emission = getIt<StorageService>().getEmissionSession();
    if (emission == null) {
      setState(() => _submitting = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text(
            'Seleccione un punto de emisión antes de crear la guía.',
          ),
          backgroundColor: Theme.of(context).colorScheme.tertiary,
        ),
      );
      return;
    }

    final payload = <String, dynamic>{
      'idCompany': _idCompany,
      'idClient': _selectedCustomer!.id,
      'idEstablishment': emission.idEstablishment,
      'idIssuancePoint': emission.idInvoiceIssuancePoint,
      'issueDate': _fmtDate(_issueDate),
      'startDate': _fmtDate(_startDate),
      'originAddress': _originController.text.trim(),
      'destinationAddress': _destinationController.text.trim(),
      'carrierName': _carrierNameController.text.trim(),
      'carrierRuc': _carrierRucController.text.trim(),
      'licensePlate': _licensePlateController.text.trim(),
    };

    final refInv = _referenceInvoiceController.text.trim();
    if (refInv.isNotEmpty) {
      payload['referenceInvoiceNumber'] = refInv;
    }
    final refDocs = _referenceDocsController.text.trim();
    if (refDocs.isNotEmpty) {
      payload['referenceDocuments'] = refDocs;
    }

    context.read<ReferralGuideBloc>().add(CreateReferralGuideEvent(payload));
  }

  bool get _isDirty =>
      _submitting ||
      _selectedCustomer != null ||
      _originController.text.isNotEmpty ||
      _destinationController.text.isNotEmpty ||
      _carrierNameController.text.isNotEmpty;

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
        title: const Text('Nueva guía de remisión'),
      ),
      body: BlocListener<ReferralGuideBloc, ReferralGuideState>(
        listener: (context, state) {
          if (state is ReferralGuideCreated) {
            setState(() => _submitting = false);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: const Text('Guía de remisión creada'),
                backgroundColor: Theme.of(context).colorScheme.primary,
              ),
            );
            context.go('/referral-guides');
          } else if (state is ReferralGuideFormError) {
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
          padding: const EdgeInsets.all(16),
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
                        onChanged: (c) => setState(() => _selectedCustomer = c),
                        validator: (v) =>
                            v == null ? 'Seleccione un cliente' : null,
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
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Fecha de emisión *'),
                  subtitle: Text(app_date_utils.DateUtils.formatDate(_issueDate)),
                  trailing: IconButton(
                    icon: const Icon(Icons.calendar_today),
                    tooltip: 'Seleccionar fecha',
                    onPressed: _pickIssueDate,
                  ),
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Inicio de transporte *'),
                  subtitle: Text(app_date_utils.DateUtils.formatDate(_startDate)),
                  trailing: IconButton(
                    icon: const Icon(Icons.calendar_today),
                    tooltip: 'Seleccionar fecha',
                    onPressed: _pickStartDate,
                  ),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _originController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Dirección origen *',
                    prefixIcon: Icon(Icons.location_on_outlined),
                  ),
                  maxLines: 2,
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _destinationController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Dirección destino *',
                    prefixIcon: Icon(Icons.place_outlined),
                  ),
                  maxLines: 2,
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 24),
                const Text(
                  'Transportista',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _carrierNameController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Nombre o razón social *',
                    prefixIcon: Icon(Icons.local_shipping_outlined),
                  ),
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _carrierRucController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'RUC / identificación *',
                    prefixIcon: Icon(Icons.badge_outlined),
                  ),
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _licensePlateController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Placa *',
                    prefixIcon: Icon(Icons.directions_car_outlined),
                  ),
                  validator: (v) =>
                      v == null || v.trim().isEmpty ? 'Requerido' : null,
                ),
                const SizedBox(height: 24),
                const Text(
                  'Documentos de referencia',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _referenceInvoiceController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: 'Nº factura u otro comprobante (opcional)',
                    prefixIcon: Icon(Icons.receipt_long_outlined),
                  ),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _referenceDocsController,
                  textInputAction: TextInputAction.done,
                  decoration: const InputDecoration(
                    labelText: 'Otros documentos / observaciones (opcional)',
                    prefixIcon: Icon(Icons.description_outlined),
                  ),
                  maxLines: 3,
                ),
                const SizedBox(height: 24),
                SizedBox(
                  height: 52,
                  child: FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: _submitting
                        ? SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Theme.of(context).colorScheme.onPrimary,
                            ),
                          )
                        : const Text('Crear guía'),
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
}
