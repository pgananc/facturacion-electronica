package com.wallparisoft.ebill.auth.util.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.management.relation.Role;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ToString
public class TokenInformation {

  String userName;
  List<String> roles;
  List<Long> idCompanies;
}
