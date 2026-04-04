import 'dart:async';
import 'dart:ui';

import '../config/app_config.dart';

class InactivityService {
  Timer? _timer;
  bool _running = false;
  VoidCallback? onSessionExpired;

  void start() {
    if (_running) return;
    _running = true;
    _restartTimer();
  }

  void resetTimer() {
    if (!_running) return;
    _restartTimer();
  }

  void _restartTimer() {
    _timer?.cancel();
    _timer = Timer(AppConfig.inactivityTimeout, _onTimeout);
  }

  void _onTimeout() {
    _timer = null;
    onSessionExpired?.call();
  }

  void stop() {
    _running = false;
    _timer?.cancel();
    _timer = null;
  }

  void dispose() {
    stop();
    onSessionExpired = null;
  }
}
