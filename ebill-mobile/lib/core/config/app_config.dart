import 'package:flutter/foundation.dart';

import '../network/certificate_pinner.dart'
    if (dart.library.html) '../network/certificate_pinner_stub.dart';

enum Environment { dev, prod }

class AppConfig {
  static const String _env = String.fromEnvironment('ENV', defaultValue: 'prod');
  static const String _devHost = String.fromEnvironment('DEV_HOST', defaultValue: 'localhost');

  static Environment get environment =>
      _env == 'prod' ? Environment.prod : Environment.dev;

  static bool get isProduction => environment == Environment.prod;

  // ── Dev hosts (use DEV_HOST for physical devices, localhost for simulators) ──
  static String get _devAuth => 'http://$_devHost:8280';
  static String get _devCustomer => 'http://$_devHost:8180';
  static String get _devProduct => 'http://$_devHost:8380';
  static String get _devBilling => 'http://$_devHost:8480';
  static String get _devReport => 'http://$_devHost:8580';

  // ── Prod hosts ──
  /// Certificate pinning is configured in [CertificatePinner].
  /// Enable by adding SHA-256 pins before production release.
  static const String _prodAuth = 'https://auth.wallparisoft.com';
  static const String _prodCustomer = 'https://customer.wallparisoft.com';
  static const String _prodProduct = 'https://product.wallparisoft.com';
  static const String _prodBilling = 'https://billing.wallparisoft.com';
  static const String _prodReport = 'https://report.wallparisoft.com';

  // ── Resolved URLs ──
  static String get baseUrl => isProduction ? _prodAuth : _devAuth;

  static String get authServiceUrl =>
      '${isProduction ? _prodAuth : _devAuth}/api';

  static String get customerServiceUrl =>
      '${isProduction ? _prodCustomer : _devCustomer}/api/client';

  static String get companyServiceUrl =>
      '${isProduction ? _prodCustomer : _devCustomer}/api/company';

  static String get productServiceUrl =>
      '${isProduction ? _prodProduct : _devProduct}/api/product';

  static String get billingServiceUrl =>
      '${isProduction ? _prodBilling : _devBilling}/api/invoice';

  static String get reportServiceUrl =>
      '${isProduction ? _prodReport : _devReport}/api/report';

  static String get creditNoteServiceUrl =>
      '${isProduction ? _prodBilling : _devBilling}/api/credit-notes';

  static String get proformaServiceUrl =>
      '${isProduction ? _prodBilling : _devBilling}/api/proforma';

  static String get referralGuideServiceUrl =>
      '${isProduction ? _prodBilling : _devBilling}/api/guides';

  // App Configuration
  static const String appName = 'Wallparisoft';
  static const String appVersion = '1.0.0';

  // Storage Keys
  static const String tokenKey = 'auth_token';
  static const String refreshTokenKey = 'refresh_token';
  static const String userKey = 'user_data';
  static const String companyKey = 'selected_company';
  static const String companyIdentificationKey = 'company_identification';
  static const String emissionPointKey = 'emission_point_json';
  static const String emissionPointSelectedKey = 'emission_point_selected';
  static const String themeModeKey = 'app_theme_mode';
  static const String notificationsEnabledKey = 'app_notifications_enabled';
  static const String biometricEnabledKey = 'biometric_enabled';
  static const String pendingBiometricOfferKey = 'pending_biometric_offer';

  // Session Configuration
  static Duration get inactivityTimeout =>
      isProduction ? const Duration(minutes: 15) : const Duration(minutes: 30);

  // Network Configuration
  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 10);
  static const Duration sendTimeout = Duration(seconds: 15);

  /// Longer receive timeout for PDF/XML and other large binary downloads.
  static const Duration downloadReceiveTimeout = Duration(seconds: 60);

  static Future<void> initialize() async {
    if (kDebugMode) {
      debugPrint('AppConfig: ENV=$_env, baseUrl=$baseUrl');
    }
  }
}
