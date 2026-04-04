import 'dart:io';

import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:flutter/foundation.dart';

/// Certificate pinning for native platforms (iOS/Android/desktop).
///
/// Web builds use [certificate_pinner_stub.dart] via conditional import.
class CertificatePinner {
  CertificatePinner._();

  /// SHA-256 certificate pins for production API hosts (Subject Public Key Info,
  /// base64-encoded, as produced by the openssl pipeline in the comment below).
  /// Update these before each production release with current server certificate pins.
  ///
  /// To get a pin:
  /// `openssl s_client -connect HOST:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64`
  static const Map<String, List<String>> _pins = {
    // Example: 'api.wallparisoft.com': ['BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB='],
  };

  /// Returns true if pinning should be enforced.
  /// Only in release mode AND when pins are configured (empty map = kill switch).
  static bool get isEnabled => !kDebugMode && _pins.isNotEmpty;

  /// Optional trust store for production: pin to your server/CA certificate bytes.
  ///
  /// For real pinning, prefer bundling the server or CA PEM and using:
  /// ```dart
  /// final context = SecurityContext(withTrustedRoots: false);
  /// context.setTrustedCertificatesBytes(certBytes);
  /// return context;
  /// ```
  static SecurityContext? createSecurityContext() {
    if (!isEnabled) return null;
    // When ready for production, add server certificate:
    // final context = SecurityContext(withTrustedRoots: false);
    // context.setTrustedCertificatesBytes(certBytes);
    // return context;
    return null;
  }

  /// Configure certificate validation on an [HttpClient].
  ///
  /// [badCertificateCallback] runs when the chain fails default validation.
  /// Non-pinned hosts reject bad certificates (fail closed). Pinned hosts also
  /// reject until SPKI checks are implemented.
  static void configure(HttpClient client) {
    if (!isEnabled) return;

    client.badCertificateCallback =
        (X509Certificate cert, String host, int port) {
      final hostPins = _pins[host];
      if (hostPins == null || hostPins.isEmpty) {
        return false; // Fail closed: reject bad certificates for all hosts
      }
      // For pinned hosts, also reject until SPKI checks are implemented
      return false;
    };
  }
}

/// Applies native certificate pinning to [dio].
///
/// Call only when [CertificatePinner.isEnabled] is true.
void applyDioCertificatePinning(Dio dio) {
  dio.httpClientAdapter = IOHttpClientAdapter(
    createHttpClient: () {
      final client = HttpClient(
        context: CertificatePinner.createSecurityContext(),
      );
      CertificatePinner.configure(client);
      return client;
    },
  );
}
