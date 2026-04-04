import 'package:json_annotation/json_annotation.dart';

part 'client_search_request_model.g.dart';

/// Request model for searching/filtering clients with pagination
@JsonSerializable()
class ClientSearchRequestModel {
  final String? companyIdentification;
  final String? identification;
  final String? name;
  final int? clientType;
  final bool? status;
  final String? email;
  final String? phone;
  final String? mobile;
  final String? address;

  ClientSearchRequestModel({
    this.companyIdentification,
    this.identification,
    this.name,
    this.clientType,
    this.status,
    this.email,
    this.phone,
    this.mobile,
    this.address,
  });

  factory ClientSearchRequestModel.fromJson(Map<String, dynamic> json) =>
      _$ClientSearchRequestModelFromJson(json);

  Map<String, dynamic> toJson() => _$ClientSearchRequestModelToJson(this);
}

