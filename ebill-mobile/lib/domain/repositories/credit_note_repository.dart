import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/credit_note.dart';

abstract class CreditNoteRepository {
  Future<Either<Failure, List<CreditNote>>> getCreditNotes({
    int page = 0,
    int pageSize = 20,
    required int idCompany,
  });

  Future<Either<Failure, CreditNote>> getCreditNoteById(int id);

  Future<Either<Failure, CreditNote>> createCreditNote({
    required CreditNote creditNote,
    required List<Map<String, dynamic>> items,
  });

  Future<Either<Failure, List<int>>> downloadPdf(int id);

  Future<Either<Failure, List<int>>> downloadXml(int id);
}
