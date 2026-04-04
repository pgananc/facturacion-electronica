import 'package:dio/dio.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../models/product_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class ProductRemoteDataSource {
  Future<List<ProductModel>> getProducts({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
  });
  Future<ProductModel> getProductById(int id);
  Future<ProductModel> createProduct(ProductModel product);
  Future<ProductModel> updateProduct(int id, ProductModel product);
  Future<void> deleteProduct(int id);
}

class ProductRemoteDataSourceImpl implements ProductRemoteDataSource {
  final DioClient dioClient;

  ProductRemoteDataSourceImpl(this.dioClient);

  @override
  Future<List<ProductModel>> getProducts({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
  }) async {
    try {
      // El backend tiene dos opciones:
      // 1. GET /company/{idCompany} - Obtener por compañía
      // 2. GET /pageable - Búsqueda avanzada con query params
      
      // Si se proporciona idCompany, usar GET /company/{id}
      if (idCompany != null) {
        final response = await dioClient.get(
          '${AppConfig.productServiceUrl}/company/$idCompany',
        );

        if (_isSuccess(response.statusCode)) {
          if (response.data is! Map<String, dynamic>) {
            throw const ServerException('Respuesta inesperada del servidor');
          }
          // El backend devuelve ProductResponse con productDtos
          final List<dynamic> productDtos = response.data['productDtos'] ?? [];
          return productDtos
              .whereType<Map<String, dynamic>>()
              .map((json) => ProductModel.fromJson(json))
              .toList();
        } else {
          throw ServerException(
            'Error al obtener productos por compañía',
            statusCode: response.statusCode,
          );
        }
      }

      // Si no hay idCompany, usar GET /pageable con query params
      final queryParams = <String, dynamic>{
        'page': page ?? 0,
        'size': pageSize ?? 20,
        'status': true, // Solo productos activos por defecto
      };
      
      if (search != null && search.isNotEmpty) {
        queryParams['name'] = search;
      }

      final response = await dioClient.get(
        '${AppConfig.productServiceUrl}/pageable',
        queryParameters: queryParams,
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        // El backend devuelve Page<ProductDto>
        final List<dynamic> content = response.data['content'] ?? [];
        return content
            .whereType<Map<String, dynamic>>()
            .map((json) => ProductModel.fromJson(json))
            .toList();
      } else {
        throw ServerException(
          'Error al obtener productos',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e, 'Error al obtener productos');
    } catch (e) {
      if (e is ServerException || e is NetworkException) rethrow;
      throw const ServerException('Error inesperado al obtener productos.');
    }
  }

  @override
  Future<ProductModel> getProductById(int id) async {
    try {
      // El backend usa GET /id/{idProduct}
      final response = await dioClient.get(
        '${AppConfig.productServiceUrl}/id/$id',
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        // El backend devuelve ProductResponse con productDto
        final map = response.data as Map<String, dynamic>;
        final raw = map['productDto'] ?? map;
        if (raw is! Map<String, dynamic>) {
          throw const ServerException('Formato de producto inválido');
        }
        return ProductModel.fromJson(raw);
      } else {
        throw ServerException(
          'Error al obtener producto',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e, 'Error al obtener producto');
    }
  }

  @override
  Future<ProductModel> createProduct(ProductModel product) async {
    try {
      final response = await dioClient.post(
        AppConfig.productServiceUrl,
        data: product.toJson(),
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        final map = response.data as Map<String, dynamic>;
        final raw = map['productDto'] ?? map;
        if (raw is! Map<String, dynamic>) {
          throw const ServerException('Formato de producto inválido');
        }
        return ProductModel.fromJson(raw);
      } else {
        throw ServerException(
          'Error al crear producto',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e, 'Error al crear producto');
    }
  }

  @override
  Future<ProductModel> updateProduct(int id, ProductModel product) async {
    try {
      final response = await dioClient.patch(
        '${AppConfig.productServiceUrl}/$id',
        data: product.toJson(),
      );

      if (!_isSuccess(response.statusCode)) {
        throw ServerException(
          'Error al actualizar producto',
          statusCode: response.statusCode,
        );
      }
      if (response.statusCode == 204 || response.data == null) {
        return await getProductById(id);
      }
      if (response.data is! Map<String, dynamic>) {
        throw const ServerException('Respuesta inesperada del servidor');
      }
      final map = response.data as Map<String, dynamic>;
      final raw = map['productDto'] ?? map;
      if (raw is! Map<String, dynamic>) {
        throw const ServerException('Formato de producto inválido');
      }
      return ProductModel.fromJson(raw);
    } on DioException catch (e) {
      _throwDio(e, 'Error al actualizar producto');
    }
  }

  @override
  Future<void> deleteProduct(int id) async {
    try {
      final response = await dioClient.delete(
        '${AppConfig.productServiceUrl}/$id',
      );

      if (!_isSuccess(response.statusCode)) {
        throw ServerException(
          'Error al eliminar producto',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e, 'Error al eliminar producto');
    }
  }

  Never _throwDio(DioException e, String fallback) => handleDioException(e);
}

