import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/entities/invoice_issuance_point.dart';
import '../../domain/repositories/invoice_session_repository.dart';
import '../datasources/invoice_session_remote_datasource.dart';
import 'repository_guard.dart';

class InvoiceSessionRepositoryImpl
    with RepositoryGuard
    implements InvoiceSessionRepository {
  InvoiceSessionRepositoryImpl(this._dataSource);

  final InvoiceSessionRemoteDataSource _dataSource;

  @override
  Future<Either<Failure, List<InvoiceIssuancePoint>>> getAvailablePoints(
    List<int> establishmentIds,
  ) =>
      guard(() => _dataSource.getAvailablePoints(establishmentIds));

  @override
  Future<Either<Failure, void>> assignPoint(int idInvoiceIssuancePoint) =>
      guard(() => _dataSource.assignPoint(idInvoiceIssuancePoint));

  @override
  Future<Either<Failure, void>> releasePoint() =>
      guard(() => _dataSource.releasePoint());

  @override
  Future<Either<Failure, void>> heartbeat() =>
      guard(() => _dataSource.heartbeat());
}
