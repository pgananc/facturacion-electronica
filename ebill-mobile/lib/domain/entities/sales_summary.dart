import 'package:equatable/equatable.dart';

class SalesSummary extends Equatable {
  final double totalSales;
  final int totalInvoices;
  final double averagePerInvoice;
  final int pendingInvoices;
  final double pendingAmount;
  final List<DailySales> dailySales;
  final List<TopClient> topClients;
  final List<TopProduct> topProducts;
  final double todayTotal;
  final double weekTotal;
  final double monthTotal;
  final double previousMonthTotal;
  final int totalClients;
  final int totalProducts;

  const SalesSummary({
    required this.totalSales,
    required this.totalInvoices,
    required this.averagePerInvoice,
    this.pendingInvoices = 0,
    this.pendingAmount = 0,
    this.dailySales = const [],
    this.topClients = const [],
    this.topProducts = const [],
    this.todayTotal = 0,
    this.weekTotal = 0,
    this.monthTotal = 0,
    this.previousMonthTotal = 0,
    this.totalClients = 0,
    this.totalProducts = 0,
  });

  @override
  List<Object?> get props => [
        totalSales,
        totalInvoices,
        averagePerInvoice,
        pendingInvoices,
        pendingAmount,
        dailySales,
        topClients,
        topProducts,
        todayTotal,
        weekTotal,
        monthTotal,
        previousMonthTotal,
        totalClients,
        totalProducts,
      ];
}

class DailySales extends Equatable {
  final DateTime date;
  final double amount;
  final int count;

  const DailySales({
    required this.date,
    required this.amount,
    required this.count,
  });

  @override
  List<Object?> get props => [date, amount, count];
}

class TopClient extends Equatable {
  final String name;
  final double totalAmount;
  final int invoiceCount;

  const TopClient({
    required this.name,
    required this.totalAmount,
    required this.invoiceCount,
  });

  @override
  List<Object?> get props => [name, totalAmount, invoiceCount];
}

class TopProduct extends Equatable {
  final int id;
  final String code;
  final String name;
  final double totalSales;
  final int quantitySold;
  final int invoiceCount;

  const TopProduct({
    required this.id,
    required this.code,
    required this.name,
    required this.totalSales,
    required this.quantitySold,
    required this.invoiceCount,
  });

  @override
  List<Object?> get props => [id, code, name, totalSales, quantitySold, invoiceCount];
}
