import 'package:flutter/foundation.dart';

import '../../domain/entities/sales_summary.dart';

class SalesSummaryModel extends SalesSummary {
  const SalesSummaryModel({
    required super.totalSales,
    required super.totalInvoices,
    required super.averagePerInvoice,
    super.pendingInvoices = 0,
    super.pendingAmount = 0,
    super.dailySales = const [],
    super.topClients = const [],
    super.topProducts = const [],
    super.todayTotal = 0,
    super.weekTotal = 0,
    super.monthTotal = 0,
    super.previousMonthTotal = 0,
    super.totalClients = 0,
    super.totalProducts = 0,
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
    if (v == null) {
      if (kDebugMode) debugPrint('SalesSummaryModel._parseDate: null date value');
      return DateTime.utc(1970);
    }
    if (v is String) {
      return DateTime.tryParse(v) ?? DateTime.utc(1970);
    }
    if (kDebugMode) debugPrint('SalesSummaryModel._parseDate: unexpected type ${v.runtimeType}');
    return DateTime.utc(1970);
  }

  static DailySales _dailyFromJson(Map<String, dynamic> json) {
    return DailySales(
      date: _parseDate(json['date'] ?? json['saleDate'] ?? json['day']),
      amount: _num(json['amount'] ?? json['totalAmount'] ?? json['total']),
      count: _int(json['count'] ?? json['invoiceCount'] ?? json['invoices']),
    );
  }

  static TopClient _topClientFromJson(Map<String, dynamic> json) {
    return TopClient(
      name: (json['name'] ?? json['clientName'] ?? json['razonSocial'] ?? '')
          .toString(),
      totalAmount: _num(json['totalAmount'] ?? json['amount'] ?? json['total']),
      invoiceCount: _int(json['invoiceCount'] ?? json['count'] ?? json['invoices']),
    );
  }

  static TopProduct _topProductFromJson(Map<String, dynamic> json) {
    return TopProduct(
      id: _int(json['productId'] ?? json['id']),
      code: (json['productCode'] ?? json['code'] ?? '').toString(),
      name: (json['productName'] ?? json['name'] ?? '').toString(),
      totalSales: _num(json['totalSales'] ?? json['total']),
      quantitySold: _int(json['quantitySold'] ?? json['quantity']),
      invoiceCount: _int(json['invoiceCount'] ?? json['count']),
    );
  }

  /// Parses the backend `SalesDashboardResponseDto` shape as well as the
  /// legacy flat shape used by `/report/summary`.
  factory SalesSummaryModel.fromJson(Map<String, dynamic> json) {
    final root = json['data'] is Map<String, dynamic>
        ? json['data'] as Map<String, dynamic>
        : json;

    final isDashboard = root.containsKey('todayTotal') ||
        root.containsKey('salesTrend') ||
        root.containsKey('additionalMetrics');

    if (isDashboard) {
      return _fromDashboard(root);
    }
    return _fromLegacy(root);
  }

  static SalesSummaryModel _fromDashboard(Map<String, dynamic> root) {
    final metrics = root['additionalMetrics'] is Map<String, dynamic>
        ? root['additionalMetrics'] as Map<String, dynamic>
        : <String, dynamic>{};

    final trendRaw = root['salesTrend'] ?? [];
    final productsRaw = root['topProducts'] ?? [];

    final dailyList = trendRaw is List
        ? trendRaw.whereType<Map<String, dynamic>>().map(_dailyFromJson).toList()
        : <DailySales>[];

    final productList = productsRaw is List
        ? productsRaw.whereType<Map<String, dynamic>>().map(_topProductFromJson).toList()
        : <TopProduct>[];

    final today = _num(root['todayTotal']);
    final week = _num(root['weekTotal']);
    final month = _num(root['monthTotal']);

    return SalesSummaryModel(
      totalSales: month > 0 ? month : (week > 0 ? week : today),
      totalInvoices: _int(metrics['totalInvoices']),
      averagePerInvoice: _num(metrics['averageTicket']),
      todayTotal: today,
      weekTotal: week,
      monthTotal: month,
      previousMonthTotal: _num(root['previousMonthTotal']),
      dailySales: dailyList,
      topProducts: productList,
      totalClients: _int(metrics['totalClients']),
      totalProducts: _int(metrics['totalProducts']),
    );
  }

  static SalesSummaryModel _fromLegacy(Map<String, dynamic> root) {
    final dailyRaw = root['dailySales'] ?? root['daily_sales'] ?? [];
    final topRaw = root['topClients'] ?? root['top_clients'] ?? [];

    final dailyList = dailyRaw is List
        ? dailyRaw.whereType<Map<String, dynamic>>().map(_dailyFromJson).toList()
        : <DailySales>[];

    final topList = topRaw is List
        ? topRaw.whereType<Map<String, dynamic>>().map(_topClientFromJson).toList()
        : <TopClient>[];

    return SalesSummaryModel(
      totalSales: _num(root['totalSales'] ?? root['total_sales'] ?? root['total']),
      totalInvoices: _int(root['totalInvoices'] ?? root['total_invoices']),
      averagePerInvoice:
          _num(root['averagePerInvoice'] ?? root['average_per_invoice']),
      pendingInvoices: _int(root['pendingInvoices'] ?? root['pending_invoices']),
      pendingAmount: _num(root['pendingAmount'] ?? root['pending_amount']),
      dailySales: dailyList,
      topClients: topList,
    );
  }

  SalesSummary toEntity() => this;
}
