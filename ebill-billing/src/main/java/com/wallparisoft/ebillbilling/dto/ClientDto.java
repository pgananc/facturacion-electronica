package com.wallparisoft.ebillbilling.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

  Long idCompany;

  String companyIdentification;

  List<ContactDto> contacts;
}
