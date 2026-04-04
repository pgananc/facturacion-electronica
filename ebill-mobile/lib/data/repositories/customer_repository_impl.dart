import 'package:dartz/dartz.dart';

import '../../core/error/failures.dart';
import '../../domain/entities/customer.dart';
import '../../domain/repositories/customer_repository.dart';
import '../datasources/customer_remote_datasource.dart';
import '../models/customer_model.dart';
import 'repository_guard.dart';

class CustomerRepositoryImpl with RepositoryGuard implements CustomerRepository {
  final CustomerRemoteDataSource remoteDataSource;

  CustomerRepositoryImpl(this.remoteDataSource);

  @override
  Future<Either<Failure, List<Customer>>> getCustomers({
    int? page,
    int? pageSize,
    String? search,
    int? idCompany,
    String? companyIdentification,
  }) =>
      guard(() async {
        final customers = await remoteDataSource.getCustomers(
          page: page,
          pageSize: pageSize,
          search: search,
          idCompany: idCompany,
          companyIdentification: companyIdentification,
        );
        return customers.map((model) => model.toEntity()).toList();
      });

  @override
  Future<Either<Failure, Customer>> getCustomerById(int id) =>
      guard(() async {
        final customer = await remoteDataSource.getCustomerById(id);
        return customer.toEntity();
      });

  @override
  Future<Either<Failure, Customer>> createCustomer(Customer customer) =>
      guard(() async {
        final customerModel = CustomerModel(
          id: 0,
          identification: customer.identification,
          name: customer.name,
          email: customer.email,
          phone: customer.phone,
          address: customer.address,
          isActive: customer.isActive,
        );
        final createdCustomer =
            await remoteDataSource.createCustomer(customerModel);
        return createdCustomer.toEntity();
      });

  @override
  Future<Either<Failure, Customer>> updateCustomer(
    int id,
    Customer customer,
  ) =>
      guard(() async {
        final customerModel = CustomerModel(
          id: customer.id,
          identification: customer.identification,
          name: customer.name,
          email: customer.email,
          phone: customer.phone,
          address: customer.address,
          isActive: customer.isActive,
        );
        final updatedCustomer =
            await remoteDataSource.updateCustomer(id, customerModel);
        return updatedCustomer.toEntity();
      });

  @override
  Future<Either<Failure, void>> deleteCustomer(int id) =>
      guard(() => remoteDataSource.deleteCustomer(id));
}
