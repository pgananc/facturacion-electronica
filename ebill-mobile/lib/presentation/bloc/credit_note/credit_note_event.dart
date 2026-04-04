part of 'credit_note_bloc.dart';

abstract class CreditNoteEvent extends Equatable {
  const CreditNoteEvent();

  @override
  List<Object?> get props => [];
}

class LoadCreditNotesEvent extends CreditNoteEvent {
  const LoadCreditNotesEvent({
    this.page = 0,
    this.pageSize = 20,
    required this.idCompany,
    this.forceRefresh = false,
  });

  final int page;
  final int pageSize;
  final int idCompany;
  final bool forceRefresh;

  @override
  List<Object?> get props => [page, pageSize, idCompany, forceRefresh];
}

class LoadCreditNoteByIdEvent extends CreditNoteEvent {
  const LoadCreditNoteByIdEvent(this.id);

  final int id;

  @override
  List<Object> get props => [id];
}

class CreateCreditNoteEvent extends CreditNoteEvent {
  const CreateCreditNoteEvent(this.creditNote, this.items);

  final CreditNote creditNote;
  final List<Map<String, dynamic>> items;

  @override
  List<Object?> get props => [creditNote, items];
}

class ResetCreditNoteEvent extends CreditNoteEvent {
  const ResetCreditNoteEvent();

  @override
  List<Object?> get props => [];
}
