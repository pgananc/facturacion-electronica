import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get_it/get_it.dart';
import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../data/datasources/auth_remote_datasource.dart';
import '../../data/datasources/company_remote_datasource.dart';
import '../../data/datasources/credit_note_remote_datasource.dart';
import '../../data/datasources/customer_remote_datasource.dart';
import '../../data/datasources/invoice_remote_datasource.dart';
import '../../data/datasources/invoice_session_remote_datasource.dart';
import '../../data/datasources/proforma_remote_datasource.dart';
import '../../data/datasources/product_remote_datasource.dart';
import '../../data/datasources/referral_guide_remote_datasource.dart';
import '../../data/datasources/report_remote_datasource.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../data/repositories/company_repository_impl.dart';
import '../../data/repositories/credit_note_repository_impl.dart';
import '../../data/repositories/customer_repository_impl.dart';
import '../../data/repositories/invoice_repository_impl.dart';
import '../../data/repositories/invoice_session_repository_impl.dart';
import '../../data/repositories/proforma_repository_impl.dart';
import '../../data/repositories/product_repository_impl.dart';
import '../../data/repositories/referral_guide_repository_impl.dart';
import '../../data/repositories/report_repository_impl.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/repositories/company_repository.dart';
import '../../domain/repositories/credit_note_repository.dart';
import '../../domain/repositories/customer_repository.dart';
import '../../domain/repositories/invoice_repository.dart';
import '../../domain/repositories/invoice_session_repository.dart';
import '../../domain/repositories/proforma_repository.dart';
import '../../domain/repositories/product_repository.dart';
import '../../domain/repositories/referral_guide_repository.dart';
import '../../domain/repositories/report_repository.dart';
import '../network/dio_client.dart';
import '../services/biometric_service.dart';
import '../services/inactivity_service.dart';
import '../storage/storage_service.dart';

final getIt = GetIt.instance;

Future<void> configureDependencies() async {
  final prefs = await SharedPreferences.getInstance();
  const secureStorage = FlutterSecureStorage();

  final storageService = StorageService(prefs, secureStorage);
  await storageService.hydrateFromPersistence();
  getIt.registerSingleton<StorageService>(storageService);

  getIt.registerSingleton<DioClient>(DioClient(storageService));
  getIt.registerSingleton<InactivityService>(InactivityService());
  getIt.registerLazySingleton<LocalAuthentication>(() => LocalAuthentication());
  getIt.registerLazySingleton<BiometricService>(
    () => BiometricService(
      getIt<StorageService>(),
      getIt<LocalAuthentication>(),
    ),
  );

  // Datasources
  getIt.registerLazySingleton<AuthRemoteDataSource>(
    () => AuthRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<CompanyRemoteDataSource>(
    () => CompanyRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<InvoiceSessionRemoteDataSource>(
    () => InvoiceSessionRemoteDataSource(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<InvoiceRemoteDataSource>(
    () => InvoiceRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<CreditNoteRemoteDataSource>(
    () => CreditNoteRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<ProformaRemoteDataSource>(
    () => ProformaRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<CustomerRemoteDataSource>(
    () => CustomerRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<ProductRemoteDataSource>(
    () => ProductRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<ReferralGuideRemoteDataSource>(
    () => ReferralGuideRemoteDataSourceImpl(getIt<DioClient>()),
  );
  getIt.registerLazySingleton<ReportRemoteDataSource>(
    () => ReportRemoteDataSourceImpl(getIt<DioClient>()),
  );

  // Repositories
  getIt.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(
      getIt<AuthRemoteDataSource>(),
      getIt<StorageService>(),
      getIt<CompanyRemoteDataSource>(),
      getIt<InvoiceSessionRemoteDataSource>(),
    ),
  );
  getIt.registerLazySingleton<CompanyRepository>(
    () => CompanyRepositoryImpl(getIt<CompanyRemoteDataSource>()),
  );
  getIt.registerLazySingleton<InvoiceSessionRepository>(
    () => InvoiceSessionRepositoryImpl(
      getIt<InvoiceSessionRemoteDataSource>(),
    ),
  );
  getIt.registerLazySingleton<InvoiceRepository>(
    () => InvoiceRepositoryImpl(getIt<InvoiceRemoteDataSource>()),
  );
  getIt.registerLazySingleton<CreditNoteRepository>(
    () => CreditNoteRepositoryImpl(
      getIt<CreditNoteRemoteDataSource>(),
      getIt<StorageService>(),
    ),
  );
  getIt.registerLazySingleton<ProformaRepository>(
    () => ProformaRepositoryImpl(getIt<ProformaRemoteDataSource>()),
  );
  getIt.registerLazySingleton<CustomerRepository>(
    () => CustomerRepositoryImpl(getIt<CustomerRemoteDataSource>()),
  );
  getIt.registerLazySingleton<ProductRepository>(
    () => ProductRepositoryImpl(getIt<ProductRemoteDataSource>()),
  );
  getIt.registerLazySingleton<ReferralGuideRepository>(
    () => ReferralGuideRepositoryImpl(
      getIt<ReferralGuideRemoteDataSource>(),
    ),
  );
  getIt.registerLazySingleton<ReportRepository>(
    () => ReportRepositoryImpl(getIt<ReportRemoteDataSource>()),
  );

}
