import 'package:flutter/foundation.dart';

import '../../domain/entities/credit_note.dart';

class CreditNoteModel extends CreditNote {
  const CreditNoteModel({
    required super.id,
    required super.creditNoteNumber,
    required super.issueDate,
    required super.idClient,
    required super.idCompany,
    required super.idInvoiceRef,
    super.invoiceRefNumber,
    required super.reason,
    required super.subtotalIva,
    required super.subtotalZero,
    super.subtotalIce,
    required super.totalAmount,
    super.status,
    super.authorizationNumber,
    super.authorizationDate,
    super.clientName,
  });

  static DateTime _parseDate(dynamic v) {
    if (v == null) {
      if (kDebugMode) debugPrint('CreditNoteModel._parseDate: null date value');
      return DateTime.utc(1970);
    }
    if (v is String) {
      if (v.length >= 10) {
        return DateTime.tryParse(v.substring(0, 10)) ?? DateTime.tryParse(v) ?? DateTime.utc(1970);
      }
      return DateTime.tryParse(v) ?? DateTime.utc(1970);
    }
    if (kDebugMode) debugPrint('CreditNoteModel._parseDate: unexpected type ${v.runtimeType}');
    return DateTime.utc(1970);
  }

  static double _num(dynamic v) {
    if (v == null) return 0;
    if (v is num) return v.toDouble();
    return double.tryParse('$v') ?? 0;
  }

  factory CreditNoteModel.fromJson(Map<String, dynamic> json) {
    final idRaw = json['idCreditNote'] ?? json['id'];
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

    final status = json['creditNoteStatus'] as String? ??
        json['documentStatus'] as String? ??
        json['status'] as String?;

    final issueRaw = json['issueDate'];
    final DateTime issueDate;
    if (issueRaw is String) {
      issueDate = DateTime.tryParse(issueRaw) ?? _parseDate(issueRaw);
    } else {
      issueDate = _parseDate(issueRaw);
    }

    final idInvoiceRefRaw =
        json['originalInvoiceId'] ?? json['idInvoice'] ?? 0;
    final idInvoiceRef = idInvoiceRefRaw is num
        ? idInvoiceRefRaw.toInt()
        : int.tryParse('$idInvoiceRefRaw') ?? 0;

    String reason = json['reason'] as String? ?? '';
    final reasonEntity = json['creditNoteReason'];
    if (reason.isEmpty && reasonEntity is Map<String, dynamic>) {
      final d = reasonEntity['description'];
      if (d != null) reason = d.toString();
    }

    String? clientName = json['buyerName'] as String?;
    final clientDto = json['clientDto'];
    if (clientName == null && clientDto is Map<String, dynamic>) {
      clientName = clientDto['name'] as String?;
    }

    final idClientRaw = json['idClient'] ??
        (clientDto is Map<String, dynamic> ? clientDto['idClient'] : null);
    final idClient = idClientRaw is num
        ? idClientRaw.toInt()
        : int.tryParse('$idClientRaw') ?? 0;

    final idCompanyRaw = json['idCompany'];
    final idCompany = idCompanyRaw is num
        ? idCompanyRaw.toInt()
        : int.tryParse('$idCompanyRaw') ?? 0;

    return CreditNoteModel(
      id: id,
      creditNoteNumber: json['creditNoteNumber'] as String? ?? '',
      issueDate: issueDate,
      idClient: idClient,
      idCompany: idCompany,
      idInvoiceRef: idInvoiceRef,
      invoiceRefNumber: json['originalInvoiceNumber'] as String?,
      reason: reason,
      subtotalIva: _num(json['subtotalIva']),
      subtotalZero: _num(json['subtotalZero']),
      subtotalIce: _num(json['subtotalIce']),
      totalAmount: _num(json['totalAmount']),
      status: status,
      authorizationNumber: authNum.isEmpty ? null : authNum,
      authorizationDate: authDate,
      clientName: clientName,
    );
  }

  CreditNote toEntity() => this;
}
