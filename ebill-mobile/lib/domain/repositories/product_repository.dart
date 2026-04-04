import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/product.dart';

abstract class ProductRepository {
  Future<Either<Failure, List<Product>>> getProducts({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
  });
  Future<Either<Failure, Product>> getProductById(int id);
  Future<Either<Failure, Product>> createProduct(Product product, {required int idCompany});
  Future<Either<Failure, Product>> updateProduct(int id, Product product, {required int idCompany});
  Future<Either<Failure, void>> deleteProduct(int id);
}

