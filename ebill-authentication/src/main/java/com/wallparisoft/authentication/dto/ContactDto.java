package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ContactDto {

  private Long idContact;
  private String contactType;
  private String value;
  private Boolean status;
}
