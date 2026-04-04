part of 'report_bloc.dart';

abstract class ReportEvent extends Equatable {
  const ReportEvent();

  @override
  List<Object?> get props => [];
}

class LoadSalesSummaryEvent extends ReportEvent {
  const LoadSalesSummaryEvent({
    required this.period,
    required this.idCompany,
    this.from,
    this.to,
    this.forceRefresh = false,
  });

  final String period;
  final int idCompany;
  final DateTime? from;
  final DateTime? to;
  final bool forceRefresh;

  @override
  List<Object?> get props => [period, idCompany, from, to, forceRefresh];
}

class LoadSalesByPeriodEvent extends ReportEvent {
  const LoadSalesByPeriodEvent({
    required this.idCompany,
    required this.dateFrom,
    required this.dateTo,
    this.page = 0,
    this.size = 20,
  });

  final int idCompany;
  final DateTime dateFrom;
  final DateTime dateTo;
  final int page;
  final int size;

  @override
  List<Object?> get props => [idCompany, dateFrom, dateTo, page, size];
}

class LoadSalesByClientEvent extends ReportEvent {
  const LoadSalesByClientEvent({
    required this.idCompany,
    required this.dateFrom,
    required this.dateTo,
    this.page = 0,
    this.size = 20,
  });

  final int idCompany;
  final DateTime dateFrom;
  final DateTime dateTo;
  final int page;
  final int size;

  @override
  List<Object?> get props => [idCompany, dateFrom, dateTo, page, size];
}

class LoadSalesByProductEvent extends ReportEvent {
  const LoadSalesByProductEvent({
    required this.idCompany,
    required this.dateFrom,
    required this.dateTo,
    this.page = 0,
    this.size = 20,
  });

  final int idCompany;
  final DateTime dateFrom;
  final DateTime dateTo;
  final int page;
  final int size;

  @override
  List<Object?> get props => [idCompany, dateFrom, dateTo, page, size];
}

class LoadPendingInvoicesEvent extends ReportEvent {
  const LoadPendingInvoicesEvent({
    required this.idCompany,
    required this.dateFrom,
    required this.dateTo,
    this.statusFilter,
    this.page = 0,
    this.size = 20,
  });

  final int idCompany;
  final DateTime dateFrom;
  final DateTime dateTo;
  final String? statusFilter;
  final int page;
  final int size;

  @override
  List<Object?> get props => [idCompany, dateFrom, dateTo, statusFilter, page, size];
}

class ResetReportEvent extends ReportEvent {
  const ResetReportEvent();

  @override
  List<Object?> get props => [];
}
