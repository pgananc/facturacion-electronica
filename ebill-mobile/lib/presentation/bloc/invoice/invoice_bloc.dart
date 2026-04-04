import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../domain/entities/emission_session.dart';
import '../../../domain/entities/invoice.dart';
import '../../../domain/entities/invoice_item.dart';
import '../../../domain/repositories/invoice_repository.dart';

part 'invoice_event.dart';
part 'invoice_state.dart';

class InvoiceBloc extends Bloc<InvoiceEvent, InvoiceState> {
  final InvoiceRepository invoiceRepository;

  InvoiceBloc(this.invoiceRepository) : super(InvoiceInitial()) {
    on<LoadInvoicesEvent>(_onLoadInvoices);
    on<LoadInvoiceByIdEvent>(_onLoadInvoiceById);
    on<CreateInvoiceEvent>(_onCreateInvoice);
    on<SaveDraftEvent>(_onSaveDraft);
    on<LoadDraftsEvent>(_onLoadDrafts);
    on<DeleteDraftEvent>(_onDeleteDraft);
    on<ResetInvoiceEvent>(
      (event, emit) => emit(InvoiceInitial()),
    );
  }

  Future<void> _onLoadInvoices(
    LoadInvoicesEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    if (!event.forceRefresh && state is InvoiceLoaded) return;
    emit(InvoiceLoading());
    final result = await invoiceRepository.getInvoices(
      page: event.page,
      pageSize: event.pageSize,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(InvoiceListError(failure.message)),
      (invoices) => emit(InvoiceLoaded(invoices)),
    );
  }

  Future<void> _onLoadInvoiceById(
    LoadInvoiceByIdEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    emit(InvoiceDetailLoading());
    final result = await invoiceRepository.getInvoiceById(event.id);
    result.fold(
      (failure) => emit(InvoiceDetailError(failure.message)),
      (invoice) => emit(InvoiceDetailLoaded(invoice)),
    );
  }

  Future<void> _onCreateInvoice(
    CreateInvoiceEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    emit(InvoiceLoading());
    final result = await invoiceRepository.createInvoice(
      invoice: event.invoice,
      items: event.items,
      emission: event.emission,
    );
    result.fold(
      (failure) => emit(InvoiceError(failure.message)),
      (invoice) => emit(InvoiceCreated(invoice)),
    );
  }

  Future<void> _onSaveDraft(
    SaveDraftEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    emit(InvoiceLoading());
    final result = await invoiceRepository.saveDraft(
      invoice: event.invoice,
      items: event.items,
    );
    result.fold(
      (failure) => emit(InvoiceError(failure.message)),
      (invoice) => emit(InvoiceDraftSaved(invoice)),
    );
  }

  Future<void> _onLoadDrafts(
    LoadDraftsEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    emit(InvoiceLoading());
    final result = await invoiceRepository.getDrafts();
    result.fold(
      (failure) => emit(InvoiceListError(failure.message)),
      (drafts) => emit(InvoiceDraftsLoaded(drafts)),
    );
  }

  Future<void> _onDeleteDraft(
    DeleteDraftEvent event,
    Emitter<InvoiceState> emit,
  ) async {
    emit(InvoiceLoading());
    final result = await invoiceRepository.deleteDraft(event.id);
    result.fold(
      (failure) => emit(InvoiceError(failure.message)),
      (_) => emit(DraftDeleted(event.id)),
    );
  }
}
