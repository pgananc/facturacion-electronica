import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:ebill_mobile/core/config/app_config.dart';
import 'package:ebill_mobile/core/di/injection.dart';
import 'package:ebill_mobile/main.dart';
import 'package:ebill_mobile/presentation/di/bloc_module.dart';
import 'package:ebill_mobile/presentation/routes/app_router.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() {
    const channel =
        MethodChannel('plugins.it_nomads.com/flutter_secure_storage');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      switch (call.method) {
        case 'read':
          return null;
        case 'readAll':
          return <String, String>{};
        case 'write':
        case 'delete':
        case 'deleteAll':
          return null;
        default:
          return null;
      }
    });
  });

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    AppRouter.resetRouterForTesting();
    await getIt.reset();
    await configureDependencies();
    registerBlocs();
    await AppConfig.initialize();
  });

  testWidgets('Login screen builds', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pump();
    expect(find.text('Iniciar Sesión'), findsOneWidget);
  });
}
