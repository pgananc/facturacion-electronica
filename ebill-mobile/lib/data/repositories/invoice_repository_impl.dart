import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../core/utils/invoice_api_payload.dart';
import '../../core/utils/invoice_tax_calculator.dart';
import '../../domain/entities/emission_session.dart';
import '../../domain/entities/invoice.dart';
import '../../domain/entities/invoice_item.dart';
import '../../domain/repositories/invoice_repository.dart';
import '../datasources/invoice_remote_datasource.dart';
import 'repository_guard.dart';

class InvoiceRepositoryImpl with RepositoryGuard implements InvoiceRepository {
  final InvoiceRemoteDataSource remoteDataSource;

  InvoiceRepositoryImpl(this.remoteDataSource);

  @override
  Future<Either<Failure, List<Invoice>>> getInvoices({
    int? page,
    int? pageSize,
    int? idCompany,
  }) =>
      guard(() async {
        final invoices = await remoteDataSource.getInvoices(
          page: page,
          pageSize: pageSize,
          idCompany: idCompany,
        );
        return invoices.map((model) => model.toEntity()).toList();
      });

  @override
  Future<Either<Failure, Invoice>> getInvoiceById(int id) =>
      guard(() async {
        final invoice = await remoteDataSource.getInvoiceById(id);
        return invoice.toEntity();
      });

  @override
  Future<Either<Failure, Invoice>> createInvoice({
    required Invoice invoice,
    required List<InvoiceItem> items,
    required EmissionSession emission,
  }) =>
      guard(() async {
        final totals = InvoiceTaxCalculator.compute(items);
        final payload = buildInvoiceCreatePayload(
          idCompany: invoice.idCompany,
          idClient: invoice.idClient,
          issueDate: invoice.issueDate,
          idPaymentMethod: invoice.idPaymentMethod,
          totals: totals,
          emission: emission,
        );
        final created = await remoteDataSource.createInvoice(payload);
        return created.toEntity();
      });

  @override
  Future<Either<Failure, Invoice>> saveDraft({
    required Invoice invoice,
    required List<InvoiceItem> items,
  }) =>
      guard(() async {
        final totals = InvoiceTaxCalculator.compute(items);
        final payload = buildInvoiceDraftPayload(
          idCompany: invoice.idCompany,
          idClient: invoice.idClient,
          issueDate: invoice.issueDate,
          idPaymentMethod: invoice.idPaymentMethod,
          totals: totals,
        );
        final created = await remoteDataSource.saveDraft(payload);
        return created.toEntity();
      });

  @override
  Future<Either<Failure, List<Map<String, dynamic>>>> getDrafts() =>
      guard(() => remoteDataSource.getDrafts());

  @override
  Future<Either<Failure, void>> deleteDraft(int id) =>
      guard(() => remoteDataSource.deleteDraft(id));

  @override
  Future<Either<Failure, List<int>>> downloadPdf(int invoiceId) =>
      guard(() => remoteDataSource.downloadPdfBytes(invoiceId));

  @override
  Future<Either<Failure, List<int>>> downloadXml(int invoiceId) =>
      guard(() => remoteDataSource.downloadXmlBytes(invoiceId));
}
