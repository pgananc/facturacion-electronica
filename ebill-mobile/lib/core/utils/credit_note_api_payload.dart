import '../../domain/entities/credit_note.dart';
import '../../domain/entities/emission_session.dart';

Map<String, dynamic> buildCreditNoteCreatePayload({
  required CreditNote creditNote,
  required List<Map<String, dynamic>> creditNoteDetails,
  required EmissionSession emission,
}) {
  final y = creditNote.issueDate.year.toString().padLeft(4, '0');
  final m = creditNote.issueDate.month.toString().padLeft(2, '0');
  final d = creditNote.issueDate.day.toString().padLeft(2, '0');
  return {
    'idCompany': creditNote.idCompany,
    'companyDto': {'idCompany': creditNote.idCompany},
    'idClient': creditNote.idClient,
    'clientDto': {'idClient': creditNote.idClient},
    'issueDate': '$y-$m-$d',
    'originalInvoiceId': creditNote.idInvoiceRef,
    'idInvoice': creditNote.idInvoiceRef,
    'reason': creditNote.reason,
    'totalAmount': creditNote.totalAmount,
    'subtotalZero': creditNote.subtotalZero,
    'subtotalIva': creditNote.subtotalIva,
    'subtotalIce': creditNote.subtotalIce,
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
    'creditNoteDetails': creditNoteDetails,
  };
}
