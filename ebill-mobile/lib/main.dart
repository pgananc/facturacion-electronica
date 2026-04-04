import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'core/config/app_config.dart';
import 'core/di/injection.dart';
import 'core/network/dio_client.dart';
import 'core/services/inactivity_service.dart';
import 'core/storage/storage_service.dart';
import 'presentation/bloc/auth/auth_bloc.dart';
import 'presentation/bloc/credit_note/credit_note_bloc.dart';
import 'presentation/bloc/customer/customer_bloc.dart';
import 'presentation/bloc/invoice/invoice_bloc.dart';
import 'presentation/bloc/proforma/proforma_bloc.dart';
import 'presentation/bloc/product/product_bloc.dart';
import 'presentation/bloc/referral_guide/referral_guide_bloc.dart';
import 'presentation/bloc/report/report_bloc.dart';
import 'presentation/bloc/settings/settings_bloc.dart';
import 'presentation/di/bloc_module.dart';
import 'presentation/theme/app_theme.dart';
import 'presentation/routes/app_router.dart';
import 'presentation/widgets/biometric_offer_listener.dart';
import 'presentation/widgets/session_heartbeat.dart';

void main() {
  runZonedGuarded(() async {
    WidgetsFlutterBinding.ensureInitialized();

    FlutterError.onError = (details) {
      if (kDebugMode) {
        FlutterError.presentError(details);
      }
    };

    try {
      await initializeDateFormatting('es');
      await configureDependencies();
      registerBlocs();
      setupAuthListener();
      await AppConfig.initialize();

      void expireSession() {
        getIt<AuthBloc>().add(const SessionExpiredEvent());
      }

      getIt<DioClient>().onSessionExpired = expireSession;
      getIt<InactivityService>().onSessionExpired = expireSession;

      getIt<AuthBloc>().add(const CheckAuthEvent());

      runApp(const MyApp());
    } catch (e) {
      runApp(
        MaterialApp(
          theme: ThemeData.light(useMaterial3: true),
          home: Scaffold(
            body: Center(
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Text(
                  kDebugMode
                      ? 'Error de inicio: $e'
                      : 'Ocurrió un error al iniciar la aplicación. Por favor, reinténtelo.',
                  style: const TextStyle(color: Colors.red, fontSize: 16),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
          ),
        ),
      );
    }
  }, (error, stack) {
    if (kDebugMode) {
      debugPrint('Uncaught error: $error');
      debugPrint('$stack');
    }
  });
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<AuthBloc>.value(value: getIt<AuthBloc>()),
        BlocProvider<SettingsBloc>.value(value: getIt<SettingsBloc>()),
        BlocProvider<InvoiceBloc>.value(value: getIt<InvoiceBloc>()),
        BlocProvider<CreditNoteBloc>.value(value: getIt<CreditNoteBloc>()),
        BlocProvider<ProformaBloc>.value(value: getIt<ProformaBloc>()),
        BlocProvider<CustomerBloc>.value(value: getIt<CustomerBloc>()),
        BlocProvider<ProductBloc>.value(value: getIt<ProductBloc>()),
        BlocProvider<ReportBloc>.value(value: getIt<ReportBloc>()),
        BlocProvider<ReferralGuideBloc>.value(value: getIt<ReferralGuideBloc>()),
      ],
      child: BlocSelector<SettingsBloc, SettingsState, ThemeMode>(
        selector: (state) => state.themeMode,
        builder: (context, themeMode) {
          return BlocSelector<AuthBloc, AuthState, bool>(
            selector: (state) => state is AuthAuthenticated,
            builder: (context, isAuthenticated) {
              return _buildApp(context, themeMode, isAuthenticated);
            },
          );
        },
      ),
    );
  }

  Widget _buildApp(
    BuildContext context,
    ThemeMode themeMode,
    bool isAuthenticated,
  ) {
    final inactivity = getIt<InactivityService>();
    final isFullyAuthenticated =
        isAuthenticated && getIt<StorageService>().isEmissionSessionSelected;

    if (isFullyAuthenticated) {
      inactivity.start();
    } else {
      inactivity.stop();
    }

    final app = Listener(
      behavior: HitTestBehavior.translucent,
      onPointerDown: (_) => inactivity.resetTimer(),
      onPointerMove: (_) => inactivity.resetTimer(),
      child: MaterialApp.router(
        title: 'Wallparisoft',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        darkTheme: AppTheme.darkTheme,
        themeMode: themeMode,
        routerConfig: AppRouter.router,
        builder: (context, child) {
          return BiometricOfferListener(child: child ?? const SizedBox.shrink());
        },
      ),
    );

    if (isFullyAuthenticated) {
      return SessionHeartbeat(child: app);
    }
    return app;
  }
}
