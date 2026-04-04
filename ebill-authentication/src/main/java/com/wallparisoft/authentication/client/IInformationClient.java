package com.wallparisoft.authentication.client;

import com.wallparisoft.authentication.dto.ClientDto;
import com.wallparisoft.authentication.dto.ClientSearchRequestDto;
import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "information-client",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface IInformationClient {

  @PostMapping("/api/client/search")
  ClientDto findByIdentificationAndEmail(@Valid @RequestBody ClientSearchRequestDto searchRequest);
}
