import 'package:dio/dio.dart';

/// Stub for web — certificate pinning uses `dart:io` and is not applied here.
class CertificatePinner {
  CertificatePinner._();

  static bool get isEnabled => false;

  static Object? createSecurityContext() => null;

  static void configure(Object client) {}
}

/// No-op on web; native builds use [certificate_pinner.dart].
void applyDioCertificatePinning(Dio dio) {}
