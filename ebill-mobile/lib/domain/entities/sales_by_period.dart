import 'package:equatable/equatable.dart';

class SalesByPeriodReport extends Equatable {
  final List<SalesByPeriodItem> items;
  final SalesByPeriodMetrics metrics;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  const SalesByPeriodReport({
    required this.items,
    required this.metrics,
    this.totalElements = 0,
    this.totalPages = 0,
    this.hasNext = false,
  });

  @override
  List<Object?> get props => [items, metrics, totalElements, totalPages, hasNext];
}

class SalesByPeriodItem extends Equatable {
  final String invoiceNumber;
  final int idClient;
  final String clientName;
  final String clientIdentification;
  final double subtotal;
  final double iva;
  final double total;
  final String authorizationStatusSri;
  final String receptionStatusSri;
  final DateTime issueDate;

  const SalesByPeriodItem({
    required this.invoiceNumber,
    required this.idClient,
    required this.clientName,
    required this.clientIdentification,
    required this.subtotal,
    required this.iva,
    required this.total,
    required this.authorizationStatusSri,
    required this.receptionStatusSri,
    required this.issueDate,
  });

  @override
  List<Object?> get props => [
        invoiceNumber,
        idClient,
        clientName,
        clientIdentification,
        subtotal,
        iva,
        total,
        authorizationStatusSri,
        receptionStatusSri,
        issueDate,
      ];
}

class SalesByPeriodMetrics extends Equatable {
  final double totalInvoiced;
  final int totalComprobantes;
  final double averageTicket;

  const SalesByPeriodMetrics({
    required this.totalInvoiced,
    required this.totalComprobantes,
    required this.averageTicket,
  });

  @override
  List<Object?> get props => [totalInvoiced, totalComprobantes, averageTicket];
}
