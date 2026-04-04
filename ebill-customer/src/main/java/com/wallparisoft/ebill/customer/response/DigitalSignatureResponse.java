package com.wallparisoft.ebill.customer.response;

import com.wallparisoft.ebill.customer.dto.DigitalSignatureDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class DigitalSignatureResponse extends BasicResponse {
  List<DigitalSignatureDto> digitalSignatureDtos;
  DigitalSignatureDto digitalSignatureDto;
}
