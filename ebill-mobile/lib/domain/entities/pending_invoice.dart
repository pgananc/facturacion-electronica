import 'package:equatable/equatable.dart';

class PendingInvoicesReport extends Equatable {
  final List<PendingInvoiceItem> items;
  final int totalPending;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  const PendingInvoicesReport({
    required this.items,
    this.totalPending = 0,
    this.totalElements = 0,
    this.totalPages = 0,
    this.hasNext = false,
  });

  @override
  List<Object?> get props => [items, totalPending, totalElements, totalPages, hasNext];
}

class PendingInvoiceItem extends Equatable {
  final String invoiceNumber;
  final DateTime issueDate;
  final String receptionStatusSri;
  final String authorizationStatusSri;
  final String receptionError;
  final String authorizationError;
  final int idClient;
  final String clientName;
  final int retryCount;

  const PendingInvoiceItem({
    required this.invoiceNumber,
    required this.issueDate,
    required this.receptionStatusSri,
    required this.authorizationStatusSri,
    this.receptionError = '',
    this.authorizationError = '',
    required this.idClient,
    required this.clientName,
    this.retryCount = 0,
  });

  @override
  List<Object?> get props => [
        invoiceNumber,
        issueDate,
        receptionStatusSri,
        authorizationStatusSri,
        receptionError,
        authorizationError,
        idClient,
        clientName,
        retryCount,
      ];
}
