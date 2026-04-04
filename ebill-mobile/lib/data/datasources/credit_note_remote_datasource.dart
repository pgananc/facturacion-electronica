import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_client.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/credit_note_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class CreditNoteRemoteDataSource {
  Future<List<CreditNoteModel>> getCreditNotes({
    int page = 0,
    int pageSize = 20,
    required int idCompany,
  });

  Future<CreditNoteModel> getCreditNoteById(int id);

  Future<CreditNoteModel> createCreditNote(Map<String, dynamic> payload);

  Future<List<int>> downloadPdfBytes(int id);

  Future<List<int>> downloadXmlBytes(int id);
}

class CreditNoteRemoteDataSourceImpl implements CreditNoteRemoteDataSource {
  CreditNoteRemoteDataSourceImpl(this.dioClient);

  final DioClient dioClient;

  static String get _baseUrl => AppConfig.creditNoteServiceUrl;

  @override
  Future<List<CreditNoteModel>> getCreditNotes({
    int page = 0,
    int pageSize = 20,
    required int idCompany,
  }) async {
    try {
      final response = await dioClient.post(
        '$_baseUrl/search-by-company',
        data: {
          'idCompany': idCompany,
        },
        queryParameters: {
          'page': page,
          'size': pageSize,
          'sort': 'creationDate,desc',
        },
      );
      if (_isSuccess(response.statusCode)) {
        if (response.statusCode == 204 || response.data == null) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        final content = response.data['content'];
        if (content is! List) return const [];
        return content
            .whereType<Map<String, dynamic>>()
            .map((e) => CreditNoteModel.fromJson(e))
            .toList();
      }
      throw ServerException(
        'Error al listar notas de crédito',
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<CreditNoteModel> getCreditNoteById(int id) async {
    try {
      final response = await dioClient.get('$_baseUrl/$id');

      if (_isSuccess(response.statusCode)) {
        if (response.statusCode == 204 || response.data == null) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        return CreditNoteModel.fromJson(response.data as Map<String, dynamic>);
      } else {
        throw ServerException(
          'No se pudo cargar la nota de crédito',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _handleDio(e);
    }
  }

  @override
  Future<CreditNoteModel> createCreditNote(Map<String, dynamic> payload) async {
    try {
      final response = await dioClient.post(
        _baseUrl,
        data: payload,
      );

      if (!_isSuccess(response.statusCode)) {
        throw ServerException(
          'No se pudo crear la nota de crédito',
          statusCode: response.statusCode,
        );
      }
      if (response.statusCode == 204 || response.data == null) {
        throw const ServerException('No se pudo crear la nota de crédito');
      }
      if (response.data is Map<String, dynamic>) {
        final map = response.data as Map<String, dynamic>;
        final creditNoteDto = map['creditNoteDto'];
        final list = map['creditNoteDtos'];
        if (creditNoteDto is Map<String, dynamic>) {
          return CreditNoteModel.fromJson(creditNoteDto);
        }
        if (list is List && list.isNotEmpty && list.first is Map<String, dynamic>) {
          return CreditNoteModel.fromJson(list.first as Map<String, dynamic>);
        }
        if (map.containsKey('idCreditNote') || map.containsKey('creditNoteNumber')) {
          return CreditNoteModel.fromJson(map);
        }
      }
      throw ServerException(
        'No se pudo crear la nota de crédito',
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
