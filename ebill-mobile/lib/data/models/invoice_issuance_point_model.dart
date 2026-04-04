import '../../domain/entities/invoice_issuance_point.dart';

class InvoiceIssuancePointModel extends InvoiceIssuancePoint {
  const InvoiceIssuancePointModel({
    required super.idInvoiceIssuancePoint,
    required super.code,
    required super.establishmentId,
    super.sequentialInvoice,
    super.status,
  });

  factory InvoiceIssuancePointModel.fromJson(Map<String, dynamic> json) {
    return InvoiceIssuancePointModel(
      idInvoiceIssuancePoint: (json['idInvoiceIssuancePoint'] as num?)?.toInt() ?? 0,
      code: json['code'] as String? ?? '001',
      establishmentId: (json['establishmentId'] as num?)?.toInt() ?? 0,
      sequentialInvoice: (json['sequentialInvoice'] as num?)?.toInt(),
      status: json['status'] as bool? ?? true,
    );
  }
}
