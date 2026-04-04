import 'package:equatable/equatable.dart';

class Customer extends Equatable {
  final int id;
  final String identification;
  final String name;
  final String? email;
  final String? phone;
  final String? address;
  final bool isActive;

  const Customer({
    required this.id,
    required this.identification,
    required this.name,
    this.email,
    this.phone,
    this.address,
    this.isActive = true,
  });

  @override
  List<Object?> get props => [
        id,
        identification,
        name,
        email,
        phone,
        address,
        isActive,
      ];
}

