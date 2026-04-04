part of 'product_bloc.dart';

abstract class ProductState extends Equatable {
  const ProductState();

  @override
  List<Object> get props => [];
}

class ProductInitial extends ProductState {}

class ProductLoading extends ProductState {}

class ProductDetailLoading extends ProductState {}

class ProductsLoaded extends ProductState {
  final List<Product> products;

  const ProductsLoaded(this.products);

  @override
  List<Object> get props => [products];
}

class ProductDetailLoaded extends ProductState {
  final Product product;

  const ProductDetailLoaded(this.product);

  @override
  List<Object> get props => [product];
}

class ProductCreated extends ProductState {
  final Product product;

  const ProductCreated(this.product);

  @override
  List<Object> get props => [product];
}

class ProductUpdated extends ProductState {
  final Product product;

  const ProductUpdated(this.product);

  @override
  List<Object> get props => [product];
}

class ProductDeleted extends ProductState {
  final int id;

  const ProductDeleted(this.id);

  @override
  List<Object> get props => [id];
}

class ProductError extends ProductState {
  final String message;

  const ProductError(this.message);

  @override
  List<Object> get props => [message];
}

class ProductListError extends ProductState {
  final String message;

  const ProductListError(this.message);

  @override
  List<Object> get props => [message];
}

class ProductDetailError extends ProductState {
  final String message;

  const ProductDetailError(this.message);

  @override
  List<Object> get props => [message];
}
