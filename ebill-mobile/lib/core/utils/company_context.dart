import '../../domain/entities/user.dart';
import '../storage/storage_service.dart';

/// Resolves [idCompany] for API calls: selection in storage (if valid for user),
/// then first company from JWT, then [User.companyId]. Returns null if none found.
Future<int?> resolveCompanyId(StorageService storage, User? user) async {
  final selected = await storage.getSelectedCompany();
  final selectedInt = int.tryParse(selected ?? '');
  final allowed = user?.companyEstablishments
          .map((e) => e.idCompany)
          .where((id) => id > 0)
          .toSet() ??
      {};

  if (allowed.isNotEmpty) {
    if (selectedInt != null && allowed.contains(selectedInt)) {
      return selectedInt;
    }
    return allowed.first;
  }

  return user?.companyId;
}
