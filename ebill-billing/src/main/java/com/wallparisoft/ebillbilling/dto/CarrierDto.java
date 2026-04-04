package com.wallparisoft.ebillbilling.dto;

import java.io.Serializable;
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
public class CarrierDto implements Serializable {

  Long idCarrier;

  Long idCompany;

  String identificationType;

  String identification;

  String name;

  String address;

  String phone;

  String email;

  String licenseNumber;

  String vehicleType;

  Boolean status;
}
