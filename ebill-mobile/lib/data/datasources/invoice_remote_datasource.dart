import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/invoice_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class InvoiceRemoteDataSource {
  Future<List<InvoiceModel>> getInvoices({
    int? page,
    int? pageSize,
    int? idCompany,
  });

  /// Búsqueda paginada por compañía (recomendado).
  Future<List<InvoiceModel>> searchInvoices({
    required int idCompany,
    int page,
    int size,
  });

  Future<InvoiceModel> getInvoiceById(int id);
  Future<InvoiceModel> createInvoice(Map<String, dynamic> invoiceData);
  Future<InvoiceModel> saveDraft(Map<String, dynamic> invoiceData);
  Future<List<Map<String, dynamic>>> getDrafts();
  Future<void> deleteDraft(int id);
  Future<List<int>> downloadPdfBytes(int id);
  Future<List<int>> downloadXmlBytes(int id);
}

class InvoiceRemoteDataSourceImpl implements InvoiceRemoteDataSource {
  final DioClient dioClient;

  InvoiceRemoteDataSourceImpl(this.dioClient);

  @override
  Future<List<InvoiceModel>> searchInvoices({
    required int idCompany,
    int page = 0,
    int size = 50,
  }) async {
    try {
      final response = await dioClient.post(
        '${AppConfig.billingServiceUrl}/search',
        data: {
          'idCompany': idCompany,
        },
        queryParameters: {
          'page': page,
          'size': size,
          'sort': 'creationDate,desc',
        },
      );
      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        final content = response.data['content'];
        if (content is! List) return const [];
        return content
            .whereType<Map<String, dynamic>>()
            .map((e) => InvoiceModel.fromJson(e))
            .toList();
      }
      throw ServerException('Error al listar facturas', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<List<InvoiceModel>> getInvoices({
    int? page,
    int? pageSize,
    int? idCompany,
  }) async {
    if (idCompany == null) {
      throw const ServerException('idCompany es requerido para listar facturas');
    }
    return searchInvoices(idCompany: idCompany, page: page ?? 0, size: pageSize ?? 50);
  }

  @override
  Future<InvoiceModel> getInvoiceById(int id) async {
    try {
      final response = await dioClient.get(
        '${AppConfig.billingServiceUrl}/$id',
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        return InvoiceModel.fromJson(response.data as Map<String, dynamic>);
      } else {
        throw ServerException(
          'Error al obtener factura',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<InvoiceModel> createInvoice(Map<String, dynamic> invoiceData) async {
    try {
      final response = await dioClient.post(
        AppConfig.billingServiceUrl,
        data: invoiceData,
      );

      if (_isSuccess(response.statusCode) && response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final invoiceDto = map['invoiceDto'] ?? map;
        if (invoiceDto is Map<String, dynamic>) {
          return InvoiceModel.fromJson(invoiceDto);
        }
      }
      throw ServerException(
        'Error al crear factura',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<InvoiceModel> saveDraft(Map<String, dynamic> invoiceData) async {
    try {
      final response = await dioClient.post(
        '${AppConfig.billingServiceUrl}/draft',
        data: invoiceData,
      );
      if (!_isSuccess(response.statusCode)) {
        throw ServerException('No se pudo guardar el borrador', statusCode: response.statusCode);
      }
      if (response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final invoiceDto = map['invoiceDto'] ?? map;
        if (invoiceDto is Map<String, dynamic>) {
          return InvoiceModel.fromJson(invoiceDto);
        }
      }
      throw ServerException('No se pudo guardar el borrador', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<List<Map<String, dynamic>>> getDrafts() async {
    try {
      final response = await dioClient.get('${AppConfig.billingServiceUrl}/drafts');
      if (_isSuccess(response.statusCode) && response.data is List) {
        return (response.data as List).whereType<Map<String, dynamic>>().toList();
      }
      throw ServerException('Borradores no disponibles', statusCode: response.statusCode);
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<void> deleteDraft(int id) async {
    try {
      final response = await dioClient.delete('${AppConfig.billingServiceUrl}/draft/$id');
      if (!_isSuccess(response.statusCode)) {
        final msg = response.data is Map ? response.data['message'] : null;
        throw ServerException(msg?.toString() ?? 'No se pudo eliminar', statusCode: response.statusCode);
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<List<int>> downloadPdfBytes(int id) async {
    try {
      final response = await dioClient.get(
        '${AppConfig.billingServiceUrl}/$id/pdf',
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
        '${AppConfig.billingServiceUrl}/$id/xml',
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
