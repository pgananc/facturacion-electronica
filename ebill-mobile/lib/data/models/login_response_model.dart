import 'package:json_annotation/json_annotation.dart';

part 'login_response_model.g.dart';

/// Model for login response from backend
/// Structure: { code: 1, status: "OK", token: { token: "JWT..." } }
@JsonSerializable()
class LoginResponseModel {
  @JsonKey(defaultValue: 0)
  final int code;
  @JsonKey(defaultValue: '')
  final String status;
  final TokenModel? token;

  LoginResponseModel({
    required this.code,
    required this.status,
    this.token,
  });

  factory LoginResponseModel.fromJson(Map<String, dynamic> json) =>
      _$LoginResponseModelFromJson(json);

  Map<String, dynamic> toJson() => _$LoginResponseModelToJson(this);
}

@JsonSerializable()
class TokenModel {
  @JsonKey(defaultValue: '')
  final String token;
  final String? refreshToken;

  TokenModel({required this.token, this.refreshToken});

  factory TokenModel.fromJson(Map<String, dynamic> json) =>
      _$TokenModelFromJson(json);

  Map<String, dynamic> toJson() => _$TokenModelToJson(this);
}

