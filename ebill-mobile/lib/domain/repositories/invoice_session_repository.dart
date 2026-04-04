import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/invoice_issuance_point.dart';

abstract class InvoiceSessionRepository {
  Future<Either<Failure, List<InvoiceIssuancePoint>>> getAvailablePoints(
    List<int> establishmentIds,
  );

  Future<Either<Failure, void>> assignPoint(int idInvoiceIssuancePoint);

  Future<Either<Failure, void>> releasePoint();

  Future<Either<Failure, void>> heartbeat();
}
