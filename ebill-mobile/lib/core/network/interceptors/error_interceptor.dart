import 'dart:io';

import 'package:dio/dio.dart';

import '../../error/exception.dart';
import '../dio_exception_handler.dart';

class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Let 401s pass through untouched so AuthInterceptor can handle
    // token refresh (Dio error chain runs in reverse registration order).
    if (err.response?.statusCode == 401) {
      handler.next(err);
      return;
    }

    switch (err.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
      case DioExceptionType.sendTimeout:
        throw const NetworkException(kDioMsgTimeout);
      case DioExceptionType.connectionError:
        throw const NetworkException(kDioMsgConnectionError);
      case DioExceptionType.badCertificate:
        throw const NetworkException(kDioMsgBadCertificate);
      case DioExceptionType.cancel:
        throw const NetworkException(kDioMsgRequestCancelled);
      case DioExceptionType.unknown:
        if (err.error is SocketException) {
          throw const NetworkException(kDioMsgNoInternet);
        }
        if (err.response != null) {
          break;
        }
        throw const ServerException(kDioMsgUnknownConnection);
      case DioExceptionType.badResponse:
        break;
    }

    final backendMsg = extractBackendErrorMessage(err.response?.data);
    final status = err.response?.statusCode;
    throw ServerException(
      backendMsg.isNotEmpty ? backendMsg : fallbackMessageForHttpStatus(status),
      statusCode: status,
    );
  }
}
