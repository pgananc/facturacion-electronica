import 'package:equatable/equatable.dart';

class Product extends Equatable {
  final int id;
  final String mainCode;
  final String name;
  final double unitPrice;
  final String? description;
  final bool hasIce;
  final bool hasTourismIva;
  final int? idTaxType;
  /// Porcentaje IVA (p.ej. 15) desde tax type; null = usar reglas por defecto.
  final double? ivaPercent;
  /// Porcentaje ICE si aplica.
  final double? icePercent;
  final bool isActive;

  const Product({
    required this.id,
    required this.mainCode,
    required this.name,
    required this.unitPrice,
    this.description,
    this.hasIce = false,
    this.hasTourismIva = false,
    this.idTaxType,
    this.ivaPercent,
    this.icePercent,
    this.isActive = true,
  });

  @override
  List<Object?> get props => [
        id,
        mainCode,
        name,
        unitPrice,
        description,
        hasIce,
        hasTourismIva,
        idTaxType,
        ivaPercent,
        icePercent,
        isActive,
      ];
}

