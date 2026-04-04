import '../../domain/entities/sales_by_client.dart';

class SalesByClientReportModel extends SalesByClientReport {
  const SalesByClientReportModel({
    required super.items,
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

  factory SalesByClientReportModel.fromJson(Map<String, dynamic> json) {
    final itemsRaw = json['items'] ?? [];
    final paginationRaw = json['pagination'] is Map<String, dynamic>
        ? json['pagination'] as Map<String, dynamic>
        : <String, dynamic>{};

    final items = itemsRaw is List
        ? itemsRaw.whereType<Map<String, dynamic>>().map((m) {
            return SalesByClientItem(
              idClient: _int(m['idClient']),
              clientName: (m['clientName'] ?? '').toString(),
              clientIdentification: (m['clientIdentification'] ?? '').toString(),
              totalSales: _num(m['totalSales']),
              totalInvoices: _int(m['totalInvoices']),
              averageTicket: _num(m['averageTicket']),
            );
          }).toList()
        : <SalesByClientItem>[];

    return SalesByClientReportModel(
      items: items,
      totalElements: _int(paginationRaw['totalElements']),
      totalPages: _int(paginationRaw['totalPages']),
      hasNext: paginationRaw['hasNext'] == true,
    );
  }

  SalesByClientReport toEntity() => this;
}
