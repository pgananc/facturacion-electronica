import 'package:flutter/foundation.dart';
import 'package:local_auth/local_auth.dart';

import '../storage/storage_service.dart';

class BiometricService {
  BiometricService(this._storage, this._localAuth);

  final StorageService _storage;
  final LocalAuthentication _localAuth;

  bool get isBiometricEnabled => _storage.isBiometricEnabled;

  Future<void> setBiometricEnabled(bool value) =>
      _storage.setBiometricEnabled(value);

  Future<bool> isAvailable() async {
    try {
      final can = await _localAuth.canCheckBiometrics;
      final supported = await _localAuth.isDeviceSupported();
      if (!can || !supported) {
        if (kDebugMode) {
          debugPrint('[BiometricService] canCheckBiometrics=$can, isDeviceSupported=$supported');
          final enrolled = await _localAuth.getAvailableBiometrics();
          debugPrint('[BiometricService] enrolledBiometrics=$enrolled');
        }
      }
      return can && supported;
    } catch (e) {
      if (kDebugMode) {
        debugPrint('[BiometricService] isAvailable error: $e');
      }
      return false;
    }
  }

  Future<bool> authenticate({required String localizedReason}) async {
    try {
      return await _localAuth.authenticate(
        localizedReason: localizedReason,
        biometricOnly: true,
      );
    } catch (e) {
      if (kDebugMode) {
        debugPrint('[BiometricService] authenticate error: $e');
      }
      return false;
    }
  }
}
