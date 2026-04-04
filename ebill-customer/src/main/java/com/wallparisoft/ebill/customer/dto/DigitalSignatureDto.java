package com.wallparisoft.ebill.customer.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DigitalSignatureDto {

  private Long id;
  private byte[] signFile;

  private String password;

  private Long idCompany;

  private String fileName;

  private CertificateInfoDto certificateInfo;
}
