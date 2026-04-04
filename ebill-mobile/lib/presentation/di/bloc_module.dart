import 'dart:async';

import '../../core/di/injection.dart';
import '../../core/services/biometric_service.dart';
import '../../core/storage/storage_service.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/repositories/credit_note_repository.dart';
import '../../domain/repositories/customer_repository.dart';
import '../../domain/repositories/invoice_repository.dart';
import '../../domain/repositories/proforma_repository.dart';
import '../../domain/repositories/product_repository.dart';
import '../../domain/repositories/referral_guide_repository.dart';
import '../../domain/repositories/report_repository.dart';
import '../bloc/auth/auth_bloc.dart';
import '../bloc/credit_note/credit_note_bloc.dart';
import '../bloc/customer/customer_bloc.dart';
import '../bloc/invoice/invoice_bloc.dart';
import '../bloc/proforma/proforma_bloc.dart';
import '../bloc/product/product_bloc.dart';
import '../bloc/referral_guide/referral_guide_bloc.dart';
import '../bloc/report/report_bloc.dart';
import '../bloc/settings/settings_bloc.dart';

void registerBlocs() {
  getIt.registerLazySingleton<AuthBloc>(
    () => AuthBloc(
          getIt<AuthRepository>(),
          getIt<BiometricService>(),
          getIt<StorageService>(),
        ),
  );
  getIt.registerLazySingleton<InvoiceBloc>(
    () => InvoiceBloc(getIt<InvoiceRepository>()),
  );
  getIt.registerLazySingleton<CreditNoteBloc>(
    () => CreditNoteBloc(getIt<CreditNoteRepository>()),
  );
  getIt.registerLazySingleton<ProformaBloc>(
    () => ProformaBloc(getIt<ProformaRepository>()),
  );
  getIt.registerLazySingleton<CustomerBloc>(
    () => CustomerBloc(getIt<CustomerRepository>()),
  );
  getIt.registerLazySingleton<ProductBloc>(
    () => ProductBloc(getIt<ProductRepository>()),
  );
  getIt.registerLazySingleton<ReferralGuideBloc>(
    () => ReferralGuideBloc(getIt<ReferralGuideRepository>()),
  );
  getIt.registerLazySingleton<ReportBloc>(
    () => ReportBloc(getIt<ReportRepository>()),
  );
  getIt.registerLazySingleton<SettingsBloc>(
    () => SettingsBloc(getIt<StorageService>(), getIt<BiometricService>()),
  );
}

StreamSubscription<AuthState>? _authSubscription;

/// Dispatches reset events to all feature BLoCs when the user is logged out or
/// the session ends, so singleton blocs do not keep the previous user's data.
void setupAuthListener() {
  _authSubscription?.cancel();
  final authBloc = getIt<AuthBloc>();
  _authSubscription = authBloc.stream.listen((state) {
    if (state is AuthInitial ||
        state is AuthUnauthenticated ||
        state is AuthSessionExpired) {
      getIt<InvoiceBloc>().add(const ResetInvoiceEvent());
      getIt<CustomerBloc>().add(const ResetCustomerEvent());
      getIt<ProductBloc>().add(const ResetProductEvent());
      getIt<CreditNoteBloc>().add(const ResetCreditNoteEvent());
      getIt<ProformaBloc>().add(const ResetProformaEvent());
      getIt<ReferralGuideBloc>().add(const ResetReferralGuideEvent());
      getIt<ReportBloc>().add(const ResetReportEvent());
    }
  });
}
