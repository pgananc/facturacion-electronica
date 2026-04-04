import '../../domain/entities/sales_by_product.dart';

class SalesByProductReportModel extends SalesByProductReport {
  const SalesByProductReportModel({
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

  factory SalesByProductReportModel.fromJson(Map<String, dynamic> json) {
    final itemsRaw = json['items'] ?? [];
    final paginationRaw = json['pagination'] is Map<String, dynamic>
        ? json['pagination'] as Map<String, dynamic>
        : <String, dynamic>{};

    final items = itemsRaw is List
        ? itemsRaw.whereType<Map<String, dynamic>>().map((m) {
            return SalesByProductItem(
              idProduct: _int(m['idProduct']),
              productName: (m['productName'] ?? '').toString(),
              productCode: (m['productCode'] ?? '').toString(),
              quantitySold: _int(m['quantitySold']),
              totalInvoiced: _num(m['totalInvoiced']),
              unitPrice: _num(m['unitPrice']),
            );
          }).toList()
        : <SalesByProductItem>[];

    return SalesByProductReportModel(
      items: items,
      totalElements: _int(paginationRaw['totalElements']),
      totalPages: _int(paginationRaw['totalPages']),
      hasNext: paginationRaw['hasNext'] == true,
    );
  }

  SalesByProductReport toEntity() => this;
}
