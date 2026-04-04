import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../entities/customer.dart';

abstract class CustomerRepository {
  Future<Either<Failure, List<Customer>>> getCustomers({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
    String? companyIdentification,
  });
  Future<Either<Failure, Customer>> getCustomerById(int id);
  Future<Either<Failure, Customer>> createCustomer(Customer customer);
  Future<Either<Failure, Customer>> updateCustomer(int id, Customer customer);
  Future<Either<Failure, void>> deleteCustomer(int id);
}

