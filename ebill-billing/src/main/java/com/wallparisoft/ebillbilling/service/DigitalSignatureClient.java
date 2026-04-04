package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import com.wallparisoft.ebillbilling.response.DigitalSignatureResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "digital-signature-service",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface DigitalSignatureClient {

  @GetMapping("/api/digital-signature/company/{idCompany}")
  DigitalSignatureResponse getDigitalSignatureByCompanyId(
      @PathVariable("idCompany") Long idCompany);
}
