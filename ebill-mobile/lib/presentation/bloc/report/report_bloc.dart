import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../core/error/failures.dart';
import '../../../domain/entities/pending_invoice.dart';
import '../../../domain/entities/sales_by_client.dart';
import '../../../domain/entities/sales_by_period.dart';
import '../../../domain/entities/sales_by_product.dart';
import '../../../domain/entities/sales_summary.dart';
import '../../../domain/repositories/report_repository.dart';

part 'report_event.dart';
part 'report_state.dart';

class ReportBloc extends Bloc<ReportEvent, ReportState> {
  final ReportRepository reportRepository;

  ReportBloc(this.reportRepository) : super(ReportInitial()) {
    on<LoadSalesSummaryEvent>(_onLoadSalesSummary);
    on<LoadSalesByPeriodEvent>(_onLoadSalesByPeriod);
    on<LoadSalesByClientEvent>(_onLoadSalesByClient);
    on<LoadSalesByProductEvent>(_onLoadSalesByProduct);
    on<LoadPendingInvoicesEvent>(_onLoadPendingInvoices);
    on<ResetReportEvent>(
      (event, emit) => emit(ReportInitial()),
    );
  }

  Future<void> _onLoadSalesSummary(
    LoadSalesSummaryEvent event,
    Emitter<ReportState> emit,
  ) async {
    if (!event.forceRefresh && state is ReportLoaded) return;
    emit(ReportLoading());
    final result = await reportRepository.getSalesSummary(
      idCompany: event.idCompany,
      period: event.period,
      from: event.from,
      to: event.to,
    );
    result.fold(
      (failure) => emit(
        ReportError(
          failure.message,
          httpStatus:
              failure is ServerFailure ? failure.statusCode : null,
        ),
      ),
      (summary) => emit(ReportLoaded(summary)),
    );
  }

  Future<void> _onLoadSalesByPeriod(
    LoadSalesByPeriodEvent event,
    Emitter<ReportState> emit,
  ) async {
    emit(SalesByPeriodLoading());
    final result = await reportRepository.getSalesByPeriod(
      idCompany: event.idCompany,
      dateFrom: event.dateFrom,
      dateTo: event.dateTo,
      page: event.page,
      size: event.size,
    );
    result.fold(
      (failure) => emit(SalesByPeriodError(failure.message)),
      (report) => emit(SalesByPeriodLoaded(report)),
    );
  }

  Future<void> _onLoadSalesByClient(
    LoadSalesByClientEvent event,
    Emitter<ReportState> emit,
  ) async {
    emit(SalesByClientLoading());
    final result = await reportRepository.getSalesByClient(
      idCompany: event.idCompany,
      dateFrom: event.dateFrom,
      dateTo: event.dateTo,
      page: event.page,
      size: event.size,
    );
    result.fold(
      (failure) => emit(SalesByClientError(failure.message)),
      (report) => emit(SalesByClientLoaded(report)),
    );
  }

  Future<void> _onLoadSalesByProduct(
    LoadSalesByProductEvent event,
    Emitter<ReportState> emit,
  ) async {
    emit(SalesByProductLoading());
    final result = await reportRepository.getSalesByProduct(
      idCompany: event.idCompany,
      dateFrom: event.dateFrom,
      dateTo: event.dateTo,
      page: event.page,
      size: event.size,
    );
    result.fold(
      (failure) => emit(SalesByProductError(failure.message)),
      (report) => emit(SalesByProductLoaded(report)),
    );
  }

  Future<void> _onLoadPendingInvoices(
    LoadPendingInvoicesEvent event,
    Emitter<ReportState> emit,
  ) async {
    emit(PendingInvoicesLoading());
    final result = await reportRepository.getPendingInvoices(
      idCompany: event.idCompany,
      dateFrom: event.dateFrom,
      dateTo: event.dateTo,
      statusFilter: event.statusFilter,
      page: event.page,
      size: event.size,
    );
    result.fold(
      (failure) => emit(PendingInvoicesError(failure.message)),
      (report) => emit(PendingInvoicesLoaded(report)),
    );
  }
}
