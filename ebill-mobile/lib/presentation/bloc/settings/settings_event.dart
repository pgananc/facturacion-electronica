part of 'settings_bloc.dart';

abstract class SettingsEvent extends Equatable {
  const SettingsEvent();

  @override
  List<Object?> get props => [];
}

class SettingsThemeModeChanged extends SettingsEvent {
  const SettingsThemeModeChanged(this.mode);

  final ThemeMode mode;

  @override
  List<Object?> get props => [mode];
}

class SettingsNotificationsChanged extends SettingsEvent {
  const SettingsNotificationsChanged(this.enabled);

  final bool enabled;

  @override
  List<Object?> get props => [enabled];
}

class SettingsBiometricChanged extends SettingsEvent {
  const SettingsBiometricChanged(this.enabled);

  final bool enabled;

  @override
  List<Object?> get props => [enabled];
}

class SettingsCheckBiometricAvailability extends SettingsEvent {
  const SettingsCheckBiometricAvailability();
}
