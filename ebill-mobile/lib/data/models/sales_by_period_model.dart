import '../../domain/entities/sales_by_period.dart';

class SalesByPeriodReportModel extends SalesByPeriodReport {
  const SalesByPeriodReportModel({
    required super.items,
    required super.metrics,
    super.totalElements,
    super.totalPages,
    super.hasNext,
  });

  static double _num(dynamic v) {
    if (v == null) return 0;
    if (v is num) return v.toDouble();
    return double.tryParse('$v') ?? 0;
  }

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

  factory SalesByPeriodReportModel.fromJson(Map<String, dynamic> json) {
    final itemsRaw = json['items'] ?? [];
    final metricsRaw = json['metrics'] is Map<String, dynamic>
        ? json['metrics'] as Map<String, dynamic>
        : <String, dynamic>{};
    final paginationRaw = json['pagination'] is Map<String, dynamic>
        ? json['pagination'] as Map<String, dynamic>
        : <String, dynamic>{};

    final items = itemsRaw is List
        ? itemsRaw.whereType<Map<String, dynamic>>().map((m) {
            return SalesByPeriodItem(
              invoiceNumber: (m['invoiceNumber'] ?? '').toString(),
              idClient: _int(m['idClient']),
              clientName: (m['clientName'] ?? '').toString(),
              clientIdentification: (m['clientIdentification'] ?? '').toString(),
              subtotal: _num(m['subtotal']),
              iva: _num(m['iva']),
              total: _num(m['total']),
              authorizationStatusSri: (m['authorizationStatusSri'] ?? '').toString(),
              receptionStatusSri: (m['receptionStatusSri'] ?? '').toString(),
              issueDate: _parseDate(m['issueDate']),
            );
          }).toList()
        : <SalesByPeriodItem>[];

    final metrics = SalesByPeriodMetrics(
      totalInvoiced: _num(metricsRaw['totalInvoiced']),
      totalComprobantes: _int(metricsRaw['totalComprobantes']),
      averageTicket: _num(metricsRaw['averageTicket']),
    );

    return SalesByPeriodReportModel(
      items: items,
      metrics: metrics,
      totalElements: _int(paginationRaw['totalElements']),
      totalPages: _int(paginationRaw['totalPages']),
      hasNext: paginationRaw['hasNext'] == true,
    );
  }

  SalesByPeriodReport toEntity() => this;
}
