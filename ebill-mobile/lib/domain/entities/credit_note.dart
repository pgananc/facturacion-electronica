import 'package:equatable/equatable.dart';

/// Nota de crédito (dominio; usado por listados y payloads).
class CreditNote extends Equatable {
  final int id;
  final String creditNoteNumber;
  final DateTime issueDate;
  final int idClient;
  final int idCompany;
  final int idInvoiceRef;
  final String? invoiceRefNumber;
  final String reason;
  final double subtotalIva;
  final double subtotalZero;
  final double subtotalIce;
  final double totalAmount;
  final String? status;
  final String? authorizationNumber;
  final DateTime? authorizationDate;
  final String? clientName;

  const CreditNote({
    required this.id,
    required this.creditNoteNumber,
    required this.issueDate,
    required this.idClient,
    required this.idCompany,
    required this.idInvoiceRef,
    this.invoiceRefNumber,
    required this.reason,
    required this.subtotalIva,
    required this.subtotalZero,
    this.subtotalIce = 0,
    required this.totalAmount,
    this.status,
    this.authorizationNumber,
    this.authorizationDate,
    this.clientName,
  });

  @override
  List<Object?> get props => [
        id,
        creditNoteNumber,
        issueDate,
        idClient,
        idCompany,
        idInvoiceRef,
        invoiceRefNumber,
        reason,
        subtotalIva,
        subtotalZero,
        subtotalIce,
        totalAmount,
        status,
        authorizationNumber,
        authorizationDate,
        clientName,
      ];
}
