import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/proforma_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class ProformaRemoteDataSource {
  Future<List<ProformaModel>> getProformas({
    int? page,
    int? pageSize,
    int? idCompany,
    String? status,
  });

  Future<ProformaModel> getProformaById(int id, int idCompany);

  Future<ProformaModel> createProforma(Map<String, dynamic> data);

  Future<int> convertToInvoice(int id, int idCompany);
}

class ProformaRemoteDataSourceImpl implements ProformaRemoteDataSource {
  ProformaRemoteDataSourceImpl(this.dioClient);

  final DioClient dioClient;

  String get _baseUrl => AppConfig.proformaServiceUrl;

  @override
  Future<List<ProformaModel>> getProformas({
    int? page,
    int? pageSize,
    int? idCompany,
    String? status,
  }) async {
    if (idCompany == null) {
      throw const ServerException('idCompany es requerido');
    }
    try {
      final queryParameters = <String, dynamic>{
        'idCompany': idCompany,
        'page': page ?? 0,
        'size': pageSize ?? 50,
        'sort': 'issueDate,desc',
      };
      if (status != null && status.isNotEmpty) {
        queryParameters['status'] = status;
      }
      final response = await dioClient.get(
        _baseUrl,
        queryParameters: queryParameters,
      );
      if (_isSuccess(response.statusCode)) {
        final data = response.data;
        if (data is Map<String, dynamic>) {
          final content = data['content'];
          if (content is List) {
            return content
                .whereType<Map<String, dynamic>>()
                .map((e) => ProformaModel.fromJson(e))
                .toList();
          }
        }
        if (data is List) {
          return data
              .whereType<Map<String, dynamic>>()
              .map((e) => ProformaModel.fromJson(e))
              .toList();
        }
        return const [];
      }
      throw ServerException(
        'Error al listar proformas',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<ProformaModel> getProformaById(int id, int idCompany) async {
    try {
      final response = await dioClient.get(
        '$_baseUrl/$id',
        queryParameters: {'idCompany': idCompany},
      );
      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        return ProformaModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw ServerException(
        'No se pudo cargar la proforma',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<ProformaModel> createProforma(Map<String, dynamic> data) async {
    try {
      final response = await dioClient.post(
        _baseUrl,
        data: data,
      );
      if (_isSuccess(response.statusCode) && response.data is Map<String, dynamic>) {
        return ProformaModel.fromJson(response.data as Map<String, dynamic>);
      }
      throw ServerException(
        'No se pudo crear la proforma',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<int> convertToInvoice(int id, int idCompany) async {
    try {
      final response = await dioClient.post(
        '$_baseUrl/$id/convert-to-invoice',
        queryParameters: {'idCompany': idCompany},
      );
      if (_isSuccess(response.statusCode) && response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final idInvoice = map['idInvoice'];
        if (idInvoice is int) return idInvoice;
        if (idInvoice is num) return idInvoice.toInt();
        if (idInvoice != null) {
          final parsed = int.tryParse('$idInvoice');
          if (parsed != null) return parsed;
        }
      }
      throw ServerException(
        'No se pudo convertir a factura',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  Never _handleDio(DioException e) => handleDioException(e);
}
