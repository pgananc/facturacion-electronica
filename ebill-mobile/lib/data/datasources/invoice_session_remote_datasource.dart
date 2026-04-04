import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/invoice_issuance_point_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

/// Sesión de punto de emisión (ebill-authentication `/api/invoice-points/session`).
class InvoiceSessionRemoteDataSource {
  InvoiceSessionRemoteDataSource(this._dio);

  final DioClient _dio;

  static String get _base => '${AppConfig.authServiceUrl}/invoice-points/session';

  Future<List<InvoiceIssuancePointModel>> getAvailablePoints(
    List<int> establishmentIds,
  ) async {
    if (establishmentIds.isEmpty) return const [];
    try {
      final response = await _dio.get(
        '$_base/available',
        queryParameters: {
          'idEstablishments': establishmentIds,
        },
      );
      if (_isSuccess(response.statusCode) && response.data is List) {
        return (response.data as List)
            .whereType<Map<String, dynamic>>()
            .map((e) => InvoiceIssuancePointModel.fromJson(e))
            .toList();
      }
      throw ServerException('Puntos de emisión no disponibles', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Future<void> assignPoint(int idInvoiceIssuancePoint) async {
    try {
      final response = await _dio.post(
        _base,
        data: {'idInvoiceIssuancePoint': idInvoiceIssuancePoint},
      );
      if (!_isSuccess(response.statusCode)) {
        throw ServerException('No se pudo asignar el punto', statusCode: response.statusCode);
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Future<void> releasePoint() async {
    try {
      await _dio.delete(_base);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Future<void> heartbeat() async {
    try {
      await _dio.post('$_base/heartbeat');
    } on DioException catch (e) {
      if (e.response?.statusCode == 404 ||
          e.response?.statusCode == 401 ||
          e.response?.statusCode == 403) {
        throw const AuthException('Sesión de punto de emisión inactiva o expirada');
      }
      _handleDio(e);
    }
  }

  Never _handleDio(DioException e) => handleDioException(e);
}
