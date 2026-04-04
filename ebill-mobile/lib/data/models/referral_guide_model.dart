import 'package:flutter/foundation.dart';

import '../../domain/entities/referral_guide.dart';

class ReferralGuideModel extends ReferralGuide {
  const ReferralGuideModel({
    required super.id,
    required super.guideNumber,
    required super.issueDate,
    required super.idCompany,
    required super.idClient,
    required super.startDate,
    required super.originAddress,
    required super.destinationAddress,
    required super.carrierName,
    required super.carrierRuc,
    required super.licensePlate,
    super.status,
    super.authorizationNumber,
    super.authorizationDate,
    super.clientName,
  });

  static int? _safeInt(dynamic value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static DateTime _parseDate(dynamic v) {
    if (v == null) {
      if (kDebugMode) debugPrint('ReferralGuideModel._parseDate: null date value');
      return DateTime.utc(1970);
    }
    if (v is String) {
      if (v.length >= 10) {
        return DateTime.tryParse(v.substring(0, 10)) ?? DateTime.tryParse(v) ?? DateTime.utc(1970);
      }
      return DateTime.tryParse(v) ?? DateTime.utc(1970);
    }
    if (kDebugMode) debugPrint('ReferralGuideModel._parseDate: unexpected type ${v.runtimeType}');
    return DateTime.utc(1970);
  }

  static DateTime? _parseDateNullable(dynamic v) {
    if (v == null) return null;
    if (v is String) {
      if (v.length >= 10) {
        return DateTime.tryParse(v.substring(0, 10));
      }
      return DateTime.tryParse(v);
    }
    return null;
  }

  factory ReferralGuideModel.fromJson(Map<String, dynamic> json) {
    final idRaw = json['idReferralGuide'] ?? json['id'];
    final id = idRaw is num ? idRaw.toInt() : int.tryParse('$idRaw') ?? 0;

    String? clientName = json['clientName'] as String?;
    final clientDto = json['clientDto'];
    if (clientName == null && clientDto is Map<String, dynamic>) {
      final n = clientDto['name'] ?? clientDto['businessName'];
      if (n != null) clientName = n.toString();
    }

    return ReferralGuideModel(
      id: id,
      guideNumber: json['guideNumber'] as String? ??
          json['referralGuideNumber'] as String? ??
          '',
      issueDate: _parseDate(json['issueDate']),
      idCompany: _safeInt(json['idCompany']) ?? 0,
      idClient: _safeInt(json['idClient']) ?? 0,
      startDate: _parseDate(json['startDate'] ?? json['transportStartDate']),
      originAddress: json['originAddress'] as String? ?? '',
      destinationAddress: json['destinationAddress'] as String? ?? '',
      carrierName: json['carrierName'] as String? ?? '',
      carrierRuc: json['carrierRuc'] as String? ?? json['carrierIdentification'] as String? ?? '',
      licensePlate: json['licensePlate'] as String? ?? json['vehiclePlate'] as String? ?? '',
      status: json['documentStatus'] as String? ?? json['status'] as String?,
      authorizationNumber: json['authorizationNumber'] as String?,
      authorizationDate: _parseDateNullable(
        json['authorizationDate'] ?? json['authDate'],
      ),
      clientName: clientName,
    );
  }

  ReferralGuide toEntity() => this;
}
