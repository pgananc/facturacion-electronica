import 'package:equatable/equatable.dart';

class Invoice extends Equatable {
  final int id;
  final String invoiceNumber;
  final DateTime issueDate;
  final int idClient;
  final int idCompany;
  final double subtotalIva;
  final double subtotalZero;
  final double subtotalIce;
  final double totalAmount;
  final int idPaymentMethod;
  final String? status;
  final String? authorizationNumber;
  final DateTime? authorizationDate;
  final String? clientName;
  final String? clientIdentification;

  const Invoice({
    required this.id,
    required this.invoiceNumber,
    required this.issueDate,
    required this.idClient,
    required this.idCompany,
    required this.subtotalIva,
    required this.subtotalZero,
    this.subtotalIce = 0,
    required this.totalAmount,
    required this.idPaymentMethod,
    this.status,
    this.authorizationNumber,
    this.authorizationDate,
    this.clientName,
    this.clientIdentification,
  });

  @override
  List<Object?> get props => [
        id,
        invoiceNumber,
        issueDate,
        idClient,
        idCompany,
        subtotalIva,
        subtotalZero,
        subtotalIce,
        totalAmount,
        idPaymentMethod,
        status,
        authorizationNumber,
        authorizationDate,
        clientName,
        clientIdentification,
      ];
}

