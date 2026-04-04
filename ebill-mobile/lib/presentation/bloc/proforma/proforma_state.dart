part of 'proforma_bloc.dart';

abstract class ProformaState extends Equatable {
  const ProformaState();

  @override
  List<Object?> get props => [];
}

class ProformaInitial extends ProformaState {}

class ProformaLoading extends ProformaState {}

class ProformaDetailLoading extends ProformaState {}

class ProformaLoaded extends ProformaState {
  final List<Proforma> proformas;

  const ProformaLoaded(this.proformas);

  @override
  List<Object> get props => [proformas];
}

class ProformaDetailLoaded extends ProformaState {
  final Proforma proforma;

  const ProformaDetailLoaded(this.proforma);

  @override
  List<Object> get props => [proforma];
}

class ProformaCreated extends ProformaState {
  final Proforma proforma;

  const ProformaCreated(this.proforma);

  @override
  List<Object> get props => [proforma];
}

class ProformaConvertedToInvoice extends ProformaState {
  final int invoiceId;

  const ProformaConvertedToInvoice(this.invoiceId);

  @override
  List<Object> get props => [invoiceId];
}

class ProformaListError extends ProformaState {
  final String message;

  const ProformaListError(this.message);

  @override
  List<Object?> get props => [message];
}

class ProformaDetailError extends ProformaState {
  final String message;

  const ProformaDetailError(this.message);

  @override
  List<Object?> get props => [message];
}

class ProformaFormError extends ProformaState {
  final String message;

  const ProformaFormError(this.message);

  @override
  List<Object?> get props => [message];
}
