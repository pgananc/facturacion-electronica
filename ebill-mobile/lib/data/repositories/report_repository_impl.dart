import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/entities/pending_invoice.dart';
import '../../domain/entities/sales_by_client.dart';
import '../../domain/entities/sales_by_period.dart';
import '../../domain/entities/sales_by_product.dart';
import '../../domain/entities/sales_summary.dart';
import '../../domain/repositories/report_repository.dart';
import '../datasources/report_remote_datasource.dart';
import 'repository_guard.dart';

class ReportRepositoryImpl with RepositoryGuard implements ReportRepository {
  ReportRepositoryImpl(this.remoteDataSource);

  final ReportRemoteDataSource remoteDataSource;

  static String _formatDate(DateTime d) {
    final y = d.year.toString().padLeft(4, '0');
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '$y-$m-$day';
  }

  @override
  Future<Either<Failure, SalesSummary>> getSalesSummary({
    required int idCompany,
    required String period,
    DateTime? from,
    DateTime? to,
  }) =>
      guard(() async {
        final summary = await remoteDataSource.getSalesSummary(
          idCompany: idCompany,
          period: period,
          from: from != null ? _formatDate(from) : null,
          to: to != null ? _formatDate(to) : null,
        );
        return summary.toEntity();
      });

  @override
  Future<Either<Failure, SalesByPeriodReport>> getSalesByPeriod({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  }) =>
      guard(() async {
        final result = await remoteDataSource.getSalesByPeriod(
          idCompany: idCompany,
          dateFrom: _formatDate(dateFrom),
          dateTo: _formatDate(dateTo),
          page: page,
          size: size,
        );
        return result.toEntity();
      });

  @override
  Future<Either<Failure, SalesByClientReport>> getSalesByClient({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  }) =>
      guard(() async {
        final result = await remoteDataSource.getSalesByClient(
          idCompany: idCompany,
          dateFrom: _formatDate(dateFrom),
          dateTo: _formatDate(dateTo),
          page: page,
          size: size,
        );
        return result.toEntity();
      });

  @override
  Future<Either<Failure, SalesByProductReport>> getSalesByProduct({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  }) =>
      guard(() async {
        final result = await remoteDataSource.getSalesByProduct(
          idCompany: idCompany,
          dateFrom: _formatDate(dateFrom),
          dateTo: _formatDate(dateTo),
          page: page,
          size: size,
        );
        return result.toEntity();
      });

  @override
  Future<Either<Failure, PendingInvoicesReport>> getPendingInvoices({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    String? statusFilter,
    int page = 0,
    int size = 20,
  }) =>
      guard(() async {
        final result = await remoteDataSource.getPendingInvoices(
          idCompany: idCompany,
          dateFrom: _formatDate(dateFrom),
          dateTo: _formatDate(dateTo),
          statusFilter: statusFilter,
          page: page,
          size: size,
        );
        return result.toEntity();
      });
}
