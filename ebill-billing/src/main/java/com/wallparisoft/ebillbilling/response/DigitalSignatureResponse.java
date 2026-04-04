package com.wallparisoft.ebillbilling.response;

import com.wallparisoft.ebillbilling.dto.DigitalSignatureDto;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor
public class DigitalSignatureResponse implements Serializable {
  /** */
  private static final long serialVersionUID = 2911491177452694119L;

  DigitalSignatureDto digitalSignatureDto;
}
