import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/entities/product.dart';
import '../../domain/repositories/product_repository.dart';
import '../datasources/product_remote_datasource.dart';
import '../models/product_model.dart';
import 'repository_guard.dart';

class ProductRepositoryImpl with RepositoryGuard implements ProductRepository {
  final ProductRemoteDataSource remoteDataSource;

  ProductRepositoryImpl(this.remoteDataSource);

  @override
  Future<Either<Failure, List<Product>>> getProducts({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
  }) =>
      guard(() async {
        final products = await remoteDataSource.getProducts(
          page: page,
          pageSize: pageSize,
          search: search,
          idCompany: idCompany,
        );
        return products.map((model) => model.toEntity()).toList();
      });

  @override
  Future<Either<Failure, Product>> getProductById(int id) =>
      guard(() async {
        final product = await remoteDataSource.getProductById(id);
        return product.toEntity();
      });

  @override
  Future<Either<Failure, Product>> createProduct(Product product, {required int idCompany}) =>
      guard(() async {
        final productModel = ProductModel(
          productId: 0,
          mainCode: product.mainCode,
          name: product.name,
          unitPrice: product.unitPrice,
          companyId: idCompany,
          description: product.description,
          hasIce: product.hasIce,
          hasTourismIva: product.hasTourismIva,
          idTaxType: product.idTaxType,
          isActive: product.isActive,
        );
        final createdProduct =
            await remoteDataSource.createProduct(productModel);
        return createdProduct.toEntity();
      });

  @override
  Future<Either<Failure, Product>> updateProduct(int id, Product product, {required int idCompany}) =>
      guard(() async {
        final productModel = ProductModel(
          productId: product.id,
          mainCode: product.mainCode,
          name: product.name,
          unitPrice: product.unitPrice,
          companyId: idCompany,
          description: product.description,
          hasIce: product.hasIce,
          hasTourismIva: product.hasTourismIva,
          idTaxType: product.idTaxType,
          isActive: product.isActive,
        );
        final updatedProduct =
            await remoteDataSource.updateProduct(id, productModel);
        return updatedProduct.toEntity();
      });

  @override
  Future<Either<Failure, void>> deleteProduct(int id) =>
      guard(() => remoteDataSource.deleteProduct(id));
}
