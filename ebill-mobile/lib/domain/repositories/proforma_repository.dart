import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/emission_session.dart';
import '../entities/invoice_item.dart';
import '../entities/proforma.dart';

abstract class ProformaRepository {
  Future<Either<Failure, List<Proforma>>> getProformas({
    int? page,
    int? pageSize,
    int? idCompany,
    String? status,
  });

  Future<Either<Failure, Proforma>> getProformaById(int id, int idCompany);

  Future<Either<Failure, Proforma>> createProforma({
    required Proforma proforma,
    required List<InvoiceItem> items,
    required EmissionSession emission,
  });

  Future<Either<Failure, int>> convertToInvoice({
    required int proformaId,
    required int idCompany,
  });
}
