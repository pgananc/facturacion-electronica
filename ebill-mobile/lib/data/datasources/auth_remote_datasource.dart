import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/login_response_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

/// Minimal user payload from `GET /api/user/userName/{userName}`.
class RemoteUserProfileDto {
  RemoteUserProfileDto({
    required this.idUser,
    required this.userName,
    this.mail,
    this.name,
  });

  final int idUser;
  final String userName;
  final String? mail;
  final String? name;
}

class LoginTokens {
  final String accessToken;
  final String? refreshToken;
  const LoginTokens({required this.accessToken, this.refreshToken});
}

abstract class AuthRemoteDataSource {
  Future<LoginTokens> login(String username, String password);
  Future<void> logout(String userName);

  Future<RemoteUserProfileDto> getUserByUserName(String userName);

  Future<void> validateCredentials(String userName, String password);

  Future<void> changePassword(int idUser, String newPassword);

  Future<void> updateUserProfile(int idUser, {String? name, String? mail});

  Future<void> sendPasswordResetEmail(String email);
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final DioClient dioClient;

  AuthRemoteDataSourceImpl(this.dioClient);

  @override
  Future<LoginTokens> login(String username, String password) async {
    try {
      final response = await dioClient.post(
        '${AppConfig.authServiceUrl}/login',
        data: {
          'userName': username,
          'password': password,
        },
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        final data = response.data as Map<String, dynamic>;

        if (data['code'] != 1 || data['status'] != 'OK') {
          throw ServerException(
            'Error de autenticación: ${data['status'] ?? 'estado desconocido'}',
            statusCode: response.statusCode,
          );
        }

        final loginResponse = LoginResponseModel.fromJson(data);

        final tokenModel = loginResponse.token;
        if (tokenModel == null || tokenModel.token.isEmpty) {
          throw ServerException(
            'Token vacío en el response de login',
            statusCode: response.statusCode,
          );
        }

        return LoginTokens(
          accessToken: tokenModel.token,
          refreshToken: tokenModel.refreshToken,
        );
      } else {
        throw ServerException('Error de autenticación', statusCode: response.statusCode);
      }
    } on DioException catch (e) {
      _handleDio(e);
    } catch (e) {
      if (e is ServerException || e is NetworkException) rethrow;
      throw const ServerException('Error inesperado al procesar la solicitud.');
    }
  }

  @override
  Future<void> logout(String userName) async {
    try {
      await dioClient.post(
        '${AppConfig.authServiceUrl}/login/logout',
        data: {'userName': userName},
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<RemoteUserProfileDto> getUserByUserName(String userName) async {
    try {
      final response = await dioClient.get(
        '${AppConfig.authServiceUrl}/user/userName/${Uri.encodeComponent(userName)}',
      );
      if (!_isSuccess(response.statusCode)) {
        throw ServerException('No se pudo cargar el perfil', statusCode: response.statusCode);
      }
      if (response.data is! Map<String, dynamic>) {
        throw const ServerException('Respuesta inesperada del servidor');
      }
      final data = response.data as Map<String, dynamic>;
      Map<String, dynamic>? ud = data['userDto'] as Map<String, dynamic>?;
      if (ud == null) {
        final userDtos = data['userDtos'];
        if (userDtos is! List || userDtos.isEmpty) {
          throw const ServerException('Datos de usuario no encontrados');
        }
        Map<String, dynamic>? userMap;
        for (final e in userDtos) {
          if (e is Map<String, dynamic>) {
            userMap = e;
            break;
          }
        }
        if (userMap == null) {
          throw const ServerException('Formato de datos de usuario inválido');
        }
        ud = userMap;
      }
      final idRaw = ud['idUser'];
      final int? idUserParsed = idRaw is int ? idRaw : int.tryParse('$idRaw');
      if (idUserParsed == null) {
        throw const ServerException('ID de usuario no encontrado en la respuesta');
      }
      final idUser = idUserParsed;
      return RemoteUserProfileDto(
        idUser: idUser,
        userName: ud['userName'] as String? ?? userName,
        mail: ud['mail'] as String?,
        name: ud['name'] as String?,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<void> validateCredentials(String userName, String password) async {
    try {
      final response = await dioClient.post(
        '${AppConfig.authServiceUrl}/user/userName/password',
        data: {
          'userName': userName,
          'password': password,
        },
      );
      if (!_isSuccess(response.statusCode)) {
        throw const ServerException('Contraseña actual incorrecta');
      }
      if (response.data != null && response.data is! Map<String, dynamic>) {
        throw const ServerException('Respuesta inesperada del servidor');
      }
      final data = response.data as Map<String, dynamic>?;
      if (data != null && data['code'] != null && data['code'] != 1) {
        throw ServerException(data['message']?.toString() ?? 'Credenciales inválidas');
      }
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) {
        throw const ServerException('Contraseña actual incorrecta');
      }
      _handleDio(e);
    }
  }

  @override
  Future<void> changePassword(int idUser, String newPassword) async {
    try {
      final response = await dioClient.patch(
        '${AppConfig.authServiceUrl}/user/$idUser/password',
        data: {'password': newPassword},
      );
      if (!_isSuccess(response.statusCode)) {
        throw const ServerException('No se pudo actualizar la contraseña');
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<void> updateUserProfile(int idUser, {String? name, String? mail}) async {
    try {
      final data = <String, dynamic>{'idUser': idUser};
      if (name != null) data['name'] = name;
      if (mail != null) data['mail'] = mail;

      final response = await dioClient.put(
        '${AppConfig.authServiceUrl}/user',
        data: data,
      );
      if (!_isSuccess(response.statusCode)) {
        throw const ServerException('No se pudo actualizar el perfil');
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<void> sendPasswordResetEmail(String email) async {
    try {
      await dioClient.post(
        '${AppConfig.authServiceUrl}/mail/send',
        data: email,
        options: Options(contentType: 'text/plain'),
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Never _handleDio(DioException e) => handleDioException(e);
}

