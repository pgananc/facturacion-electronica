package com.wallparisoft.ebill.customer.response;

import com.wallparisoft.ebill.customer.dto.CertificateInfoDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class CertificateInfoResponse extends BasicResponse {
  CertificateInfoDto certificateInfoDto;
}
