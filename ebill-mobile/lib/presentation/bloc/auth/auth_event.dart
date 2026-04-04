part of 'auth_bloc.dart';

abstract class AuthEvent extends Equatable {
  const AuthEvent();

  @override
  List<Object> get props => [];
}

class LoginEvent extends AuthEvent {
  final String username;
  final String password;

  const LoginEvent(this.username, this.password);

  @override
  List<Object> get props => [username];
}

class LogoutEvent extends AuthEvent {
  const LogoutEvent();
}

class CheckAuthEvent extends AuthEvent {
  const CheckAuthEvent();
}

/// Restaura la sesión tras validar Face ID / huella (sesión y token ya guardados).
class BiometricAuthEvent extends AuthEvent {
  const BiometricAuthEvent();
}

/// Fired by [AuthInterceptor] when token refresh fails and storage is cleared.
class SessionExpiredEvent extends AuthEvent {
  const SessionExpiredEvent();
}

/// Updates the authenticated user (e.g. after loading profile from API).
class SessionUserUpdatedEvent extends AuthEvent {
  final User user;

  const SessionUserUpdatedEvent(this.user);

  @override
  List<Object> get props => [user];
}

