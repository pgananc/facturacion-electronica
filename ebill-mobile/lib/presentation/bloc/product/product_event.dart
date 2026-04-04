part of 'product_bloc.dart';

abstract class ProductEvent extends Equatable {
  const ProductEvent();

  @override
  List<Object?> get props => [];
}

class LoadProductsEvent extends ProductEvent {
  final int? page;
  final int? pageSize;
  final String? search;
  final int? idCompany;
  final bool forceRefresh;

  const LoadProductsEvent({
    this.page,
    this.pageSize,
    this.search,
    this.idCompany,
    this.forceRefresh = false,
  });

  @override
  List<Object?> get props => [page, pageSize, search, idCompany, forceRefresh];
}

class LoadProductByIdEvent extends ProductEvent {
  final int id;

  const LoadProductByIdEvent(this.id);

  @override
  List<Object> get props => [id];
}

class CreateProductEvent extends ProductEvent {
  final Product product;
  final int idCompany;

  const CreateProductEvent(this.product, {required this.idCompany});

  @override
  List<Object> get props => [product, idCompany];
}

class UpdateProductEvent extends ProductEvent {
  final int id;
  final Product product;
  final int idCompany;

  const UpdateProductEvent(this.id, this.product, {required this.idCompany});

  @override
  List<Object> get props => [id, product, idCompany];
}

class DeleteProductEvent extends ProductEvent {
  final int id;

  const DeleteProductEvent(this.id);

  @override
  List<Object> get props => [id];
}

class ResetProductEvent extends ProductEvent {
  const ResetProductEvent();

  @override
  List<Object?> get props => [];
}
