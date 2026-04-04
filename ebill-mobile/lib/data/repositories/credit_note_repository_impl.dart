import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../core/storage/storage_service.dart';
import '../../core/utils/credit_note_api_payload.dart';
import '../../domain/entities/credit_note.dart';
import '../../domain/repositories/credit_note_repository.dart';
import '../datasources/credit_note_remote_datasource.dart';
import 'repository_guard.dart';

class CreditNoteRepositoryImpl
    with RepositoryGuard
    implements CreditNoteRepository {
  CreditNoteRepositoryImpl(this.remoteDataSource, this.storageService);

  final CreditNoteRemoteDataSource remoteDataSource;
  final StorageService storageService;

  @override
  Future<Either<Failure, List<CreditNote>>> getCreditNotes({
    int page = 0,
    int pageSize = 20,
    required int idCompany,
  }) =>
      guard(() async {
        final list = await remoteDataSource.getCreditNotes(
          page: page,
          pageSize: pageSize,
          idCompany: idCompany,
        );
        return list.map((m) => m.toEntity()).toList();
      });

  @override
  Future<Either<Failure, CreditNote>> getCreditNoteById(int id) =>
      guard(() async {
        final model = await remoteDataSource.getCreditNoteById(id);
        return model.toEntity();
      });

  @override
  Future<Either<Failure, CreditNote>> createCreditNote({
    required CreditNote creditNote,
    required List<Map<String, dynamic>> items,
  }) async {
    final emission = storageService.getEmissionSession();
    if (emission == null) {
      return const Left(
        ValidationFailure('Seleccione un punto de emisión en el menú principal.'),
      );
    }
    return guard(() async {
      final payload = buildCreditNoteCreatePayload(
        creditNote: creditNote,
        creditNoteDetails: items,
        emission: emission,
      );
      final created = await remoteDataSource.createCreditNote(payload);
      return created.toEntity();
    });
  }

  @override
  Future<Either<Failure, List<int>>> downloadPdf(int id) =>
      guard(() => remoteDataSource.downloadPdfBytes(id));

  @override
  Future<Either<Failure, List<int>>> downloadXml(int id) =>
      guard(() => remoteDataSource.downloadXmlBytes(id));
}
