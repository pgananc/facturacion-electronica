import 'package:equatable/equatable.dart';

/// Punto de emisión activo (persistido localmente tras asignación en auth MS).
class EmissionSession extends Equatable {
  const EmissionSession({
    required this.idInvoiceIssuancePoint,
    required this.idEstablishment,
    required this.issuancePointCode,
    this.establishmentCode = '001',
  });

  final int idInvoiceIssuancePoint;
  final int idEstablishment;
  final String issuancePointCode;
  final String establishmentCode;

  Map<String, dynamic> toJson() => {
        'idInvoiceIssuancePoint': idInvoiceIssuancePoint,
        'idEstablishment': idEstablishment,
        'issuancePointCode': issuancePointCode,
        'establishmentCode': establishmentCode,
      };

  factory EmissionSession.fromJson(Map<String, dynamic> json) {
    return EmissionSession(
      idInvoiceIssuancePoint: (json['idInvoiceIssuancePoint'] as num?)?.toInt() ?? 0,
      idEstablishment: (json['idEstablishment'] as num?)?.toInt() ?? 0,
      issuancePointCode: json['issuancePointCode'] as String? ?? '001',
      establishmentCode: json['establishmentCode'] as String? ?? '001',
    );
  }

  @override
  List<Object?> get props =>
      [idInvoiceIssuancePoint, idEstablishment, issuancePointCode, establishmentCode];
}
