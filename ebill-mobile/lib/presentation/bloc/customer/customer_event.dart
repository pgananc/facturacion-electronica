part of 'customer_bloc.dart';

abstract class CustomerEvent extends Equatable {
  const CustomerEvent();

  @override
  List<Object?> get props => [];
}

class LoadCustomersEvent extends CustomerEvent {
  final int? page;
  final int? pageSize;
  final String? search;
  final int? idCompany;
  final String? companyIdentification;
  final bool forceRefresh;

  const LoadCustomersEvent({
    this.page,
    this.pageSize,
    this.search,
    this.idCompany,
    this.companyIdentification,
    this.forceRefresh = false,
  });

  @override
  List<Object?> get props => [page, pageSize, search, idCompany, companyIdentification, forceRefresh];
}

class LoadCustomerByIdEvent extends CustomerEvent {
  final int id;

  const LoadCustomerByIdEvent(this.id);

  @override
  List<Object> get props => [id];
}

class CreateCustomerEvent extends CustomerEvent {
  final Customer customer;

  const CreateCustomerEvent(this.customer);

  @override
  List<Object> get props => [customer];
}

class UpdateCustomerEvent extends CustomerEvent {
  final int id;
  final Customer customer;

  const UpdateCustomerEvent(this.id, this.customer);

  @override
  List<Object> get props => [id, customer];
}

class DeleteCustomerEvent extends CustomerEvent {
  final int id;

  const DeleteCustomerEvent(this.id);

  @override
  List<Object> get props => [id];
}

class ResetCustomerEvent extends CustomerEvent {
  const ResetCustomerEvent();

  @override
  List<Object?> get props => [];
}
