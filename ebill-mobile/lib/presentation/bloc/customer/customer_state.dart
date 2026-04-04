part of 'customer_bloc.dart';

abstract class CustomerState extends Equatable {
  const CustomerState();

  @override
  List<Object> get props => [];
}

class CustomerInitial extends CustomerState {}

class CustomerLoading extends CustomerState {}

class CustomerDetailLoading extends CustomerState {}

class CustomersLoaded extends CustomerState {
  final List<Customer> customers;

  const CustomersLoaded(this.customers);

  @override
  List<Object> get props => [customers];
}

class CustomerDetailLoaded extends CustomerState {
  final Customer customer;

  const CustomerDetailLoaded(this.customer);

  @override
  List<Object> get props => [customer];
}

class CustomerCreated extends CustomerState {
  final Customer customer;

  const CustomerCreated(this.customer);

  @override
  List<Object> get props => [customer];
}

class CustomerUpdated extends CustomerState {
  final Customer customer;

  const CustomerUpdated(this.customer);

  @override
  List<Object> get props => [customer];
}

class CustomerDeleted extends CustomerState {
  final int id;

  const CustomerDeleted(this.id);

  @override
  List<Object> get props => [id];
}

class CustomerError extends CustomerState {
  final String message;

  const CustomerError(this.message);

  @override
  List<Object> get props => [message];
}

class CustomerListError extends CustomerState {
  final String message;

  const CustomerListError(this.message);

  @override
  List<Object> get props => [message];
}

class CustomerDetailError extends CustomerState {
  final String message;

  const CustomerDetailError(this.message);

  @override
  List<Object> get props => [message];
}

