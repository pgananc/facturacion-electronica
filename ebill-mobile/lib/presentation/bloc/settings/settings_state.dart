part of 'settings_bloc.dart';

class SettingsState extends Equatable {
  const SettingsState({
    required this.themeMode,
    required this.notificationsEnabled,
    required this.biometricEnabled,
    required this.biometricAvailable,
  });

  factory SettingsState.initial(StorageService storage) {
    final modeStr = storage.getThemeMode();
    final mode = switch (modeStr) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
    return SettingsState(
      themeMode: mode,
      notificationsEnabled: storage.getNotificationsEnabled(),
      biometricEnabled: storage.isBiometricEnabled,
      biometricAvailable: false,
    );
  }

  final ThemeMode themeMode;
  final bool notificationsEnabled;
  final bool biometricEnabled;
  final bool biometricAvailable;

  SettingsState copyWith({
    ThemeMode? themeMode,
    bool? notificationsEnabled,
    bool? biometricEnabled,
    bool? biometricAvailable,
  }) {
    return SettingsState(
      themeMode: themeMode ?? this.themeMode,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
      biometricAvailable: biometricAvailable ?? this.biometricAvailable,
    );
  }

  @override
  List<Object?> get props => [
        themeMode,
        notificationsEnabled,
        biometricEnabled,
        biometricAvailable,
      ];
}
