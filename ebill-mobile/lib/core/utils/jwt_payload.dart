import 'dart:convert';

/// Decodifica el payload del JWT (sin verificar firma; solo lectura en cliente).
class JwtPayload {
  JwtPayload({
    this.subjectUserId,
    this.userName,
    this.companyEstablishments = const [],
    this.exp,
  });

  final int? subjectUserId;
  final String? userName;
  final List<CompanyEstablishmentClaim> companyEstablishments;
  final int? exp;

  /// When [exp] is missing the token is treated as non-expiring (server controls lifetime).
  /// [bufferSeconds] extends validity after [exp] (grace period).
  bool isExpired({int bufferSeconds = 0}) {
    if (exp == null) return false;
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    return now > exp! + bufferSeconds;
  }

  static JwtPayload? fromToken(String? token) => decode(token);

  static JwtPayload? decode(String? token) {
    if (token == null || token.isEmpty) return null;
    final parts = token.split('.');
    if (parts.length != 3) return null;
    try {
      final normalized = base64Url.normalize(parts[1]);
      final payloadMap =
          json.decode(utf8.decode(base64Url.decode(normalized))) as Map<String, dynamic>;
      final sub = payloadMap['sub'];
      final userName = payloadMap['userName'] as String?;
      final expRaw = payloadMap['exp'];
      int? exp;
      if (expRaw is num) {
        exp = expRaw.toInt();
      } else if (expRaw != null) {
        exp = int.tryParse('$expRaw');
      }
      final rawList = payloadMap['companyEstablishments'];
      final companies = <CompanyEstablishmentClaim>[];
      if (rawList is List) {
        for (final e in rawList) {
          if (e is Map<String, dynamic>) {
            final id = e['idCompany'];
            final est = e['establishments'];
            companies.add(
              CompanyEstablishmentClaim(
                idCompany: (id is num) ? id.toInt() : int.tryParse('$id'),
                establishmentIds: est is List
                    ? est.map((x) => (x is num) ? x.toInt() : int.tryParse('$x') ?? 0).where((x) => x > 0).toList()
                    : const [],
              ),
            );
          }
        }
      }
      return JwtPayload(
        subjectUserId: sub is num ? sub.toInt() : int.tryParse('$sub'),
        userName: userName,
        companyEstablishments: companies,
        exp: exp,
      );
    } catch (_) {
      return null;
    }
  }
}

class CompanyEstablishmentClaim {
  CompanyEstablishmentClaim({
    required this.idCompany,
    required this.establishmentIds,
  });

  final int? idCompany;
  final List<int> establishmentIds;
}
