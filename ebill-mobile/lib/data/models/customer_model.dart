import '../../domain/entities/customer.dart';

class CustomerModel extends Customer {
  const CustomerModel({
    required super.id,
    required super.identification,
    required super.name,
    super.email,
    super.phone,
    super.address,
    super.isActive,
  });

  factory CustomerModel.fromJson(Map<String, dynamic> json) {
    final contacts = json['contacts'] as List?;
    String? email;
    String? phone;
    String? address;

    if (contacts != null) {
      for (final c in contacts) {
        if (c is! Map) continue;
        final type = (c['contactType'] ?? c['type'] ?? '').toString().toUpperCase();
        final value = (c['value'] ?? c['contact'] ?? '').toString();
        if (value.isEmpty) continue;
        if (type.contains('MAIL') || type.contains('EMAIL')) {
          email ??= value;
        } else if (type.contains('PHONE') || type.contains('TEL') || type.contains('CELULAR')) {
          phone ??= value;
        } else if (type.contains('DIR') || type.contains('ADDRESS')) {
          address ??= value;
        }
      }
    }

    email ??= json['email'] as String?;
    phone ??= json['phone'] as String?;
    address ??= json['address'] as String?;

    final rawId = json['idClient'] ?? json['id'];
    final id = rawId is int ? rawId : (rawId is num ? rawId.toInt() : int.tryParse('$rawId') ?? 0);

    return CustomerModel(
      id: id,
      identification: json['identification'] as String? ?? '',
      name: json['name'] as String? ?? '',
      email: email,
      phone: phone,
      address: address,
      isActive: json['isActive'] as bool? ?? json['status'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    final contacts = <Map<String, dynamic>>[];
    if (email != null && email!.isNotEmpty) {
      contacts.add({'contactType': 'EMAIL', 'value': email});
    }
    if (phone != null && phone!.isNotEmpty) {
      contacts.add({'contactType': 'PHONE', 'value': phone});
    }
    if (address != null && address!.isNotEmpty) {
      contacts.add({'contactType': 'ADDRESS', 'value': address});
    }
    return {
      'idClient': id,
      'identification': identification,
      'name': name,
      'isActive': isActive,
      'contacts': contacts,
    };
  }

  Customer toEntity() {
    return Customer(
      id: id,
      identification: identification,
      name: name,
      email: email,
      phone: phone,
      address: address,
      isActive: isActive,
    );
  }
}
