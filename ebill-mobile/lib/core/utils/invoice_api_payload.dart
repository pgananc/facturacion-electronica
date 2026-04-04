import '../../domain/entities/emission_session.dart';
import 'invoice_tax_calculator.dart';

/// Construye el JSON esperado por `POST /api/invoice` (InvoiceDto).
Map<String, dynamic> buildInvoiceCreatePayload({
  required int idCompany,
  required int idClient,
  required DateTime issueDate,
  required int idPaymentMethod,
  required InvoiceTotals totals,
  required EmissionSession emission,
}) {
  final y = issueDate.year.toString().padLeft(4, '0');
  final m = issueDate.month.toString().padLeft(2, '0');
  final d = issueDate.day.toString().padLeft(2, '0');
  return {
    'idCompany': idCompany,
    'idClient': idClient,
    'issueDate': '$y-$m-$d',
    'totalAmount': totals.totalAmount,
    'subtotalZero': totals.subtotalZero,
    'subtotalIva': totals.subtotalIva,
    'subtotalIce': totals.subtotalIce,
    'discount': 0,
    'tip': 0,
    'establishment': {
      'idEstablishment': emission.idEstablishment,
      'code': emission.establishmentCode,
      'issuancePoint': {
        'idIssuancePoint': emission.idInvoiceIssuancePoint,
        'code': emission.issuancePointCode,
      },
    },
    'invoiceDetails': totals.invoiceDetails,
    'paymentMethods': [
      {
        'idPaymentMethod': idPaymentMethod,
        'total': totals.totalAmount,
      },
    ],
  };
}

/// `POST /api/invoice/draft` — sin punto de emisión (el backend lo anula).
Map<String, dynamic> buildInvoiceDraftPayload({
  required int idCompany,
  required int idClient,
  required DateTime issueDate,
  required int idPaymentMethod,
  required InvoiceTotals totals,
}) {
  final y = issueDate.year.toString().padLeft(4, '0');
  final m = issueDate.month.toString().padLeft(2, '0');
  final d = issueDate.day.toString().padLeft(2, '0');
  return {
    'idCompany': idCompany,
    'idClient': idClient,
    'issueDate': '$y-$m-$d',
    'totalAmount': totals.totalAmount,
    'subtotalZero': totals.subtotalZero,
    'subtotalIva': totals.subtotalIva,
    'subtotalIce': totals.subtotalIce,
    'discount': 0,
    'tip': 0,
    'invoiceDetails': totals.invoiceDetails,
    'paymentMethods': [
      {
        'idPaymentMethod': idPaymentMethod,
        'total': totals.totalAmount,
      },
    ],
  };
}
