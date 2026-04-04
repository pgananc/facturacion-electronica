import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/entities/referral_guide.dart';
import '../../domain/repositories/referral_guide_repository.dart';
import '../datasources/referral_guide_remote_datasource.dart';
import 'repository_guard.dart';

class ReferralGuideRepositoryImpl
    with RepositoryGuard
    implements ReferralGuideRepository {
  ReferralGuideRepositoryImpl(this.remoteDataSource);

  final ReferralGuideRemoteDataSource remoteDataSource;

  @override
  Future<Either<Failure, List<ReferralGuide>>> getReferralGuides({
    int? page,
    int? pageSize,
    int? idCompany,
  }) =>
      guard(() async {
        final list = await remoteDataSource.getReferralGuides(
          page: page,
          pageSize: pageSize,
          idCompany: idCompany,
        );
        return list.map((m) => m.toEntity()).toList();
      });

  @override
  Future<Either<Failure, ReferralGuide>> getReferralGuideById(
    int id, {
    int? idCompany,
  }) =>
      guard(() async {
        final model = await remoteDataSource.getReferralGuideById(
          id,
          idCompany: idCompany,
        );
        return model.toEntity();
      });

  @override
  Future<Either<Failure, ReferralGuide>> createReferralGuide(
    Map<String, dynamic> data,
  ) =>
      guard(() async {
        final created = await remoteDataSource.createReferralGuide(data);
        return created.toEntity();
      });

  @override
  Future<Either<Failure, List<int>>> downloadPdf(int id) =>
      guard(() => remoteDataSource.downloadPdfBytes(id));

  @override
  Future<Either<Failure, List<int>>> downloadXml(int id) =>
      guard(() => remoteDataSource.downloadXmlBytes(id));
}
