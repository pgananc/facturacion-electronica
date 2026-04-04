part of 'proforma_bloc.dart';

enum ProformaListFilter { all, vigentes, vencidas, convertidas }

abstract class ProformaEvent extends Equatable {
  const ProformaEvent();

  @override
  List<Object?> get props => [];
}

class LoadProformasEvent extends ProformaEvent {
  final int? page;
  final int? pageSize;
  final int? idCompany;
  final ProformaListFilter filter;
  final bool forceRefresh;

  const LoadProformasEvent({
    this.page,
    this.pageSize,
    this.idCompany,
    this.filter = ProformaListFilter.all,
    this.forceRefresh = false,
  });

  @override
  List<Object?> get props => [page, pageSize, idCompany, filter, forceRefresh];
}

class LoadProformaByIdEvent extends ProformaEvent {
  final int id;
  final int idCompany;

  const LoadProformaByIdEvent(this.id, this.idCompany);

  @override
  List<Object> get props => [id, idCompany];
}

class CreateProformaEvent extends ProformaEvent {
  final Proforma proforma;
  final List<InvoiceItem> items;
  final EmissionSession emission;

  const CreateProformaEvent(this.proforma, this.items, this.emission);

  @override
  List<Object?> get props => [proforma, items, emission];
}

class ConvertToInvoiceEvent extends ProformaEvent {
  final int proformaId;
  final int idCompany;

  const ConvertToInvoiceEvent({
    required this.proformaId,
    required this.idCompany,
  });

  @override
  List<Object> get props => [proformaId, idCompany];
}

class ResetProformaEvent extends ProformaEvent {
  const ResetProformaEvent();

  @override
  List<Object?> get props => [];
}
