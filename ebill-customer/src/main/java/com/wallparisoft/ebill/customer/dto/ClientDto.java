package com.wallparisoft.ebill.customer.dto;

import com.wallparisoft.ebill.customer.validation.ValidClientIdentification;
import com.wallparisoft.ebill.customer.validation.ValidEcuadorianIdentification;
import java.io.Serializable;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidClientIdentification
public class ClientDto implements Serializable {

  Long idClient;
  Integer idType;

  String identification;

  String name;
  Integer clientType;
  Boolean status;

  @ValidEcuadorianIdentification(message = "RUC de la compañía inválido", allowNull = true)
  String companyIdentification;

  List<ContactDto> contacts;
}
