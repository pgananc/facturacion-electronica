import 'package:equatable/equatable.dart';

/// Company (+ establishment ids) as embedded in the JWT `companyEstablishments` claim.
class CompanyEstablishment extends Equatable {
  final int idCompany;
  final List<int> establishmentIds;

  const CompanyEstablishment({
    required this.idCompany,
    required this.establishmentIds,
  });

  @override
  List<Object?> get props => [idCompany, establishmentIds];
}
