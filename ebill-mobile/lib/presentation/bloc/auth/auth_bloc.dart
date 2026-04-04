import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../core/services/biometric_service.dart';
import '../../../core/storage/storage_service.dart';
import '../../../core/utils/jwt_payload.dart';
import '../../../domain/entities/user.dart';
import '../../../domain/repositories/auth_repository.dart';

part 'auth_event.dart';
part 'auth_state.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthRepository authRepository;
  final BiometricService biometricService;
  final StorageService storageService;

  AuthBloc(
    this.authRepository,
    this.biometricService,
    this.storageService,
  ) : super(AuthInitial()) {
    on<LoginEvent>(_onLogin);
    on<LogoutEvent>(_onLogout);
    on<CheckAuthEvent>(_onCheckAuth);
    on<BiometricAuthEvent>(_onBiometricAuth);
    on<SessionExpiredEvent>(_onSessionExpired);
    on<SessionUserUpdatedEvent>(_onSessionUserUpdated);
  }

  Future<void> _onSessionExpired(
    SessionExpiredEvent event,
    Emitter<AuthState> emit,
  ) async {
    await storageService.clearAll();
    emit(
      const AuthSessionExpired(
        'Tu sesión ha expirado. Inicia sesión nuevamente.',
      ),
    );
  }

  void _onSessionUserUpdated(
    SessionUserUpdatedEvent event,
    Emitter<AuthState> emit,
  ) {
    emit(AuthAuthenticated(event.user));
  }

  Future<void> _onLogin(LoginEvent event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final result = await authRepository.login(event.username, event.password);
      await result.fold(
        (failure) async {
          final errorMessage = failure.message.isNotEmpty
              ? failure.message
              : 'Error desconocido al iniciar sesión';
          emit(AuthError(errorMessage));
        },
        (user) async {
          await storageService.setPendingBiometricOffer(true);
          emit(AuthAuthenticated(user));
        },
      );
    } catch (_) {
      emit(const AuthError('Error inesperado al iniciar sesión. Intente nuevamente.'));
    }
  }

  Future<void> _onLogout(LogoutEvent event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final result = await authRepository.logout();
      result.fold(
        (failure) => emit(AuthUnauthenticated()),
        (_) => emit(AuthUnauthenticated()),
      );
    } catch (_) {
      emit(AuthUnauthenticated());
    }
  }

  Future<void> _onCheckAuth(
    CheckAuthEvent event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    final result = await authRepository.isAuthenticated();
    if (result.isLeft()) {
      emit(AuthUnauthenticated());
      return;
    }
    final isAuthenticated = result.getOrElse(() => false);
    if (!isAuthenticated) {
      emit(AuthUnauthenticated());
      return;
    }
    if (storageService.isBiometricEnabled) {
      final ok = await biometricService.authenticate(
        localizedReason: 'Desbloquee Wallparisoft para continuar',
      );
      if (!ok) {
        emit(AuthUnauthenticated());
        return;
      }
    }
    final userResult = await authRepository.getCurrentUser();
    userResult.fold(
      (_) => emit(AuthUnauthenticated()),
      (user) {
        if (user != null) {
          emit(AuthAuthenticated(user));
        } else {
          emit(AuthUnauthenticated());
        }
      },
    );
  }

  Future<void> _onBiometricAuth(
    BiometricAuthEvent event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    if (!storageService.isBiometricEnabled) {
      emit(const AuthError('La autenticación biométrica no está activada.'));
      return;
    }
    final token = storageService.getToken();
    if (token == null || token.isEmpty) {
      emit(const AuthError('No hay una sesión guardada.'));
      return;
    }
    final payload = JwtPayload.fromToken(token);
    if (payload == null || payload.isExpired()) {
      await storageService.clearAll();
      emit(AuthUnauthenticated());
      return;
    }
    final ok = await biometricService.authenticate(
      localizedReason: 'Desbloquee para continuar',
    );
    if (!ok) {
      emit(const AuthError('Autenticación cancelada.'));
      return;
    }
    final userResult = await authRepository.getCurrentUser();
    userResult.fold(
      (_) => emit(const AuthError('No se pudo restaurar la sesión.')),
      (user) {
        if (user != null) {
          emit(AuthAuthenticated(user));
        } else {
          emit(const AuthError('No se pudo restaurar la sesión.'));
        }
      },
    );
  }
}
