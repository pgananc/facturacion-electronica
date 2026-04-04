import 'package:flutter/foundation.dart';

import '../../domain/entities/invoice.dart';

class InvoiceModel extends Invoice {
  const InvoiceModel({
    required super.id,
    required super.invoiceNumber,
    required super.issueDate,
    required super.idClient,
    required super.idCompany,
    required super.subtotalIva,
    required super.subtotalZero,
    super.subtotalIce = 0,
    required super.totalAmount,
    required super.idPaymentMethod,
    super.status,
    super.authorizationNumber,
    super.authorizationDate,
    super.clientName,
    super.clientIdentification,
  });

  static DateTime _parseDate(dynamic v) {
    if (v == null) {
      if (kDebugMode) debugPrint('InvoiceModel._parseDate: null date value');
      return DateTime.utc(1970);
    }
    if (v is String) {
      if (v.length >= 10) {
        return DateTime.tryParse(v.substring(0, 10)) ?? DateTime.tryParse(v) ?? DateTime.utc(1970);
      }
      return DateTime.tryParse(v) ?? DateTime.utc(1970);
    }
    if (kDebugMode) debugPrint('InvoiceModel._parseDate: unexpected type ${v.runtimeType}');
    return DateTime.utc(1970);
  }

  static int? _safeInt(dynamic value) {
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value);
    return null;
  }

  static double _num(dynamic v) {
    if (v == null) return 0;
    if (v is num) return v.toDouble();
    return double.tryParse('$v') ?? 0;
  }

  factory InvoiceModel.fromJson(Map<String, dynamic> json) {
    final idRaw = json['idInvoice'] ?? json['id'];
    final id = idRaw is num ? idRaw.toInt() : int.tryParse('$idRaw') ?? 0;

    String authNum = json['authorizationNumber'] as String? ?? '';
    final authSri = json['authorizationSri'];
    if (authNum.isEmpty && authSri is Map<String, dynamic>) {
      final n = authSri['authorizationNumber'];
      if (n != null) authNum = n.toString();
    }

    DateTime? authDate;
    final ad = json['authorizationDate'];
    if (ad is String) {
      authDate = DateTime.tryParse(ad);
    }
    if (authDate == null && authSri is Map<String, dynamic>) {
      final f = authSri['authorizationDate'];
      if (f is String) authDate = DateTime.tryParse(f);
    }

    String? status = json['documentStatus'] as String?;
    if (status == null || status.isEmpty) {
      final rawStatus = json['status'];
      if (rawStatus is String) {
        status = rawStatus;
      }
    }

    String? clientName = json['buyerName'] as String?;
    String? clientIdentification = json['buyerIdentification'] as String?;
    final clientDto = json['clientDto'];
    if (clientDto is Map<String, dynamic>) {
      clientName ??= clientDto['name'] as String?;
      clientIdentification ??= clientDto['identification'] as String?;
    }

    return InvoiceModel(
      id: id,
      invoiceNumber: json['invoiceNumber'] as String? ?? '',
      issueDate: _parseDate(json['issueDate']),
      idClient: _safeInt(json['idClient']) ?? 0,
      idCompany: _safeInt(json['idCompany']) ?? 0,
      subtotalIva: _num(json['subtotalIva']),
      subtotalZero: _num(json['subtotalZero']),
      subtotalIce: _num(json['subtotalIce']),
      totalAmount: _num(json['totalAmount']),
      idPaymentMethod: _safeInt(json['idPaymentMethod']) ?? 1,
      status: status,
      authorizationNumber: authNum.isEmpty ? null : authNum,
      authorizationDate: authDate,
      clientName: clientName,
      clientIdentification: clientIdentification,
    );
  }

  Invoice toEntity() => this;
}
