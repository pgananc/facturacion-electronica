package com.wallparisoft.authentication.dto;

import java.io.Serializable;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientDto implements Serializable {

  Long idClient;
  Integer idType;
  String identification;
  String name;
  Integer clientType;
  Boolean status;
  String companyIdentification;
  List<ContactDto> contacts;
}
