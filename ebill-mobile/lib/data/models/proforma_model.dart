import 'package:flutter/foundation.dart';

import '../../domain/entities/proforma.dart';

class ProformaModel extends Proforma {
  const ProformaModel({
    required super.id,
    required super.proformaNumber,
    required super.issueDate,
    super.expiryDate,
    required super.idClient,
    required super.idCompany,
    required super.subtotalIva,
    required super.subtotalZero,
    required super.totalAmount,
    required super.idPaymentMethod,
    super.status,
    super.convertedInvoiceId,
    super.clientName,
  });

  static DateTime _parseDate(dynamic v) {
    if (v == null) {
      if (kDebugMode) debugPrint('ProformaModel._parseDate: null date value');
      return DateTime.utc(1970);
    }
    if (v is String) {
      if (v.length >= 10) {
        return DateTime.tryParse(v.substring(0, 10)) ?? DateTime.tryParse(v) ?? DateTime.utc(1970);
      }
      return DateTime.tryParse(v) ?? DateTime.utc(1970);
    }
    if (kDebugMode) debugPrint('ProformaModel._parseDate: unexpected type ${v.runtimeType}');
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

  static double _num(dynamic v) {
    if (v == null) return 0;
    if (v is num) return v.toDouble();
    return double.tryParse('$v') ?? 0;
  }

  factory ProformaModel.fromJson(Map<String, dynamic> json) {
    final idRaw = json['idProforma'] ?? json['id'];
    final id = idRaw is num ? idRaw.toInt() : int.tryParse('$idRaw') ?? 0;

    String? clientName = json['clientName'] as String?;
    final clientDto = json['clientDto'];
    if (clientName == null && clientDto is Map<String, dynamic>) {
      final n = clientDto['name'] ?? clientDto['businessName'];
      if (n != null) clientName = n.toString();
    }

    final idClientRaw = json['idClient'];
    final idClient = idClientRaw == null
        ? 0
        : (idClientRaw is num ? idClientRaw.toInt() : int.tryParse('$idClientRaw') ?? 0);

    return ProformaModel(
      id: id,
      proformaNumber: json['proformaNumber'] as String? ?? '',
      issueDate: _parseDate(json['issueDate']),
      expiryDate: _parseDateNullable(json['validUntil'] ?? json['expiryDate']),
      idClient: idClient,
      idCompany: (json['idCompany'] as num?)?.toInt() ?? 0,
      subtotalIva: _num(json['subtotalIva']),
      subtotalZero: _num(json['subtotalZero']),
      totalAmount: _num(json['totalAmount']),
      idPaymentMethod: (json['idPaymentMethod'] as num?)?.toInt() ?? 1,
      status: json['status'] as String?,
      convertedInvoiceId: json['idInvoice'] != null
          ? (json['idInvoice'] is num
              ? (json['idInvoice'] as num).toInt()
              : int.tryParse('${json['idInvoice']}'))
          : json['convertedInvoiceId'] != null
              ? (json['convertedInvoiceId'] is num
                  ? (json['convertedInvoiceId'] as num).toInt()
                  : int.tryParse('${json['convertedInvoiceId']}'))
              : null,
      clientName: clientName,
    );
  }

  Proforma toEntity() => this;
}
