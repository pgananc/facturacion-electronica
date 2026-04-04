import 'package:equatable/equatable.dart';

class SalesByProductReport extends Equatable {
  final List<SalesByProductItem> items;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  const SalesByProductReport({
    required this.items,
    this.totalElements = 0,
    this.totalPages = 0,
    this.hasNext = false,
  });

  @override
  List<Object?> get props => [items, totalElements, totalPages, hasNext];
}

class SalesByProductItem extends Equatable {
  final int idProduct;
  final String productName;
  final String productCode;
  final int quantitySold;
  final double totalInvoiced;
  final double unitPrice;

  const SalesByProductItem({
    required this.idProduct,
    required this.productName,
    required this.productCode,
    required this.quantitySold,
    required this.totalInvoiced,
    required this.unitPrice,
  });

  @override
  List<Object?> get props => [
        idProduct,
        productName,
        productCode,
        quantitySold,
        totalInvoiced,
        unitPrice,
      ];
}
