package com.wallparisoft.ebill.customer.dto;

import java.io.Serializable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientRequestDto implements Serializable {
  String identification;
  String name;
  Integer clientType;
  Boolean status;
  String companyIdentification;

  // Filtros por contactos
  String email; // Filtrar por correo (MAI)
  String phone; // Filtrar por teléfono (TEL)
  String mobile; // Filtrar por celular (CEL)
  String address; // Filtrar por dirección (DIR)
}
