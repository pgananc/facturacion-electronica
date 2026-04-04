package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

@Data
public class EstablishmentDto {
  private Long idEstablishment;
  private String name;
  private String code;
  private String address;
  private String type;
  private Boolean status;
}
