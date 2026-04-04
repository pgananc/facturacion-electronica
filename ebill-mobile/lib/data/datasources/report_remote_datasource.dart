import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../core/config/app_config.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_client.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/pending_invoice_model.dart';
import '../models/sales_by_client_model.dart';
import '../models/sales_by_period_model.dart';
import '../models/sales_by_product_model.dart';
import '../models/sales_summary_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class ReportRemoteDataSource {
  Future<SalesSummaryModel> getSalesSummary({
    required int idCompany,
    required String period,
    String? from,
    String? to,
  });

  Future<SalesByPeriodReportModel> getSalesByPeriod({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  });

  Future<SalesByClientReportModel> getSalesByClient({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  });

  Future<SalesByProductReportModel> getSalesByProduct({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  });

  Future<PendingInvoicesReportModel> getPendingInvoices({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    String? statusFilter,
    int page = 0,
    int size = 20,
  });
}

class ReportRemoteDataSourceImpl implements ReportRemoteDataSource {
  ReportRemoteDataSourceImpl(this.dioClient);

  final DioClient dioClient;

  static String get _baseUrl => AppConfig.reportServiceUrl;

  @override
  Future<SalesSummaryModel> getSalesSummary({
    required int idCompany,
    required String period,
    String? from,
    String? to,
  }) async {
    try {
      // Alineado con web: GET /dashboard/sales + query (respuesta ApiResponse con `data`).
      final queryParameters = <String, dynamic>{
        'companyId': idCompany,
      };
      if (from != null && from.isNotEmpty) {
        queryParameters['dateFrom'] = '${from}T00:00:00';
      }
      if (to != null && to.isNotEmpty) {
        queryParameters['dateTo'] = '${to}T23:59:59';
      }

      final url = '$_baseUrl/dashboard/sales';
      if (kDebugMode) {
        debugPrint('[ReportDS] GET $url period=$period query=$queryParameters');
      }
      final response = await dioClient.get(
        url,
        queryParameters: queryParameters,
      );

      return _parseResponse(response, SalesSummaryModel.fromJson);
    } on DioException catch (e) {
      if (kDebugMode) {
        debugPrint(
          '[ReportDS] dashboard/sales failed: type=${e.type} '
          'status=${e.response?.statusCode} data=${e.response?.data}',
        );
      }
      _handleDio(e);
    }
  }

  @override
  Future<SalesByPeriodReportModel> getSalesByPeriod({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await dioClient.post(
        '$_baseUrl/sales/by-period',
        data: {
          'idCompany': idCompany,
          'dateFrom': '${dateFrom}T00:00:00',
          'dateTo': '${dateTo}T23:59:59',
        },
        queryParameters: {
          'page': page,
          'size': size,
        },
      );

      return _parseResponse(response, SalesByPeriodReportModel.fromJson);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<SalesByClientReportModel> getSalesByClient({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await dioClient.post(
        '$_baseUrl/sales/by-client?page=$page&size=$size&sort=totalSales,desc',
        data: {
          'idCompany': idCompany,
          'dateFrom': '${dateFrom}T00:00:00',
          'dateTo': '${dateTo}T23:59:59',
        },
      );

      return _parseResponse(response, SalesByClientReportModel.fromJson);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<SalesByProductReportModel> getSalesByProduct({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await dioClient.post(
        '$_baseUrl/sales/by-product?page=$page&size=$size&sort=totalInvoiced,desc',
        data: {
          'idCompany': idCompany,
          'dateFrom': '${dateFrom}T00:00:00',
          'dateTo': '${dateTo}T23:59:59',
        },
      );

      return _parseResponse(response, SalesByProductReportModel.fromJson);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<PendingInvoicesReportModel> getPendingInvoices({
    required int idCompany,
    required String dateFrom,
    required String dateTo,
    String? statusFilter,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final body = <String, dynamic>{
        'idCompany': idCompany,
        'dateFrom': '${dateFrom}T00:00:00',
        'dateTo': '${dateTo}T23:59:59',
      };
      if (statusFilter != null && statusFilter.isNotEmpty) {
        body['statusFilter'] = statusFilter;
      }

      final response = await dioClient.post(
        '$_baseUrl/invoices/pending',
        data: body,
        queryParameters: {
          'page': page,
          'size': size,
        },
      );

      return _parseResponse(response, PendingInvoicesReportModel.fromJson);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  T _parseResponse<T>(Response response, T Function(Map<String, dynamic>) parser) {
    if (!_isSuccess(response.statusCode)) {
      throw ServerException(
        'Error al cargar el reporte',
        statusCode: response.statusCode,
      );
    }
    if (response.statusCode == 204 || response.data == null) {
      throw const ServerException('Respuesta de reporte inválida');
    }
    final data = response.data;
    if (data is Map<String, dynamic>) {
      return parser(data);
    }
    throw const ServerException('Respuesta de reporte inválida');
  }

  Never _handleDio(DioException e) => handleDioException(e);
}
