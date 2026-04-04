import 'package:equatable/equatable.dart';

class ReferralGuide extends Equatable {
  final int id;
  final String guideNumber;
  final DateTime issueDate;
  final int idCompany;
  final int idClient;
  final DateTime startDate;
  final String originAddress;
  final String destinationAddress;
  final String carrierName;
  final String carrierRuc;
  final String licensePlate;
  final String? status;
  final String? authorizationNumber;
  final DateTime? authorizationDate;
  final String? clientName;

  const ReferralGuide({
    required this.id,
    required this.guideNumber,
    required this.issueDate,
    required this.idCompany,
    required this.idClient,
    required this.startDate,
    required this.originAddress,
    required this.destinationAddress,
    required this.carrierName,
    required this.carrierRuc,
    required this.licensePlate,
    this.status,
    this.authorizationNumber,
    this.authorizationDate,
    this.clientName,
  });

  @override
  List<Object?> get props => [
        id,
        guideNumber,
        issueDate,
        idCompany,
        idClient,
        startDate,
        originAddress,
        destinationAddress,
        carrierName,
        carrierRuc,
        licensePlate,
        status,
        authorizationNumber,
        authorizationDate,
        clientName,
      ];
}
