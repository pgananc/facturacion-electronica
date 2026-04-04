package com.wallparisoft.authentication.client;

import com.wallparisoft.authentication.dto.InvoiceIssuancePointDto;
import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "invoice-point-client",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface IInvoiceIssuancePointClient {
  @GetMapping("/api/invoice-issuance-points/available")
  List<InvoiceIssuancePointDto> findAvailablePoints(
      @RequestParam("idEstablishments") List<Long> idEstablishments);

  @GetMapping("/api/invoice-issuance-points/current-session")
  InvoiceIssuancePointDto getCurrentSession(@RequestParam("userName") String userName);
}
