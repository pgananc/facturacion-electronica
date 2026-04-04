import 'dart:async';

import 'package:flutter/foundation.dart';

import '../bloc/auth/auth_bloc.dart';

/// Notifies [GoRouter] when [AuthBloc] state changes so [redirect] runs again.
class AuthRefreshNotifier extends ChangeNotifier {
  AuthRefreshNotifier(this._bloc) {
    _subscription = _bloc.stream.listen((_) => notifyListeners());
  }

  final AuthBloc _bloc;
  late final StreamSubscription<AuthState> _subscription;

  @override
  void dispose() {
    unawaited(_subscription.cancel());
    super.dispose();
  }
}
