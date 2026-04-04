import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../domain/entities/emission_session.dart';
import '../config/app_config.dart';

/// Local persistence: tokens and sensitive profile data in secure storage
/// (with in-memory cache for sync access), non-sensitive keys in [SharedPreferences].
class StorageService {
  StorageService(this._prefs, this._secure);

  static const String _keyUserData = 'secure_user_data';
  static const String _keySelectedCompany = 'secure_selected_company';
  static const String _keyCompanyIdentification = 'secure_company_identification';
  static const String _keyEmissionSessionJson = 'secure_emission_session_json';
  static const String _keyEmissionSessionSelected = 'secure_emission_session_selected';

  final SharedPreferences _prefs;
  final FlutterSecureStorage _secure;

  String? _accessTokenCache;
  String? _refreshTokenCache;
  String? _userDataCache;
  String? _selectedCompanyCache;
  String? _companyIdentificationCache;
  String? _emissionSessionJsonCache;
  bool _emissionSessionSelectedCache = false;

  /// Load tokens from secure storage (and migrate legacy values from [SharedPreferences]).
  ///
  /// Wrapped in try/catch so a corrupted keystore never crashes startup.
  Future<void> hydrateFromPersistence() async {
    try {
      _accessTokenCache = await _secure.read(key: AppConfig.tokenKey);
      _refreshTokenCache = await _secure.read(key: AppConfig.refreshTokenKey);

      final legacyToken = _prefs.getString(AppConfig.tokenKey);
      if ((_accessTokenCache == null || _accessTokenCache!.isEmpty) &&
          legacyToken != null &&
          legacyToken.isNotEmpty) {
        await _secure.write(key: AppConfig.tokenKey, value: legacyToken);
        _accessTokenCache = legacyToken;
        await _prefs.remove(AppConfig.tokenKey);
      }

      final legacyRefresh = _prefs.getString(AppConfig.refreshTokenKey);
      if ((_refreshTokenCache == null || _refreshTokenCache!.isEmpty) &&
          legacyRefresh != null &&
          legacyRefresh.isNotEmpty) {
        await _secure.write(key: AppConfig.refreshTokenKey, value: legacyRefresh);
        _refreshTokenCache = legacyRefresh;
        await _prefs.remove(AppConfig.refreshTokenKey);
      }

      _userDataCache = await _secure.read(key: _keyUserData);
      if (_userDataCache == null || _userDataCache!.isEmpty) {
        final legacyUser = _prefs.getString(AppConfig.userKey);
        if (legacyUser != null && legacyUser.isNotEmpty) {
          await _secure.write(key: _keyUserData, value: legacyUser);
          _userDataCache = legacyUser;
          await _prefs.remove(AppConfig.userKey);
        }
      }

      _selectedCompanyCache = await _secure.read(key: _keySelectedCompany);
      if (_selectedCompanyCache == null || _selectedCompanyCache!.isEmpty) {
        final legacy = _prefs.getString(AppConfig.companyKey);
        if (legacy != null && legacy.isNotEmpty) {
          await _secure.write(key: _keySelectedCompany, value: legacy);
          _selectedCompanyCache = legacy;
          await _prefs.remove(AppConfig.companyKey);
        }
      }

      _companyIdentificationCache = await _secure.read(key: _keyCompanyIdentification);
      if (_companyIdentificationCache == null || _companyIdentificationCache!.isEmpty) {
        final legacy = _prefs.getString(AppConfig.companyIdentificationKey);
        if (legacy != null && legacy.isNotEmpty) {
          await _secure.write(key: _keyCompanyIdentification, value: legacy);
          _companyIdentificationCache = legacy;
          await _prefs.remove(AppConfig.companyIdentificationKey);
        }
      }

      _emissionSessionJsonCache = await _secure.read(key: _keyEmissionSessionJson);
      final selRaw = await _secure.read(key: _keyEmissionSessionSelected);
      if (selRaw != null) {
        _emissionSessionSelectedCache = selRaw == 'true';
      } else {
        final legacyJson = _prefs.getString(AppConfig.emissionPointKey);
        final legacySel = _prefs.getBool(AppConfig.emissionPointSelectedKey) ?? false;
        if (legacyJson != null && legacyJson.isNotEmpty) {
          await _secure.write(key: _keyEmissionSessionJson, value: legacyJson);
          await _secure.write(
            key: _keyEmissionSessionSelected,
            value: legacySel ? 'true' : 'false',
          );
          _emissionSessionJsonCache = legacyJson;
          _emissionSessionSelectedCache = legacySel;
          await _prefs.remove(AppConfig.emissionPointKey);
          await _prefs.remove(AppConfig.emissionPointSelectedKey);
        }
      }
    } catch (_) {
      _accessTokenCache = null;
      _refreshTokenCache = null;
      _userDataCache = null;
      _selectedCompanyCache = null;
      _companyIdentificationCache = null;
      _emissionSessionJsonCache = null;
      _emissionSessionSelectedCache = false;
    }
  }

  // Token Management (sync reads use post-hydrate cache)
  Future<void> saveToken(String token) async {
    _accessTokenCache = token;
    try {
      await _secure.write(key: AppConfig.tokenKey, value: token);
    } catch (e) {
      _accessTokenCache = null;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist access token: $e');
      }
      rethrow;
    }
  }

  String? getToken() => _accessTokenCache;

  Future<void> clearToken() async {
    _accessTokenCache = null;
    try {
      await _secure.delete(key: AppConfig.tokenKey);
    } catch (e) {
      if (kDebugMode) {
        debugPrint('StorageService: delete failed for token key: $e');
      }
    }
  }

  Future<void> saveRefreshToken(String refreshToken) async {
    _refreshTokenCache = refreshToken;
    try {
      await _secure.write(key: AppConfig.refreshTokenKey, value: refreshToken);
    } catch (e) {
      _refreshTokenCache = null;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist refresh token: $e');
      }
      rethrow;
    }
  }

  String? getRefreshToken() => _refreshTokenCache;

  Future<void> clearRefreshToken() async {
    _refreshTokenCache = null;
    try {
      await _secure.delete(key: AppConfig.refreshTokenKey);
    } catch (e) {
      if (kDebugMode) {
        debugPrint('StorageService: delete failed for refresh token key: $e');
      }
    }
  }

  // User Data (secure storage + cache)
  Future<void> saveUserData(String userData) async {
    try {
      _userDataCache = userData;
      await _secure.write(key: _keyUserData, value: userData);
    } catch (e) {
      _userDataCache = null;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist user data: $e');
      }
      rethrow;
    }
  }

  Future<String?> getUserData() async {
    if (_userDataCache != null) return _userDataCache;
    final v = await _secure.read(key: _keyUserData);
    _userDataCache = v;
    return v;
  }

  // Company Data
  Future<void> saveSelectedCompany(String companyId) async {
    try {
      _selectedCompanyCache = companyId;
      await _secure.write(key: _keySelectedCompany, value: companyId);
    } catch (e) {
      _selectedCompanyCache = null;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist selected company: $e');
      }
      rethrow;
    }
  }

  Future<String?> getSelectedCompany() async {
    if (_selectedCompanyCache != null) return _selectedCompanyCache;
    final v = await _secure.read(key: _keySelectedCompany);
    _selectedCompanyCache = v;
    return v;
  }

  Future<void> saveCompanyIdentification(String identification) async {
    try {
      _companyIdentificationCache = identification;
      await _secure.write(key: _keyCompanyIdentification, value: identification);
    } catch (e) {
      _companyIdentificationCache = null;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist company identification: $e');
      }
      rethrow;
    }
  }

  String? getCompanyIdentification() => _companyIdentificationCache;

  Future<void> saveEmissionSession(EmissionSession session) async {
    try {
      final json = jsonEncode(session.toJson());
      _emissionSessionJsonCache = json;
      _emissionSessionSelectedCache = true;
      await _secure.write(key: _keyEmissionSessionJson, value: json);
      await _secure.write(key: _keyEmissionSessionSelected, value: 'true');
    } catch (e) {
      _emissionSessionJsonCache = null;
      _emissionSessionSelectedCache = false;
      if (kDebugMode) {
        debugPrint('StorageService: failed to persist emission session: $e');
      }
      rethrow;
    }
  }

  EmissionSession? getEmissionSession() {
    final raw = _emissionSessionJsonCache;
    if (raw == null || raw.isEmpty) {
      _emissionSessionSelectedCache = false;
      return null;
    }
    try {
      final m = jsonDecode(raw) as Map<String, dynamic>;
      return EmissionSession.fromJson(m);
    } catch (_) {
      _emissionSessionSelectedCache = false;
      return null;
    }
  }

  bool get isEmissionSessionSelected => _emissionSessionSelectedCache;

  Future<void> clearEmissionSession() async {
    _emissionSessionJsonCache = null;
    _emissionSessionSelectedCache = false;
    try {
      await _secure.delete(key: _keyEmissionSessionJson);
      await _secure.delete(key: _keyEmissionSessionSelected);
    } catch (e) {
      if (kDebugMode) {
        debugPrint('StorageService: delete failed for emission session keys: $e');
      }
    }
  }

  /// `system` | `light` | `dark`
  Future<void> saveThemeMode(String mode) async {
    await _prefs.setString(AppConfig.themeModeKey, mode);
  }

  String getThemeMode() {
    return _prefs.getString(AppConfig.themeModeKey) ?? 'system';
  }

  Future<void> saveNotificationsEnabled(bool enabled) async {
    await _prefs.setBool(AppConfig.notificationsEnabledKey, enabled);
  }

  bool getNotificationsEnabled() {
    return _prefs.getBool(AppConfig.notificationsEnabledKey) ?? true;
  }

  bool get isBiometricEnabled =>
      _prefs.getBool(AppConfig.biometricEnabledKey) ?? false;

  Future<void> setBiometricEnabled(bool value) async {
    await _prefs.setBool(AppConfig.biometricEnabledKey, value);
  }

  bool get pendingBiometricOffer =>
      _prefs.getBool(AppConfig.pendingBiometricOfferKey) ?? false;

  Future<void> setPendingBiometricOffer(bool value) async {
    await _prefs.setBool(AppConfig.pendingBiometricOfferKey, value);
  }

  Future<void> clearPendingBiometricOffer() async {
    await _prefs.remove(AppConfig.pendingBiometricOfferKey);
  }

  Future<void> clearAll() async {
    _accessTokenCache = null;
    _refreshTokenCache = null;
    _userDataCache = null;
    _selectedCompanyCache = null;
    _companyIdentificationCache = null;
    _emissionSessionJsonCache = null;
    _emissionSessionSelectedCache = false;
    try {
      await _secure.delete(key: AppConfig.tokenKey);
      await _secure.delete(key: AppConfig.refreshTokenKey);
      await _secure.delete(key: _keyUserData);
      await _secure.delete(key: _keySelectedCompany);
      await _secure.delete(key: _keyCompanyIdentification);
      await _secure.delete(key: _keyEmissionSessionJson);
      await _secure.delete(key: _keyEmissionSessionSelected);
    } catch (e) {
      if (kDebugMode) {
        debugPrint('StorageService.clearAll secure delete failed: $e');
      }
    }
    await _prefs.remove(AppConfig.tokenKey);
    await _prefs.remove(AppConfig.refreshTokenKey);
    await _prefs.remove(AppConfig.userKey);
    await _prefs.remove(AppConfig.companyKey);
    await _prefs.remove(AppConfig.companyIdentificationKey);
    await _prefs.remove(AppConfig.emissionPointKey);
    await _prefs.remove(AppConfig.emissionPointSelectedKey);
  }

  Future<void> remove(String key) async {
    await _prefs.remove(key);
  }
}
