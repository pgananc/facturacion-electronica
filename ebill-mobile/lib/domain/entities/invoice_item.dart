import 'package:equatable/equatable.dart';

import 'product.dart';

/// Line on an invoice draft: product and quantity (domain; reusable across UI).
class InvoiceItem extends Equatable {
  final Product product;
  final int quantity;

  const InvoiceItem({
    required this.product,
    required this.quantity,
  });

  @override
  List<Object?> get props => [product, quantity];
}
