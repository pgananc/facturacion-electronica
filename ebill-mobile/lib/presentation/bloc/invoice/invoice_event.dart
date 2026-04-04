part of 'invoice_bloc.dart';

abstract class InvoiceEvent extends Equatable {
  const InvoiceEvent();

  @override
  List<Object?> get props => [];
}

class LoadInvoicesEvent extends InvoiceEvent {
  final int? page;
  final int? pageSize;
  final int? idCompany;
  final bool forceRefresh;

  const LoadInvoicesEvent({
    this.page,
    this.pageSize,
    this.idCompany,
    this.forceRefresh = false,
  });

  @override
  List<Object?> get props => [page, pageSize, idCompany, forceRefresh];
}

class LoadInvoiceByIdEvent extends InvoiceEvent {
  final int id;

  const LoadInvoiceByIdEvent(this.id);

  @override
  List<Object> get props => [id];
}

class CreateInvoiceEvent extends InvoiceEvent {
  final Invoice invoice;
  final List<InvoiceItem> items;
  final EmissionSession emission;

  const CreateInvoiceEvent(this.invoice, this.items, this.emission);

  @override
  List<Object?> get props => [invoice, items, emission];
}

class SaveDraftEvent extends InvoiceEvent {
  final Invoice invoice;
  final List<InvoiceItem> items;

  const SaveDraftEvent(this.invoice, this.items);

  @override
  List<Object?> get props => [invoice, items];
}

class LoadDraftsEvent extends InvoiceEvent {
  const LoadDraftsEvent();
}

class DeleteDraftEvent extends InvoiceEvent {
  final int id;

  const DeleteDraftEvent(this.id);

  @override
  List<Object> get props => [id];
}

class ResetInvoiceEvent extends InvoiceEvent {
  const ResetInvoiceEvent();

  @override
  List<Object?> get props => [];
}
