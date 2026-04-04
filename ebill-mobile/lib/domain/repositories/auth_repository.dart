import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/user.dart';

abstract class AuthRepository {
  Future<Either<Failure, User>> login(String username, String password);
  Future<Either<Failure, void>> logout();
  Future<Either<Failure, User?>> getCurrentUser();
  Future<Either<Failure, bool>> isAuthenticated();

  /// Loads profile from auth MS and merges into stored [User] (email, name).
  Future<Either<Failure, User>> fetchUserProfile(String userName);

  Future<Either<Failure, void>> changePassword({
    required int userId,
    required String userName,
    required String currentPassword,
    required String newPassword,
  });

  Future<Either<Failure, User>> updateUserProfile({
    required int userId,
    required String userName,
    String? name,
    String? mail,
  });

  Future<Either<Failure, void>> requestPasswordReset(String email);
}

