import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../domain/entities/credit_note.dart';
import '../../../domain/repositories/credit_note_repository.dart';

part 'credit_note_event.dart';
part 'credit_note_state.dart';

class CreditNoteBloc extends Bloc<CreditNoteEvent, CreditNoteState> {
  final CreditNoteRepository creditNoteRepository;

  CreditNoteBloc(this.creditNoteRepository) : super(CreditNoteInitial()) {
    on<LoadCreditNotesEvent>(_onLoadCreditNotes);
    on<LoadCreditNoteByIdEvent>(_onLoadCreditNoteById);
    on<CreateCreditNoteEvent>(_onCreateCreditNote);
    on<ResetCreditNoteEvent>(
      (event, emit) => emit(CreditNoteInitial()),
    );
  }

  Future<void> _onLoadCreditNotes(
    LoadCreditNotesEvent event,
    Emitter<CreditNoteState> emit,
  ) async {
    if (!event.forceRefresh && state is CreditNoteLoaded) return;
    emit(CreditNoteLoading());
    final result = await creditNoteRepository.getCreditNotes(
      page: event.page,
      pageSize: event.pageSize,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(CreditNoteListError(failure.message)),
      (creditNotes) => emit(CreditNoteLoaded(creditNotes)),
    );
  }

  Future<void> _onLoadCreditNoteById(
    LoadCreditNoteByIdEvent event,
    Emitter<CreditNoteState> emit,
  ) async {
    emit(CreditNoteDetailLoading());
    final result = await creditNoteRepository.getCreditNoteById(event.id);
    result.fold(
      (failure) => emit(CreditNoteDetailError(failure.message)),
      (creditNote) => emit(CreditNoteDetailLoaded(creditNote)),
    );
  }

  Future<void> _onCreateCreditNote(
    CreateCreditNoteEvent event,
    Emitter<CreditNoteState> emit,
  ) async {
    emit(CreditNoteLoading());
    final result = await creditNoteRepository.createCreditNote(
      creditNote: event.creditNote,
      items: event.items,
    );
    result.fold(
      (failure) => emit(CreditNoteError(failure.message)),
      (creditNote) => emit(CreditNoteCreated(creditNote)),
    );
  }
}
