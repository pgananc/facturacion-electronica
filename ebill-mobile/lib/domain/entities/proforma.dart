import 'package:equatable/equatable.dart';

class Proforma extends Equatable {
  final int id;
  final String proformaNumber;
  final DateTime issueDate;
  final DateTime? expiryDate;
  final int idClient;
  final int idCompany;
  final double subtotalIva;
  final double subtotalZero;
  final double totalAmount;
  final int idPaymentMethod;
  final String? status;
  final int? convertedInvoiceId;
  final String? clientName;

  const Proforma({
    required this.id,
    required this.proformaNumber,
    required this.issueDate,
    this.expiryDate,
    required this.idClient,
    required this.idCompany,
    required this.subtotalIva,
    required this.subtotalZero,
    required this.totalAmount,
    required this.idPaymentMethod,
    this.status,
    this.convertedInvoiceId,
    this.clientName,
  });

  @override
  List<Object?> get props => [
        id,
        proformaNumber,
        issueDate,
        expiryDate,
        idClient,
        idCompany,
        subtotalIva,
        subtotalZero,
        totalAmount,
        idPaymentMethod,
        status,
        convertedInvoiceId,
        clientName,
      ];
}
