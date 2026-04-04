import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/repositories/company_repository.dart';
import '../datasources/company_remote_datasource.dart';
import 'repository_guard.dart';

class CompanyRepositoryImpl with RepositoryGuard implements CompanyRepository {
  CompanyRepositoryImpl(this._dataSource);

  final CompanyRemoteDataSource _dataSource;

  @override
  Future<Either<Failure, String>> getCompanyDisplayName(int idCompany) =>
      guard(() => _dataSource.getCompanyDisplayName(idCompany));

  @override
  Future<Either<Failure, String?>> getCompanyIdentification(int idCompany) =>
      guard(() => _dataSource.getCompanyIdentification(idCompany));
}
