import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../core/config/app_config.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_client.dart';
import '../../core/network/dio_exception_handler.dart';

/// Datos de compañía desde ebill-customer (RUC / identificación).
abstract class CompanyRemoteDataSource {
  /// Retorna la identificación tributaria (RUC) de la compañía.
  Future<String?> getCompanyIdentification(int idCompany);

  Future<String> getCompanyDisplayName(int idCompany);
}

class CompanyRemoteDataSourceImpl implements CompanyRemoteDataSource {
  CompanyRemoteDataSourceImpl(this._dio);

  final DioClient _dio;

  Future<Map<String, dynamic>?> _getCompanyMap(int idCompany) async {
    try {
      final response = await _dio.get('${AppConfig.companyServiceUrl}/$idCompany');
      if (response.data is Map<String, dynamic>) {
        return response.data as Map<String, dynamic>;
      }
      return null;
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) {
        return null;
      }
      handleDioException(e);
    } catch (e) {
      if (kDebugMode) debugPrint('CompanyRemoteDataSource._getCompanyMap: $e');
      throw const ServerException('Error al obtener información de la compañía');
    }
  }

  @override
  Future<String?> getCompanyIdentification(int idCompany) async {
    final m = await _getCompanyMap(idCompany);
    if (m == null) return null;
    final id = m['identification'];
    if (id is String && id.isNotEmpty) return id;
    if (id != null) return id.toString();
    return null;
  }

  @override
  Future<String> getCompanyDisplayName(int idCompany) async {
    final m = await _getCompanyMap(idCompany);
    if (m == null) return 'Compañía $idCompany';
    for (final key in ['name', 'businessName', 'companyName', 'razonSocial', 'tradeName']) {
      final v = m[key];
      if (v is String && v.isNotEmpty) return v;
    }
    final id = m['identification'];
    if (id is String && id.isNotEmpty) return id;
    if (id != null) return id.toString();
    return 'Compañía $idCompany';
  }
}
