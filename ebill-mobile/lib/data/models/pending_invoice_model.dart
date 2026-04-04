import '../../domain/entities/pending_invoice.dart';

class PendingInvoicesReportModel extends PendingInvoicesReport {
  const PendingInvoicesReportModel({
    required super.items,
    super.totalPending,
    super.totalElements,
    super.totalPages,
    super.hasNext,
  });

  static int _int(dynamic v) {
    if (v == null) return 0;
    if (v is int) return v;
    if (v is num) return v.toInt();
    return int.tryParse('$v') ?? 0;
  }

  static DateTime _parseDate(dynamic v) {
    if (v == null) return DateTime.now();
    if (v is String) return DateTime.tryParse(v) ?? DateTime.now();
    return DateTime.now();
  }

  factory PendingInvoicesReportModel.fromJson(Map<String, dynamic> json) {
    final itemsRaw = json['items'] ?? [];
    final paginationRaw = json['pagination'] is Map<String, dynamic>
        ? json['pagination'] as Map<String, dynamic>
        : <String, dynamic>{};

    final items = itemsRaw is List
        ? itemsRaw.whereType<Map<String, dynamic>>().map((m) {
            return PendingInvoiceItem(
              invoiceNumber: (m['invoiceNumber'] ?? '').toString(),
              issueDate: _parseDate(m['issueDate']),
              receptionStatusSri: (m['receptionStatusSri'] ?? '').toString(),
              authorizationStatusSri: (m['authorizationStatusSri'] ?? '').toString(),
              receptionError: (m['receptionError'] ?? '').toString(),
              authorizationError: (m['authorizationError'] ?? '').toString(),
              idClient: _int(m['idClient']),
              clientName: (m['clientName'] ?? '').toString(),
              retryCount: _int(m['retryCount']),
            );
          }).toList()
        : <PendingInvoiceItem>[];

    return PendingInvoicesReportModel(
      items: items,
      totalPending: _int(json['totalPending']),
      totalElements: _int(paginationRaw['totalElements']),
      totalPages: _int(paginationRaw['totalPages']),
      hasNext: paginationRaw['hasNext'] == true,
    );
  }

  PendingInvoicesReport toEntity() => this;
}
