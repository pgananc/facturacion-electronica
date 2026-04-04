import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../domain/entities/emission_session.dart';
import '../../../domain/entities/invoice_item.dart';
import '../../../domain/entities/proforma.dart';
import '../../../domain/repositories/proforma_repository.dart';

part 'proforma_event.dart';
part 'proforma_state.dart';

class ProformaBloc extends Bloc<ProformaEvent, ProformaState> {
  final ProformaRepository proformaRepository;

  ProformaBloc(this.proformaRepository) : super(ProformaInitial()) {
    on<LoadProformasEvent>(_onLoadProformas);
    on<LoadProformaByIdEvent>(_onLoadProformaById);
    on<CreateProformaEvent>(_onCreateProforma);
    on<ConvertToInvoiceEvent>(_onConvertToInvoice);
    on<ResetProformaEvent>(
      (event, emit) => emit(ProformaInitial()),
    );
  }

  Future<void> _onLoadProformas(
    LoadProformasEvent event,
    Emitter<ProformaState> emit,
  ) async {
    if (!event.forceRefresh && state is ProformaLoaded) return;
    emit(ProformaLoading());
    final String? apiStatus = switch (event.filter) {
      ProformaListFilter.all => null,
      ProformaListFilter.vigentes => null,
      ProformaListFilter.vencidas => 'EXPIRED',
      ProformaListFilter.convertidas => 'CONVERTED',
    };
    final result = await proformaRepository.getProformas(
      page: event.page,
      pageSize: event.pageSize,
      idCompany: event.idCompany,
      status: apiStatus,
    );
    result.fold(
      (failure) => emit(ProformaListError(failure.message)),
      (list) {
        if (event.filter == ProformaListFilter.vigentes) {
          final filtered = list
              .where(
                (p) => p.status == 'DRAFT' || p.status == 'SENT',
              )
              .toList();
          emit(ProformaLoaded(filtered));
        } else {
          emit(ProformaLoaded(list));
        }
      },
    );
  }

  Future<void> _onLoadProformaById(
    LoadProformaByIdEvent event,
    Emitter<ProformaState> emit,
  ) async {
    emit(ProformaDetailLoading());
    final result =
        await proformaRepository.getProformaById(event.id, event.idCompany);
    result.fold(
      (failure) => emit(ProformaDetailError(failure.message)),
      (proforma) => emit(ProformaDetailLoaded(proforma)),
    );
  }

  Future<void> _onCreateProforma(
    CreateProformaEvent event,
    Emitter<ProformaState> emit,
  ) async {
    emit(ProformaLoading());
    final result = await proformaRepository.createProforma(
      proforma: event.proforma,
      items: event.items,
      emission: event.emission,
    );
    result.fold(
      (failure) => emit(ProformaFormError(failure.message)),
      (proforma) => emit(ProformaCreated(proforma)),
    );
  }

  Future<void> _onConvertToInvoice(
    ConvertToInvoiceEvent event,
    Emitter<ProformaState> emit,
  ) async {
    emit(ProformaLoading());
    final result = await proformaRepository.convertToInvoice(
      proformaId: event.proformaId,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(ProformaFormError(failure.message)),
      (invoiceId) => emit(ProformaConvertedToInvoice(invoiceId)),
    );
  }
}
