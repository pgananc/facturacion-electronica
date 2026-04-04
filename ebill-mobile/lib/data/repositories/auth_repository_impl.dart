import 'dart:convert';

import 'package:dartz/dartz.dart';
import 'package:flutter/foundation.dart';

import '../../core/error/failures.dart';
import '../../core/storage/storage_service.dart';
import '../../core/utils/jwt_payload.dart';
import '../../domain/entities/company_establishment.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../datasources/company_remote_datasource.dart';
import '../datasources/invoice_session_remote_datasource.dart';
import 'repository_guard.dart';

class AuthRepositoryImpl with RepositoryGuard implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final StorageService _storageService;
  final CompanyRemoteDataSource _companyRemoteDataSource;
  final InvoiceSessionRemoteDataSource _sessionDataSource;

  AuthRepositoryImpl(
    this.remoteDataSource,
    this._storageService,
    this._companyRemoteDataSource,
    this._sessionDataSource,
  );

  List<CompanyEstablishment> _establishmentsFromPayload(JwtPayload? payload) {
    if (payload == null) return const [];
    return payload.companyEstablishments
        .where((c) => c.idCompany != null && c.idCompany! > 0)
        .map(
          (c) => CompanyEstablishment(
            idCompany: c.idCompany!,
            establishmentIds: c.establishmentIds,
          ),
        )
        .toList();
  }

  Future<void> _ensureDefaultCompanySelection(int? companyId) async {
    if (companyId == null) return;
    final current = await _storageService.getSelectedCompany();
    if (current == null || current.isEmpty) {
      await _storageService.saveSelectedCompany('$companyId');
    }
  }

  @override
  Future<Either<Failure, User>> login(String username, String password) =>
      guard(() async {
        final tokens = await remoteDataSource.login(username, password);
        final jwt = tokens.accessToken;

        await _storageService.saveToken(jwt);
        if (tokens.refreshToken != null && tokens.refreshToken!.isNotEmpty) {
          await _storageService.saveRefreshToken(tokens.refreshToken!);
        }

        final payload = JwtPayload.fromToken(jwt);
        final establishments = _establishmentsFromPayload(payload);
        final companyId =
            establishments.isNotEmpty ? establishments.first.idCompany : null;
        final id = payload?.subjectUserId ?? 0;
        final nameFromToken = payload?.userName;

        String? ruc;
        if (companyId != null) {
          try {
            ruc = await _companyRemoteDataSource
                .getCompanyIdentification(companyId);
            if (ruc != null && ruc.isNotEmpty) {
              await _storageService.saveCompanyIdentification(ruc);
            }
          } catch (_) {
            ruc = _storageService.getCompanyIdentification();
          }
        }

        var user = User(
          id: id,
          username: nameFromToken ?? username,
          email: '',
          isActive: true,
          companyEstablishments: establishments,
          companyId: companyId,
          companyIdentification: ruc,
        );

        await _ensureDefaultCompanySelection(companyId);

        try {
          final dto = await remoteDataSource.getUserByUserName(user.username);
          final names = _splitFullName(dto.name);
          user = user.copyWith(
            id: dto.idUser,
            email: dto.mail ?? '',
            firstName: names.$1,
            lastName: names.$2,
          );
        } catch (e) {
          if (kDebugMode) {
            debugPrint('AuthRepository: user enrichment failed: $e');
          }
        }

        await _storageService.saveUserData(jsonEncode(user.toJson()));

        return user;
      });

  @override
  Future<Either<Failure, void>> logout() async {
    try {
      final userData = await _storageService.getUserData();
      String userName = '';
      if (userData != null && userData.isNotEmpty) {
        try {
          final map = jsonDecode(userData) as Map<String, dynamic>;
          userName = map['username'] as String? ?? '';
        } catch (_) {}
      }
      try {
        await _sessionDataSource.releasePoint();
      } catch (e) {
        if (kDebugMode) {
          debugPrint('AuthRepository: remote logout failed: $e');
        }
      }
      try {
        await remoteDataSource.logout(userName);
      } catch (e) {
        if (kDebugMode) {
          debugPrint('AuthRepository: remote logout failed: $e');
        }
      }

      await _storageService.clearAll();
      return const Right(null);
    } catch (e) {
      await _storageService.clearAll();
      return const Right(null);
    }
  }

  @override
  Future<Either<Failure, User?>> getCurrentUser() async {
    try {
      final userData = await _storageService.getUserData();
      if (userData != null && userData.isNotEmpty) {
        final map = jsonDecode(userData) as Map<String, dynamic>;
        var user = User.fromJson(map);
        final ruc = _storageService.getCompanyIdentification();
        if (ruc != null &&
            ruc.isNotEmpty &&
            (user.companyIdentification == null || user.companyIdentification!.isEmpty)) {
          user = user.copyWith(companyIdentification: ruc);
        }
        return Right(user);
      }
      final token = _storageService.getToken();
      if (token != null && token.isNotEmpty) {
        final payload = JwtPayload.fromToken(token);
        if (payload != null) {
          final establishments = _establishmentsFromPayload(payload);
          final companyId =
              establishments.isNotEmpty ? establishments.first.idCompany : null;
          final user = User(
            id: payload.subjectUserId ?? 0,
            username: payload.userName ?? '',
            email: '',
            isActive: true,
            companyEstablishments: establishments,
            companyId: companyId,
          );
          await _ensureDefaultCompanySelection(companyId);
          await _storageService.saveUserData(jsonEncode(user.toJson()));
          return Right(user);
        }
      }
      return const Right(null);
    } catch (_) {
      return const Left(CacheFailure('Error al obtener el usuario.'));
    }
  }

  @override
  Future<Either<Failure, bool>> isAuthenticated() async {
    try {
      final token = _storageService.getToken();
      if (token == null || token.isEmpty) {
        return const Right(false);
      }
      final payload = JwtPayload.fromToken(token);
      if (payload == null) {
        return const Right(false);
      }
      if (payload.isExpired(bufferSeconds: 30)) {
        return const Right(false);
      }
      return const Right(true);
    } catch (_) {
      return const Left(CacheFailure('Error al verificar la autenticación.'));
    }
  }

  (String?, String?) _splitFullName(String? name) {
    if (name == null || name.trim().isEmpty) return (null, null);
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.length == 1) return (parts.first, null);
    return (parts.first, parts.sublist(1).join(' '));
  }

  @override
  Future<Either<Failure, User>> fetchUserProfile(String userName) =>
      guard(() async {
        final dto = await remoteDataSource.getUserByUserName(userName);
        final existingResult = await getCurrentUser();
        User? existing;
        existingResult.fold((_) {}, (u) => existing = u);
        final names = _splitFullName(dto.name);
        final base = existing ??
            User(
              id: dto.idUser,
              username: dto.userName,
              email: dto.mail ?? '',
            );
        final merged = base.copyWith(
          id: dto.idUser,
          username: dto.userName,
          email: dto.mail ?? base.email,
          firstName: names.$1 ?? base.firstName,
          lastName: names.$2 ?? base.lastName,
          companyEstablishments: base.companyEstablishments,
          companyId: base.companyId,
          companyIdentification: base.companyIdentification,
        );
        await _storageService.saveUserData(jsonEncode(merged.toJson()));
        return merged;
      });

  @override
  Future<Either<Failure, void>> changePassword({
    required int userId,
    required String userName,
    required String currentPassword,
    required String newPassword,
  }) =>
      guard(() async {
        await remoteDataSource.validateCredentials(userName, currentPassword);
        await remoteDataSource.changePassword(userId, newPassword);
      });

  @override
  Future<Either<Failure, void>> requestPasswordReset(String email) =>
      guard(() => remoteDataSource.sendPasswordResetEmail(email));

  @override
  Future<Either<Failure, User>> updateUserProfile({
    required int userId,
    required String userName,
    String? name,
    String? mail,
  }) async {
    final updateResult = await guard(
      () => remoteDataSource.updateUserProfile(userId, name: name, mail: mail),
    );
    return updateResult.fold(
      (failure) => Left(failure),
      (_) => fetchUserProfile(userName),
    );
  }
}
