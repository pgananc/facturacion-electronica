import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';

import '../../../domain/entities/customer.dart';
import '../../../domain/repositories/customer_repository.dart';

part 'customer_event.dart';
part 'customer_state.dart';

class CustomerBloc extends Bloc<CustomerEvent, CustomerState> {
  final CustomerRepository customerRepository;

  CustomerBloc(this.customerRepository) : super(CustomerInitial()) {
    on<LoadCustomersEvent>(_onLoadCustomers);
    on<LoadCustomerByIdEvent>(_onLoadCustomerById);
    on<CreateCustomerEvent>(_onCreateCustomer);
    on<UpdateCustomerEvent>(_onUpdateCustomer);
    on<DeleteCustomerEvent>(_onDeleteCustomer);
    on<ResetCustomerEvent>(
      (event, emit) => emit(CustomerInitial()),
    );
  }

  Future<void> _onLoadCustomers(
    LoadCustomersEvent event,
    Emitter<CustomerState> emit,
  ) async {
    final isSearch = event.search != null && event.search!.isNotEmpty;
    if (!event.forceRefresh && !isSearch && state is CustomersLoaded) return;
    emit(CustomerLoading());
    final result = await customerRepository.getCustomers(
      page: event.page,
      pageSize: event.pageSize,
      search: event.search,
      idCompany: event.idCompany,
      companyIdentification: event.companyIdentification,
    );
    result.fold(
      (failure) => emit(CustomerListError(failure.message)),
      (customers) => emit(CustomersLoaded(customers)),
    );
  }

  Future<void> _onLoadCustomerById(
    LoadCustomerByIdEvent event,
    Emitter<CustomerState> emit,
  ) async {
    emit(CustomerDetailLoading());
    final result = await customerRepository.getCustomerById(event.id);
    result.fold(
      (failure) => emit(CustomerDetailError(failure.message)),
      (customer) => emit(CustomerDetailLoaded(customer)),
    );
  }

  Future<void> _onCreateCustomer(
    CreateCustomerEvent event,
    Emitter<CustomerState> emit,
  ) async {
    emit(CustomerLoading());
    final result = await customerRepository.createCustomer(event.customer);
    result.fold(
      (failure) => emit(CustomerError(failure.message)),
      (customer) => emit(CustomerCreated(customer)),
    );
  }

  Future<void> _onUpdateCustomer(
    UpdateCustomerEvent event,
    Emitter<CustomerState> emit,
  ) async {
    emit(CustomerLoading());
    final result = await customerRepository.updateCustomer(event.id, event.customer);
    result.fold(
      (failure) => emit(CustomerError(failure.message)),
      (customer) => emit(CustomerUpdated(customer)),
    );
  }

  Future<void> _onDeleteCustomer(
    DeleteCustomerEvent event,
    Emitter<CustomerState> emit,
  ) async {
    emit(CustomerLoading());
    final result = await customerRepository.deleteCustomer(event.id);
    result.fold(
      (failure) => emit(CustomerError(failure.message)),
      (_) => emit(CustomerDeleted(event.id)),
    );
  }
}

