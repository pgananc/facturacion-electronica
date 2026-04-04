import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/referral_guide.dart';

abstract class ReferralGuideRepository {
  Future<Either<Failure, List<ReferralGuide>>> getReferralGuides({
    int? page,
    int? pageSize,
    int? idCompany,
  });

  Future<Either<Failure, ReferralGuide>> getReferralGuideById(
    int id, {
    int? idCompany,
  });

  Future<Either<Failure, ReferralGuide>> createReferralGuide(
    Map<String, dynamic> data,
  );

  Future<Either<Failure, List<int>>> downloadPdf(int id);

  Future<Either<Failure, List<int>>> downloadXml(int id);
}
