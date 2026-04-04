part of 'credit_note_bloc.dart';

abstract class CreditNoteState extends Equatable {
  const CreditNoteState();

  @override
  List<Object> get props => [];
}

class CreditNoteInitial extends CreditNoteState {}

class CreditNoteLoading extends CreditNoteState {}

class CreditNoteDetailLoading extends CreditNoteState {}

class CreditNoteLoaded extends CreditNoteState {
  const CreditNoteLoaded(this.creditNotes);

  final List<CreditNote> creditNotes;

  @override
  List<Object> get props => [creditNotes];
}

class CreditNoteDetailLoaded extends CreditNoteState {
  const CreditNoteDetailLoaded(this.creditNote);

  final CreditNote creditNote;

  @override
  List<Object> get props => [creditNote];
}

class CreditNoteCreated extends CreditNoteState {
  const CreditNoteCreated(this.creditNote);

  final CreditNote creditNote;

  @override
  List<Object> get props => [creditNote];
}

class CreditNoteError extends CreditNoteState {
  const CreditNoteError(this.message);

  final String message;

  @override
  List<Object> get props => [message];
}

class CreditNoteListError extends CreditNoteState {
  const CreditNoteListError(this.message);

  final String message;

  @override
  List<Object> get props => [message];
}

class CreditNoteDetailError extends CreditNoteState {
  const CreditNoteDetailError(this.message);

  final String message;

  @override
  List<Object> get props => [message];
}
