package com.wallparisoft.ebill.auth.util.model;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ToString
public class TokenInformation {
  String idUser;
  String userName;
  List<String> roles;
  List<CompanyEstablishment> companyEstablishments;
}
