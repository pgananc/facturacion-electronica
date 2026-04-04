import 'package:equatable/equatable.dart';

import 'company_establishment.dart';

class User extends Equatable {
  final int id;
  final String username;
  final String email;
  final String? firstName;
  final String? lastName;
  final String? phone;
  final bool isActive;

  /// Companies the user can work with (from JWT `companyEstablishments`).
  final List<CompanyEstablishment> companyEstablishments;

  /// Active company for APIs (session / selector); falls back to [resolveCompanyId] if null.
  final int? companyId;

  /// RUC / identificación tributaria (filtros customer MS `companyIdentification`).
  final String? companyIdentification;

  const User({
    required this.id,
    required this.username,
    required this.email,
    this.firstName,
    this.lastName,
    this.phone,
    this.isActive = true,
    this.companyEstablishments = const [],
    this.companyId,
    this.companyIdentification,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'username': username,
        'email': email,
        'firstName': firstName,
        'lastName': lastName,
        'phone': phone,
        'isActive': isActive,
        'companyId': companyId,
        'companyIdentification': companyIdentification,
        'companyEstablishments': companyEstablishments
            .map(
              (e) => {
                'idCompany': e.idCompany,
                'establishments': e.establishmentIds,
              },
            )
            .toList(),
      };

  factory User.fromJson(Map<String, dynamic> json) {
    final rawList = json['companyEstablishments'];
    final establishments = <CompanyEstablishment>[];
    if (rawList is List) {
      for (final item in rawList) {
        if (item is Map<String, dynamic>) {
          final idC = item['idCompany'];
          final companyId = idC is int ? idC : int.tryParse('$idC') ?? 0;
          if (companyId <= 0) continue;
          final est = item['establishments'];
          establishments.add(
            CompanyEstablishment(
              idCompany: companyId,
              establishmentIds: est is List
                  ? est.map((e) => int.tryParse('$e') ?? 0).where((e) => e != 0).toList()
                  : const [],
            ),
          );
        }
      }
    }
    return User(
      id: json['id'] is int ? json['id'] as int : int.tryParse('${json['id']}') ?? 0,
      username: json['username'] as String? ?? '',
      email: json['email'] as String? ?? '',
      firstName: json['firstName'] as String?,
      lastName: json['lastName'] as String?,
      phone: json['phone'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      companyEstablishments: establishments,
      companyId: json['companyId'] == null
          ? null
          : (json['companyId'] is int
              ? json['companyId'] as int
              : int.tryParse('${json['companyId']}')),
      companyIdentification: json['companyIdentification'] as String?,
    );
  }

  String get fullName {
    if (firstName != null && lastName != null) {
      return '$firstName $lastName';
    }
    if (firstName != null && firstName!.isNotEmpty) return firstName!;
    return username;
  }

  User copyWith({
    int? id,
    String? username,
    String? email,
    String? firstName,
    String? lastName,
    String? phone,
    bool? isActive,
    List<CompanyEstablishment>? companyEstablishments,
    int? companyId,
    String? companyIdentification,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      email: email ?? this.email,
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      phone: phone ?? this.phone,
      isActive: isActive ?? this.isActive,
      companyEstablishments: companyEstablishments ?? this.companyEstablishments,
      companyId: companyId ?? this.companyId,
      companyIdentification: companyIdentification ?? this.companyIdentification,
    );
  }

  @override
  List<Object?> get props => [
        id,
        username,
        email,
        firstName,
        lastName,
        phone,
        isActive,
        companyEstablishments,
        companyId,
        companyIdentification,
      ];
}

