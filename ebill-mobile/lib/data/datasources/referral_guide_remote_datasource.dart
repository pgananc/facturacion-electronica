import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_client.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/referral_guide_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class ReferralGuideRemoteDataSource {
  Future<List<ReferralGuideModel>> getReferralGuides({
    int? page,
    int? pageSize,
    int? idCompany,
  });

  Future<ReferralGuideModel> getReferralGuideById(int id, {int? idCompany});

  Future<ReferralGuideModel> createReferralGuide(Map<String, dynamic> data);

  Future<List<int>> downloadPdfBytes(int id);

  Future<List<int>> downloadXmlBytes(int id);
}

class ReferralGuideRemoteDataSourceImpl implements ReferralGuideRemoteDataSource {
  ReferralGuideRemoteDataSourceImpl(this.dioClient);

  final DioClient dioClient;

  static String get _baseUrl => AppConfig.referralGuideServiceUrl;

  @override
  Future<List<ReferralGuideModel>> getReferralGuides({
    int? page,
    int? pageSize,
    int? idCompany,
  }) async {
    if (idCompany == null) {
      throw const ServerException('idCompany es requerido');
    }
    try {
      final response = await dioClient.get(
        _baseUrl,
        queryParameters: <String, dynamic>{
          'idCompany': idCompany,
          'page': page ?? 0,
          'size': pageSize ?? 50,
          'sort': 'issueDate,desc',
        },
      );
      if (_isSuccess(response.statusCode)) {
        if (response.statusCode == 204 || response.data == null) {
          return const [];
        }
        final data = response.data;
        if (data is Map<String, dynamic>) {
          final content = data['content'];
          if (content is List) {
            return content
                .whereType<Map<String, dynamic>>()
                .map((e) => ReferralGuideModel.fromJson(e))
                .toList();
          }
        }
        if (data is List) {
          return data
              .whereType<Map<String, dynamic>>()
              .map((e) => ReferralGuideModel.fromJson(e))
              .toList();
        }
        return const [];
      }
      throw ServerException(
        'Error al listar guías de remisión',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<ReferralGuideModel> getReferralGuideById(int id, {int? idCompany}) async {
    try {
      final queryParameters = <String, dynamic>{};
      if (idCompany != null) {
        queryParameters['idCompany'] = idCompany;
      }
      final response = await dioClient.get(
        '$_baseUrl/$id',
        queryParameters: queryParameters.isEmpty ? null : queryParameters,
      );
      if (_isSuccess(response.statusCode)) {
        if (response.statusCode == 204 || response.data == null) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        return ReferralGuideModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw ServerException(
        'No se pudo cargar la guía de remisión',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<ReferralGuideModel> createReferralGuide(Map<String, dynamic> data) async {
    if (data['idCompany'] == null) {
      throw const ServerException('idCompany es requerido para crear guía de remisión');
    }
    try {
      final response = await dioClient.post(
        _baseUrl,
        data: data,
      );
      if (!_isSuccess(response.statusCode)) {
        throw ServerException(
          'No se pudo crear la guía de remisión',
          statusCode: response.statusCode,
        );
      }
      if (response.statusCode == 204 || response.data == null) {
        throw const ServerException('No se pudo crear la guía de remisión');
      }
      if (response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final dto = map['referralGuideDto'] ?? map['guideDto'] ?? map;
        if (dto is Map<String, dynamic>) {
          return ReferralGuideModel.fromJson(dto);
        }
      }
      throw ServerException(
        'No se pudo crear la guía de remisión',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<List<int>> downloadPdfBytes(int id) async {
    try {
      final response = await dioClient.get(
        '$_baseUrl/$id/pdf',
        options: Options(
          responseType: ResponseType.bytes,
          receiveTimeout: AppConfig.downloadReceiveTimeout,
        ),
      );
      if (_isSuccess(response.statusCode) && response.data != null) {
        final contentType = response.headers.value(Headers.contentTypeHeader) ?? '';
        if (contentType.contains('text/html') || contentType.contains('application/json')) {
          throw const ServerException('El servidor no devolvió el documento esperado');
        }
        final data = response.data;
        List<int> bytes;
        if (data is List<int>) {
          bytes = data;
        } else if (data is List) {
          bytes = List<int>.from(data);
        } else {
          bytes = <int>[];
        }
        if (bytes.isEmpty) throw const ServerException('Documento vacío recibido');
        return bytes;
      }
      throw ServerException('PDF no disponible', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<List<int>> downloadXmlBytes(int id) async {
    try {
      final response = await dioClient.get(
        '$_baseUrl/$id/xml',
        options: Options(
          responseType: ResponseType.bytes,
          receiveTimeout: AppConfig.downloadReceiveTimeout,
        ),
      );
      if (_isSuccess(response.statusCode) && response.data != null) {
        final contentType = response.headers.value(Headers.contentTypeHeader) ?? '';
        if (contentType.contains('text/html') || contentType.contains('application/json')) {
          throw const ServerException('El servidor no devolvió el documento esperado');
        }
        final data = response.data;
        List<int> bytes;
        if (data is List<int>) {
          bytes = data;
        } else if (data is List) {
          bytes = List<int>.from(data);
        } else {
          bytes = <int>[];
        }
        if (bytes.isEmpty) throw const ServerException('Documento vacío recibido');
        return bytes;
      }
      throw ServerException('XML no disponible', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Never _handleDio(DioException e) => handleDioException(e);
}
