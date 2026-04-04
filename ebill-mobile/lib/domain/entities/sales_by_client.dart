import 'package:equatable/equatable.dart';

class SalesByClientReport extends Equatable {
  final List<SalesByClientItem> items;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  const SalesByClientReport({
    required this.items,
    this.totalElements = 0,
    this.totalPages = 0,
    this.hasNext = false,
  });

  @override
  List<Object?> get props => [items, totalElements, totalPages, hasNext];
}

class SalesByClientItem extends Equatable {
  final int idClient;
  final String clientName;
  final String clientIdentification;
  final double totalSales;
  final int totalInvoices;
  final double averageTicket;

  const SalesByClientItem({
    required this.idClient,
    required this.clientName,
    required this.clientIdentification,
    required this.totalSales,
    required this.totalInvoices,
    required this.averageTicket,
  });

  @override
  List<Object?> get props => [
        idClient,
        clientName,
        clientIdentification,
        totalSales,
        totalInvoices,
        averageTicket,
      ];
}
