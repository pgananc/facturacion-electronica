import 'package:dartz/dartz.dart';
import 'package:flutter/foundation.dart';

import '../../core/error/exception.dart';
import '../../core/error/failures.dart';

/// Wraps a datasource call with consistent exception → Failure mapping.
mixin RepositoryGuard {
  Future<Either<Failure, T>> guard<T>(Future<T> Function() body) async {
    try {
      final result = await body();
      return Right(result);
    } on NetworkException catch (e, st) {
      if (kDebugMode) {
        debugPrint('[RepositoryGuard] NetworkException: ${e.message}');
        debugPrint('$st');
      }
      final message = kDebugMode ? e.message : 'Error de conexión';
      return Left(NetworkFailure(message));
    } on ServerException catch (e, st) {
      if (kDebugMode) {
        debugPrint(
          '[RepositoryGuard] ServerException: status=${e.statusCode} msg=${e.message}',
        );
        debugPrint('$st');
      }
      final message = kDebugMode ? e.message : 'Error del servidor';
      return Left(ServerFailure(message, statusCode: e.statusCode));
    } on AuthException catch (e, st) {
      if (kDebugMode) {
        debugPrint('[RepositoryGuard] AuthException: ${e.message}');
        debugPrint('$st');
      }
      return Left(AuthFailure(e.message));
    } on CacheException catch (e, st) {
      if (kDebugMode) {
        debugPrint('[RepositoryGuard] CacheException: ${e.message}');
        debugPrint('$st');
      }
      return Left(CacheFailure(e.message));
    } catch (e, st) {
      if (kDebugMode) {
        debugPrint('[RepositoryGuard] Unknown: $e');
        debugPrint('$st');
      }
      return const Left(UnknownFailure('Ocurrió un error inesperado.'));
    }
  }
}
