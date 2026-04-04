import '../../domain/entities/product.dart';

class ProductModel extends Product {
  final int productId;
  final int? companyId;
  final int? type;
  final bool? active;

  const ProductModel({
    required this.productId,
    required super.mainCode,
    required super.name,
    required super.unitPrice,
    this.companyId,
    this.type,
    this.active,
    super.description,
    super.hasIce,
    super.hasTourismIva,
    super.idTaxType,
    super.ivaPercent,
    super.icePercent,
    super.isActive,
  }) : super(id: productId);

  factory ProductModel.fromJson(Map<String, dynamic> json) {
    double? ivaFromTax;
    double? iceFromTax;
    final ivaTax = json['ivaTaxType'];
    if (ivaTax is Map<String, dynamic>) {
      ivaFromTax = (ivaTax['value'] as num?)?.toDouble();
    }
    final iceTax = json['iceTaxType'];
    if (iceTax is Map<String, dynamic>) {
      iceFromTax = (iceTax['value'] as num?)?.toDouble();
    }
    // Mapeo manual para manejar las diferencias entre backend y modelo
    return ProductModel(
      productId: (json['idProduct'] as num?)?.toInt() ?? 0,
      mainCode: json['mainCode'] as String? ?? '',
      name: json['name'] as String? ?? '',
      unitPrice: (json['unitPrice'] as num?)?.toDouble() ?? 0.0,
      companyId: (json['idCompany'] as num?)?.toInt(),
      type: (json['productType'] as num?)?.toInt(),
      description: json['description'] as String?,
      active: json['status'] as bool?,
      hasIce: json['hasIce'] as bool? ?? false,
      hasTourismIva: json['hasTourismIva'] as bool? ?? false,
      idTaxType: json['taxType'] is Map<String, dynamic>
          ? ((json['taxType'] as Map<String, dynamic>)['idTaxType'] as num?)?.toInt()
          : null,
      ivaPercent: ivaFromTax,
      icePercent: iceFromTax,
      isActive: json['status'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'idProduct': productId,
      'mainCode': mainCode,
      'name': name,
      'unitPrice': unitPrice,
      'idCompany': companyId,
      'productType': type,
      'description': description,
      'status': active ?? isActive,
      'hasIce': hasIce,
      'hasTourismIva': hasTourismIva,
      if (idTaxType != null) 'idTaxType': idTaxType,
    };
  }

  Product toEntity() {
    return Product(
      id: productId,
      mainCode: mainCode,
      name: name,
      unitPrice: unitPrice,
      description: description,
      hasIce: hasIce,
      hasTourismIva: hasTourismIva,
      idTaxType: idTaxType,
      ivaPercent: ivaPercent,
      icePercent: icePercent,
      isActive: active ?? isActive,
    );
  }
}

