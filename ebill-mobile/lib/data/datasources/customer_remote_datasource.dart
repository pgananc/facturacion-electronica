import 'package:dio/dio.dart';
import 'package:get_it/get_it.dart';

import '../../core/config/app_config.dart';
import '../../core/network/dio_client.dart';
import '../../core/error/exception.dart';
import '../../core/network/dio_exception_handler.dart';
import '../../core/storage/storage_service.dart';
import '../models/customer_model.dart';
import '../models/client_search_request_model.dart';

bool _isSuccess(int? status) => status != null && status >= 200 && status < 300;

abstract class CustomerRemoteDataSource {
  Future<List<CustomerModel>> getCustomers({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
    String? companyIdentification,
  });
  Future<CustomerModel> getCustomerById(int id);
  Future<CustomerModel> createCustomer(CustomerModel customer);
  Future<CustomerModel> updateCustomer(int id, CustomerModel customer);
  Future<void> deleteCustomer(int id);
}

class CustomerRemoteDataSourceImpl implements CustomerRemoteDataSource {
  final DioClient dioClient;

  CustomerRemoteDataSourceImpl(this.dioClient);

  @override
  Future<List<CustomerModel>> getCustomers({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
    String? companyIdentification,
  }) async {
    try {
      var effectiveCompanyIdentification = companyIdentification;
      if (idCompany != null &&
          (effectiveCompanyIdentification == null || effectiveCompanyIdentification.isEmpty)) {
        effectiveCompanyIdentification =
            GetIt.instance<StorageService>().getCompanyIdentification();
        if (effectiveCompanyIdentification == null || effectiveCompanyIdentification.isEmpty) {
          throw const ServerException(
            'Identificación de compañía requerida para buscar clientes',
          );
        }
      }

      // Preferir RUC (`companyIdentification`) + POST /pageable (filtros correctos).
      if (effectiveCompanyIdentification != null && effectiveCompanyIdentification.isNotEmpty) {
        final searchRequest = ClientSearchRequestModel(
          companyIdentification: effectiveCompanyIdentification,
          identification: search,
          name: search,
          status: true,
        );
        final response = await dioClient.post(
          '${AppConfig.customerServiceUrl}/pageable',
          data: searchRequest.toJson(),
          queryParameters: {
            'page': page ?? 0,
            'size': pageSize ?? 50,
            'sort': 'name,asc',
          },
        );
        if (_isSuccess(response.statusCode)) {
          if (response.statusCode == 204 || response.data == null) {
            return const [];
          }
          if (response.data is! Map<String, dynamic>) {
            throw const ServerException('Respuesta inesperada del servidor');
          }
          final List<dynamic> content = response.data['content'] ?? [];
          return content
              .whereType<Map<String, dynamic>>()
              .map((json) => CustomerModel.fromJson(json))
              .toList();
        }
        throw ServerException('Error al obtener clientes', statusCode: response.statusCode);
      }

      final searchRequest = ClientSearchRequestModel(
        companyIdentification: companyIdentification,
        identification: search,
        name: search,
        status: true,
      );

      final response = await dioClient.post(
        '${AppConfig.customerServiceUrl}/pageable',
        data: searchRequest.toJson(),
        queryParameters: {
          'page': page ?? 0,
          'size': pageSize ?? 20,
          'sort': 'name,asc',
        },
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        // El backend devuelve Page<ClientDto>
        final List<dynamic> content = response.data['content'] ?? [];
        return content
            .whereType<Map<String, dynamic>>()
            .map((json) => CustomerModel.fromJson(json))
            .toList();
      } else {
        throw ServerException(
          'Error al obtener clientes',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e);
    } catch (e) {
      if (e is ServerException || e is NetworkException) rethrow;
      throw const ServerException('Error inesperado al obtener clientes.');
    }
  }

  @override
  Future<CustomerModel> getCustomerById(int id) async {
    try {
      final response = await dioClient.get(
        '${AppConfig.customerServiceUrl}/$id',
      );

      if (_isSuccess(response.statusCode)) {
        if (response.data is! Map<String, dynamic>) {
          throw const ServerException('Respuesta inesperada del servidor');
        }
        final List<dynamic> clientDtos = response.data['clientDtos'] ?? [];
        if (clientDtos.isEmpty) {
          throw const ServerException('Cliente no encontrado');
        }
        final firstDto = clientDtos[0];
        if (firstDto is! Map<String, dynamic>) {
          throw const ServerException('Formato de cliente inválido');
        }
        return CustomerModel.fromJson(firstDto);
      } else {
        throw ServerException(
          'Error al obtener cliente',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e);
    } catch (e) {
      if (e is ServerException || e is NetworkException) rethrow;
      throw const ServerException('Error inesperado al obtener cliente.');
    }
  }

  @override
  Future<CustomerModel> createCustomer(CustomerModel customer) async {
    try {
      final response = await dioClient.post(
        AppConfig.customerServiceUrl,
        data: customer.toJson(),
      );

      if (_isSuccess(response.statusCode)) {
        if (response.statusCode == 204 || response.data == null) {
          throw const ServerException(
            'Error al crear cliente: respuesta inesperada del servidor',
          );
        }
        final data = response.data;
        if (data is Map<String, dynamic>) {
          final clientDtos = data['clientDtos'] as List?;
          final list = clientDtos ?? <dynamic>[];
          final dtos = list.whereType<Map<String, dynamic>>();
          if (dtos.isEmpty) {
            throw const ServerException('Respuesta inválida al crear cliente');
          }
          return CustomerModel.fromJson(dtos.first);
        }
        throw const ServerException(
          'Error al crear cliente: respuesta inesperada del servidor',
        );
      } else {
        throw ServerException(
          'Error al crear cliente',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e);
    }
  }

  @override
  Future<CustomerModel> updateCustomer(int id, CustomerModel customer) async {
    try {
      final response = await dioClient.patch(
        '${AppConfig.customerServiceUrl}/$id',
        data: customer.toJson(),
      );

      if (_isSuccess(response.statusCode)) {
        return await getCustomerById(id);
      } else {
        throw ServerException(
          'Error al actualizar cliente',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e);
    }
  }

  @override
  Future<void> deleteCustomer(int id) async {
    try {
      final response = await dioClient.delete(
        '${AppConfig.customerServiceUrl}/$id',
      );

      if (!_isSuccess(response.statusCode)) {
        throw ServerException(
          'Error al eliminar cliente',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _throwDio(e);
    }
  }

  Never _throwDio(DioException e) => handleDioException(e);
}

