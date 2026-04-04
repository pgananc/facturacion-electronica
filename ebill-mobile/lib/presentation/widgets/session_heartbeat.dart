import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:logger/logger.dart';

import '../../core/di/injection.dart';
import '../../core/error/failures.dart';
import '../../domain/repositories/invoice_session_repository.dart';
import '../bloc/auth/auth_bloc.dart';

class SessionHeartbeat extends StatefulWidget {
  const SessionHeartbeat({
    super.key,
    this.interval = const Duration(minutes: 2),
    required this.child,
  });

  final Duration interval;
  final Widget child;

  @override
  State<SessionHeartbeat> createState() => _SessionHeartbeatState();
}

class _SessionHeartbeatState extends State<SessionHeartbeat>
    with WidgetsBindingObserver {
  Timer? _timer;
  bool _busy = false;
  final _repo = getIt<InvoiceSessionRepository>();
  final _logger = Logger();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _startTimer();
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(widget.interval, (_) => _sendHeartbeat());
  }

  Future<void> _sendHeartbeat() async {
    if (_busy) return;
    _busy = true;
    try {
      final result = await _repo.heartbeat();
      result.fold(
        (failure) {
          if (kDebugMode) {
            if (failure is ServerFailure) {
              debugPrint('Heartbeat server error: ${failure.message}');
            } else {
              _logger.w('Heartbeat failed: ${failure.message}');
            }
          }
          final isSessionGone = failure is AuthFailure;
          if (isSessionGone && mounted) {
            context.read<AuthBloc>().add(const SessionExpiredEvent());
          }
        },
        (_) {},
      );
    } finally {
      _busy = false;
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive) {
      _timer?.cancel();
      _timer = null;
    } else if (state == AppLifecycleState.resumed) {
      _startTimer();
      _sendHeartbeat();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
