import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../core/services/biometric_service.dart';
import '../../../core/storage/storage_service.dart';

part 'settings_event.dart';
part 'settings_state.dart';

class SettingsBloc extends Bloc<SettingsEvent, SettingsState> {
  SettingsBloc(this._storage, this._biometric)
      : super(SettingsState.initial(_storage)) {
    on<SettingsThemeModeChanged>(_onThemeModeChanged);
    on<SettingsNotificationsChanged>(_onNotificationsChanged);
    on<SettingsBiometricChanged>(_onBiometricChanged);
    on<SettingsCheckBiometricAvailability>(_onCheckBiometric);
  }

  final StorageService _storage;
  final BiometricService _biometric;

  Future<void> _onThemeModeChanged(
    SettingsThemeModeChanged event,
    Emitter<SettingsState> emit,
  ) async {
    final s = switch (event.mode) {
      ThemeMode.light => 'light',
      ThemeMode.dark => 'dark',
      ThemeMode.system => 'system',
    };
    await _storage.saveThemeMode(s);
    emit(state.copyWith(themeMode: event.mode));
  }

  Future<void> _onNotificationsChanged(
    SettingsNotificationsChanged event,
    Emitter<SettingsState> emit,
  ) async {
    await _storage.saveNotificationsEnabled(event.enabled);
    emit(state.copyWith(notificationsEnabled: event.enabled));
  }

  Future<void> _onBiometricChanged(
    SettingsBiometricChanged event,
    Emitter<SettingsState> emit,
  ) async {
    await _biometric.setBiometricEnabled(event.enabled);
    emit(state.copyWith(biometricEnabled: event.enabled));
  }

  Future<void> _onCheckBiometric(
    SettingsCheckBiometricAvailability event,
    Emitter<SettingsState> emit,
  ) async {
    final available = await _biometric.isAvailable();
    emit(
      state.copyWith(
        biometricAvailable: available,
        biometricEnabled: _biometric.isBiometricEnabled,
      ),
    );
  }
}
