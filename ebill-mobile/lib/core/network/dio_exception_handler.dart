import 'dart:io';

import 'package:dio/dio.dart';

import '../error/exception.dart';

/// Shared user-facing Spanish messages for Dio errors (keep in sync with `error_interceptor.dart`).
const String kDioMsgTimeout =
    'Tiempo de espera agotado. Intente de nuevo.';
const String kDioMsgConnectionError = 'Error de conexión con el servidor.';
const String kDioMsgNoInternet = 'Sin conexión a internet';
const String kDioMsgUnknownConnection = 'Error de conexión desconocido.';
const String kDioMsgUnexpectedServer = 'Error inesperado del servidor.';
const String kDioMsgRequestCancelled = 'Solicitud cancelada.';
const String kDioMsgBadCertificate = 'Certificado de seguridad inválido.';

/// Mensaje cuando el cuerpo HTTP está vacío y el código es 401 (típico: JWT no validado por ese microservicio).
const String kDioMsgUnauthorizedEmptyBody =
    'No autorizado (401). Este servicio rechazó el token. En el servidor, el microservicio debe usar la misma clave/validación JWT que el servicio de autenticación.';

/// Parses error text from JSON bodies or plain-string responses.
String extractBackendErrorMessage(dynamic data) {
  if (data == null) return '';
  if (data is String) {
    final s = data.trim();
    return s.isEmpty ? '' : s;
  }
  if (data is Map) {
    for (final key in ['message', 'error', 'detail', 'error_description', 'title']) {
      final v = data[key];
      if (v is String && v.trim().isNotEmpty) return v.trim();
    }
    final errors = data['errors'];
    if (errors is Map) {
      final map = Map<String, dynamic>.from(errors);
      final fieldMessages = map.entries
          .where((e) => e.value is List && (e.value as List).isNotEmpty)
          .map((e) => '${e.key}: ${(e.value as List).first}')
          .join(', ');
      if (fieldMessages.isNotEmpty) return fieldMessages;
    }
    if (errors is List && errors.isNotEmpty) {
      final first = errors.first;
      if (first is Map) {
        final dm = first['defaultMessage'];
        if (dm is String && dm.trim().isNotEmpty) return dm.trim();
      }
    }
  }
  return '';
}

/// Cuando no hay `message` en el JSON, usa el código HTTP para un texto útil.
String fallbackMessageForHttpStatus(int? statusCode) {
  switch (statusCode) {
    case 401:
      return kDioMsgUnauthorizedEmptyBody;
    case 403:
      return 'Acceso denegado (403).';
    case 404:
      return 'Recurso no encontrado (404).';
    case 408:
      return 'Tiempo de espera del servidor agotado (408).';
    case 429:
      return 'Demasiadas solicitudes. Intente más tarde (429).';
    case 502:
    case 503:
    case 504:
      return 'Servicio no disponible temporalmente. Intente más tarde.';
    default:
      return kDioMsgUnexpectedServer;
  }
}

Never handleDioException(DioException e) {
  switch (e.type) {
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
      if (e.error is SocketException) {
        throw const NetworkException(kDioMsgNoInternet);
      }
      // Dio puede marcar como `unknown` respuestas HTTP con cuerpo (p. ej. 500 con JSON);
      // si hay [response], tratar como error de servidor con mensaje del backend.
      if (e.response != null) {
        final backendMsg = extractBackendErrorMessage(e.response?.data);
        final status = e.response?.statusCode;
        throw ServerException(
          backendMsg.isNotEmpty ? backendMsg : fallbackMessageForHttpStatus(status),
          statusCode: status,
        );
      }
      throw const ServerException(kDioMsgUnknownConnection);
    case DioExceptionType.badResponse:
      final backendMsg = extractBackendErrorMessage(e.response?.data);
      final status = e.response?.statusCode;
      throw ServerException(
        backendMsg.isNotEmpty ? backendMsg : fallbackMessageForHttpStatus(status),
        statusCode: status,
      );
  }
}
