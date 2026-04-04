part of 'report_bloc.dart';

abstract class ReportState extends Equatable {
  const ReportState();

  @override
  List<Object?> get props => [];
}

class ReportInitial extends ReportState {}

class ReportLoading extends ReportState {}

class ReportLoaded extends ReportState {
  const ReportLoaded(this.summary);

  final SalesSummary summary;

  @override
  List<Object?> get props => [summary];
}

class ReportError extends ReportState {
  const ReportError(this.message, {this.httpStatus});

  final String message;

  /// Set when [ServerFailure.statusCode] is present (debug + prod UI).
  final int? httpStatus;

  @override
  List<Object?> get props => [message, httpStatus];
}

class SalesByPeriodLoading extends ReportState {}

class SalesByPeriodLoaded extends ReportState {
  const SalesByPeriodLoaded(this.report);

  final SalesByPeriodReport report;

  @override
  List<Object?> get props => [report];
}

class SalesByPeriodError extends ReportState {
  const SalesByPeriodError(this.message);

  final String message;

  @override
  List<Object?> get props => [message];
}

class SalesByClientLoading extends ReportState {}

class SalesByClientLoaded extends ReportState {
  const SalesByClientLoaded(this.report);

  final SalesByClientReport report;

  @override
  List<Object?> get props => [report];
}

class SalesByClientError extends ReportState {
  const SalesByClientError(this.message);

  final String message;

  @override
  List<Object?> get props => [message];
}

class SalesByProductLoading extends ReportState {}

class SalesByProductLoaded extends ReportState {
  const SalesByProductLoaded(this.report);

  final SalesByProductReport report;

  @override
  List<Object?> get props => [report];
}

class SalesByProductError extends ReportState {
  const SalesByProductError(this.message);

  final String message;

  @override
  List<Object?> get props => [message];
}

class PendingInvoicesLoading extends ReportState {}

class PendingInvoicesLoaded extends ReportState {
  const PendingInvoicesLoaded(this.report);

  final PendingInvoicesReport report;

  @override
  List<Object?> get props => [report];
}

class PendingInvoicesError extends ReportState {
  const PendingInvoicesError(this.message);

  final String message;

  @override
  List<Object?> get props => [message];
}
