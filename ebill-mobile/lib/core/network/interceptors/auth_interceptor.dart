import 'dart:async';
import 'dart:ui';

import 'package:dio/dio.dart';

import '../certificate_pinner.dart'
    if (dart.library.html) '../certificate_pinner_stub.dart';
import '../../config/app_config.dart';
import '../../storage/storage_service.dart';

/// Interceptor that automatically adds Authorization header to requests
/// using the token stored in StorageService.
///
/// Uses dependency injection - StorageService must be provided in constructor,
/// ensuring it's always initialized before use.
class AuthInterceptor extends Interceptor {
  AuthInterceptor(this._storageService);

  final StorageService _storageService;
  VoidCallback? onSessionExpired;
  Completer<String?>? _refreshCompleter;

  Dio _createPinnedDio(BaseOptions options) {
    final dio = Dio(options);
    if (CertificatePinner.isEnabled) {
      applyDioCertificatePinning(dio);
    }
    return dio;
  }

  bool _isSuccess(int? status) =>
      status != null && status >= 200 && status < 300;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) {
    final path = options.uri.path;
    if (path.endsWith('/login') || path.endsWith('/login/refresh')) {
      handler.next(options);
      return;
    }

    final token = _storageService.getToken();

    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }

    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }

    final uriPath = err.requestOptions.uri.path;
    if (uriPath.contains('/login/refresh')) {
      unawaited(_storageService.clearAll());
      onSessionExpired?.call();
      handler.next(err);
      return;
    }

    final refreshToken = _storageService.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      handler.next(err);
      return;
    }

    if (_refreshCompleter != null) {
      unawaited(_awaitExistingRefresh(err, handler));
      return;
    }

    unawaited(_refreshAndRetry(err, handler, refreshToken));
  }

  Future<void> _awaitExistingRefresh(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final newToken = await _refreshCompleter?.future;
    if (newToken != null && newToken.isNotEmpty) {
      final headers = Map<String, dynamic>.from(err.requestOptions.headers);
      headers['Authorization'] = 'Bearer $newToken';
      try {
        final retry = await _createPinnedDio(
          BaseOptions(
            connectTimeout: AppConfig.connectTimeout,
            receiveTimeout: AppConfig.receiveTimeout,
            sendTimeout: AppConfig.sendTimeout,
          ),
        ).fetch<dynamic>(
          err.requestOptions.copyWith(headers: headers),
        );
        handler.resolve(retry);
        return;
      } catch (_) {
        handler.next(err);
        return;
      }
    }
    handler.next(err);
  }

  Future<void> _refreshAndRetry(
    DioException err,
    ErrorInterceptorHandler handler,
    String refreshToken,
  ) async {
    _refreshCompleter = Completer<String?>();
    try {
      final refreshDio = _createPinnedDio(
        BaseOptions(
          connectTimeout: AppConfig.connectTimeout,
          receiveTimeout: AppConfig.receiveTimeout,
          sendTimeout: AppConfig.sendTimeout,
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          },
        ),
      );
      final response = await refreshDio.post<Map<String, dynamic>>(
        '${AppConfig.authServiceUrl}/login/refresh',
        data: {'refreshToken': refreshToken},
      );

      if (!_isSuccess(response.statusCode) || response.data == null) {
        _refreshCompleter?.complete(null);
        _refreshCompleter = null;
        await _storageService.clearAll();
        onSessionExpired?.call();
        handler.next(err);
        return;
      }

      final data = response.data!;
      final tokenObj = data['token'];
      String? newAccess;
      String? newRefresh;
      if (tokenObj is Map<String, dynamic>) {
        final t = tokenObj['token'];
        if (t is String && t.isNotEmpty) {
          newAccess = t;
        }
        final r = tokenObj['refreshToken'] ?? tokenObj['refresh_token'];
        if (r is String && r.isNotEmpty) {
          newRefresh = r;
        }
      }

      if (newAccess == null || newAccess.isEmpty) {
        _refreshCompleter?.complete(null);
        _refreshCompleter = null;
        await _storageService.clearAll();
        onSessionExpired?.call();
        handler.next(err);
        return;
      }

      await _storageService.saveToken(newAccess);
      if (newRefresh != null && newRefresh.isNotEmpty) {
        await _storageService.saveRefreshToken(newRefresh);
      }

      _refreshCompleter?.complete(newAccess);

      final headers = Map<String, dynamic>.from(err.requestOptions.headers);
      headers['Authorization'] = 'Bearer $newAccess';
      try {
        final retry = await _createPinnedDio(
          BaseOptions(
            connectTimeout: AppConfig.connectTimeout,
            receiveTimeout: AppConfig.receiveTimeout,
            sendTimeout: AppConfig.sendTimeout,
          ),
        ).fetch<dynamic>(
          err.requestOptions.copyWith(headers: headers),
        );
        handler.resolve(retry);
      } catch (_) {
        handler.next(err);
      } finally {
        _refreshCompleter = null;
      }
    } catch (_) {
      _refreshCompleter?.complete(null);
      _refreshCompleter = null;
      await _storageService.clearAll();
      onSessionExpired?.call();
      handler.next(err);
    }
  }
}
