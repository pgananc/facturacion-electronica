package com.wallparisoft.ebillbilling.dto;

import java.io.Serializable;
import java.time.Instant;
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
public class DigitalSignatureDto implements Serializable {

  private static final long serialVersionUID = 8850713644044253827L;

  private byte[] signFile;

  private String password;

  private Instant effectiveDate;

  private Long idCompany;
}
