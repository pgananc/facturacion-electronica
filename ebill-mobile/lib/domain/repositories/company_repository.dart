import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';

abstract class CompanyRepository {
  Future<Either<Failure, String>> getCompanyDisplayName(int idCompany);
  Future<Either<Failure, String?>> getCompanyIdentification(int idCompany);
}
