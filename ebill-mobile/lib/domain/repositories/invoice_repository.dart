import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/emission_session.dart';
import '../entities/invoice.dart';
import '../entities/invoice_item.dart';

abstract class InvoiceRepository {
  Future<Either<Failure, List<Invoice>>> getInvoices({
    int? page,
    int? pageSize,
    int? idCompany,
  });

  Future<Either<Failure, Invoice>> getInvoiceById(int id);

  Future<Either<Failure, Invoice>> createInvoice({
    required Invoice invoice,
    required List<InvoiceItem> items,
    required EmissionSession emission,
  });

  Future<Either<Failure, Invoice>> saveDraft({
    required Invoice invoice,
    required List<InvoiceItem> items,
  });

  Future<Either<Failure, List<Map<String, dynamic>>>> getDrafts();

  Future<Either<Failure, void>> deleteDraft(int id);

  Future<Either<Failure, List<int>>> downloadPdf(int invoiceId);

  Future<Either<Failure, List<int>>> downloadXml(int invoiceId);
}
