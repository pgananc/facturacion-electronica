import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/pending_invoice.dart';
import '../entities/sales_by_client.dart';
import '../entities/sales_by_period.dart';
import '../entities/sales_by_product.dart';
import '../entities/sales_summary.dart';

abstract class ReportRepository {
  Future<Either<Failure, SalesSummary>> getSalesSummary({
    required int idCompany,
    required String period,
    DateTime? from,
    DateTime? to,
  });

  Future<Either<Failure, SalesByPeriodReport>> getSalesByPeriod({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  });

  Future<Either<Failure, SalesByClientReport>> getSalesByClient({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  });

  Future<Either<Failure, SalesByProductReport>> getSalesByProduct({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    int page = 0,
    int size = 20,
  });

  Future<Either<Failure, PendingInvoicesReport>> getPendingInvoices({
    required int idCompany,
    required DateTime dateFrom,
    required DateTime dateTo,
    String? statusFilter,
    int page = 0,
    int size = 20,
  });
}
