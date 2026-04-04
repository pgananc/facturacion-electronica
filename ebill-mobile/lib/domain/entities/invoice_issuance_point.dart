import 'package:equatable/equatable.dart';

class InvoiceIssuancePoint extends Equatable {
  const InvoiceIssuancePoint({
    required this.idInvoiceIssuancePoint,
    required this.code,
    required this.establishmentId,
    this.sequentialInvoice,
    this.status = true,
  });

  final int idInvoiceIssuancePoint;
  final String code;
  final int establishmentId;
  final int? sequentialInvoice;
  final bool status;

  @override
  List<Object?> get props => [
        idInvoiceIssuancePoint,
        code,
        establishmentId,
        sequentialInvoice,
        status,
      ];
}
