part of 'invoice_bloc.dart';

abstract class InvoiceState extends Equatable {
  const InvoiceState();

  @override
  List<Object> get props => [];
}

class InvoiceInitial extends InvoiceState {}

class InvoiceLoading extends InvoiceState {}

class InvoiceDetailLoading extends InvoiceState {}

class InvoiceLoaded extends InvoiceState {
  final List<Invoice> invoices;

  const InvoiceLoaded(this.invoices);

  @override
  List<Object> get props => [invoices];
}

class InvoiceDetailLoaded extends InvoiceState {
  final Invoice invoice;

  const InvoiceDetailLoaded(this.invoice);

  @override
  List<Object> get props => [invoice];
}

class InvoiceCreated extends InvoiceState {
  final Invoice invoice;

  const InvoiceCreated(this.invoice);

  @override
  List<Object> get props => [invoice];
}

class InvoiceError extends InvoiceState {
  final String message;

  const InvoiceError(this.message);

  @override
  List<Object> get props => [message];
}

class InvoiceListError extends InvoiceState {
  final String message;

  const InvoiceListError(this.message);

  @override
  List<Object> get props => [message];
}

class InvoiceDetailError extends InvoiceState {
  final String message;

  const InvoiceDetailError(this.message);

  @override
  List<Object> get props => [message];
}

class InvoiceDraftsLoaded extends InvoiceState {
  final List<Map<String, dynamic>> drafts;

  const InvoiceDraftsLoaded(this.drafts);

  @override
  List<Object> get props => [drafts];
}

class InvoiceDraftSaved extends InvoiceState {
  final Invoice invoice;

  const InvoiceDraftSaved(this.invoice);

  @override
  List<Object> get props => [invoice];
}

class DraftDeleted extends InvoiceState {
  final int deletedDraftId;

  const DraftDeleted(this.deletedDraftId);

  @override
  List<Object> get props => [deletedDraftId];
}

