import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:logger/logger.dart';

class LoggingInterceptor extends Interceptor {
  final Logger logger;

  static const _sensitivePathSegments = ['/login', '/password'];

  LoggingInterceptor(this.logger);

  bool _isSensitivePath(String path) {
    return _sensitivePathSegments.any((s) => path.contains(s));
  }

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    logger.d(
      'REQUEST[${options.method}] => PATH: ${options.path}',
    );
    final sanitized = Map<String, dynamic>.from(options.headers);
    if (sanitized.containsKey('Authorization')) {
      sanitized['Authorization'] = 'Bearer ***';
    }
    logger.d('Headers: $sanitized');
    if (options.data != null) {
      if (_isSensitivePath(options.path)) {
        logger.d('Data: [REDACTED]');
      } else {
        logger.d('Data: ${options.data}');
      }
    }
    if (options.queryParameters.isNotEmpty) {
      logger.d('Query Parameters: ${options.queryParameters}');
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    logger.i(
      'RESPONSE[${response.statusCode}] => PATH: ${response.requestOptions.path}',
    );
    if (_isSensitivePath(response.requestOptions.path)) {
      logger.d('Data: [REDACTED]');
    } else {
      final bodyStr = response.data?.toString() ?? '';
      final truncated = bodyStr.length > 200
          ? '${bodyStr.substring(0, 200)}...[truncated]'
          : bodyStr;
      debugPrint('Response Body: $truncated');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    logger.e(
      'ERROR[${err.response?.statusCode ?? 'NO_STATUS'}] => '
      '${err.requestOptions.method} ${err.requestOptions.path}',
    );

    if (err.response != null) {
      logger.e('Response Status Code: ${err.response?.statusCode}');
      logger.e('Response Status Message: ${err.response?.statusMessage}');

      if (err.response?.data != null) {
        if (_isSensitivePath(err.requestOptions.path)) {
          logger.e('Response Data: [REDACTED]');
        } else {
          logger.e('Response Data: ${err.response?.data}');
        }
      }
    } else {
      logger.e('Error Type: ${err.type}');
      logger.e('Error Message: ${err.message}');
    }

    handler.next(err);
  }
}
