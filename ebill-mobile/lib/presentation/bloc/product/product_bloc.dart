import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../domain/entities/product.dart';
import '../../../domain/repositories/product_repository.dart';

part 'product_event.dart';
part 'product_state.dart';

class ProductBloc extends Bloc<ProductEvent, ProductState> {
  final ProductRepository productRepository;

  ProductBloc(this.productRepository) : super(ProductInitial()) {
    on<LoadProductsEvent>(_onLoadProducts);
    on<LoadProductByIdEvent>(_onLoadProductById);
    on<CreateProductEvent>(_onCreateProduct);
    on<UpdateProductEvent>(_onUpdateProduct);
    on<DeleteProductEvent>(_onDeleteProduct);
    on<ResetProductEvent>(
      (event, emit) => emit(ProductInitial()),
    );
  }

  Future<void> _onLoadProducts(
    LoadProductsEvent event,
    Emitter<ProductState> emit,
  ) async {
    final isSearch = event.search != null && event.search!.isNotEmpty;
    if (!event.forceRefresh && !isSearch && state is ProductsLoaded) return;
    emit(ProductLoading());
    final result = await productRepository.getProducts(
      page: event.page,
      pageSize: event.pageSize,
      search: event.search,
      idCompany: event.idCompany,
    );
    result.fold(
      (failure) => emit(ProductListError(failure.message)),
      (products) => emit(ProductsLoaded(products)),
    );
  }

  Future<void> _onLoadProductById(
    LoadProductByIdEvent event,
    Emitter<ProductState> emit,
  ) async {
    emit(ProductDetailLoading());
    final result = await productRepository.getProductById(event.id);
    result.fold(
      (failure) => emit(ProductDetailError(failure.message)),
      (product) => emit(ProductDetailLoaded(product)),
    );
  }

  Future<void> _onCreateProduct(
    CreateProductEvent event,
    Emitter<ProductState> emit,
  ) async {
    emit(ProductLoading());
    final result = await productRepository.createProduct(event.product, idCompany: event.idCompany);
    result.fold(
      (failure) => emit(ProductError(failure.message)),
      (product) => emit(ProductCreated(product)),
    );
  }

  Future<void> _onUpdateProduct(
    UpdateProductEvent event,
    Emitter<ProductState> emit,
  ) async {
    emit(ProductLoading());
    final result = await productRepository.updateProduct(event.id, event.product, idCompany: event.idCompany);
    result.fold(
      (failure) => emit(ProductError(failure.message)),
      (product) => emit(ProductUpdated(product)),
    );
  }

  Future<void> _onDeleteProduct(
    DeleteProductEvent event,
    Emitter<ProductState> emit,
  ) async {
    emit(ProductLoading());
    final result = await productRepository.deleteProduct(event.id);
    result.fold(
      (failure) => emit(ProductError(failure.message)),
      (_) => emit(ProductDeleted(event.id)),
    );
  }
}

