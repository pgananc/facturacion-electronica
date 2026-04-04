import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../core/utils/invoice_tax_calculator.dart';
import '../../domain/entities/emission_session.dart';
import '../../domain/entities/invoice_item.dart';
import '../../domain/entities/proforma.dart';
import '../../domain/repositories/proforma_repository.dart';
import '../datasources/proforma_remote_datasource.dart';
import 'repository_guard.dart';

class ProformaRepositoryImpl
    with RepositoryGuard
    implements ProformaRepository {
  ProformaRepositoryImpl(this.remoteDataSource);

  final ProformaRemoteDataSource remoteDataSource;

  @override
  Future<Either<Failure, List<Proforma>>> getProformas({
    int? page,
    int? pageSize,
    int? idCompany,
    String? status,
  }) =>
      guard(() async {
        final list = await remoteDataSource.getProformas(
          page: page,
          pageSize: pageSize,
          idCompany: idCompany,
          status: status,
        );
        return list.map((m) => m.toEntity()).toList();
      });

  @override
  Future<Either<Failure, Proforma>> getProformaById(
    int id,
    int idCompany,
  ) =>
      guard(() async {
        final model = await remoteDataSource.getProformaById(id, idCompany);
        return model.toEntity();
      });

  @override
  Future<Either<Failure, Proforma>> createProforma({
    required Proforma proforma,
    required List<InvoiceItem> items,
    required EmissionSession emission,
  }) =>
      guard(() async {
        final details =
            InvoiceTaxCalculator.buildProformaDetailPayloads(items);
        final y = proforma.issueDate.year.toString().padLeft(4, '0');
        final m = proforma.issueDate.month.toString().padLeft(2, '0');
        final d = proforma.issueDate.day.toString().padLeft(2, '0');
        final payload = <String, dynamic>{
          'idCompany': proforma.idCompany,
          'idEstablishment': emission.idEstablishment,
          'idIssuancePoint': emission.idInvoiceIssuancePoint,
          'idClient': proforma.idClient,
          'issueDate': '$y-$m-$d',
          'details': details,
        };
        if (proforma.expiryDate != null) {
          final ey = proforma.expiryDate!.year.toString().padLeft(4, '0');
          final em = proforma.expiryDate!.month.toString().padLeft(2, '0');
          final ed = proforma.expiryDate!.day.toString().padLeft(2, '0');
          payload['validUntil'] = '$ey-$em-$ed';
        }
        final created = await remoteDataSource.createProforma(payload);
        return created.toEntity();
      });

  @override
  Future<Either<Failure, int>> convertToInvoice({
    required int proformaId,
    required int idCompany,
  }) =>
      guard(
        () => remoteDataSource.convertToInvoice(proformaId, idCompany),
      );
}
