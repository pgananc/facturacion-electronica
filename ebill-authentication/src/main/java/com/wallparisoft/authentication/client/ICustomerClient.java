package com.wallparisoft.authentication.client;

import com.wallparisoft.authentication.dto.CompanyDto;
import com.wallparisoft.authentication.dto.EstablishmentDto;
import com.wallparisoft.authentication.dto.InvoiceIssuancePointDetailDto;
import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para obtener información de Company, Establishment e Invoice Point desde
 * ebill-customer.
 */
@FeignClient(
    name = "customer-client",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface ICustomerClient {

  /**
   * Obtiene la información completa de una compañía por su ID.
   *
   * @param idCompany ID de la compañía
   * @return CompanyDto con la información completa
   */
  @GetMapping("/api/company/{idCompany}")
  CompanyDto getCompanyById(@PathVariable("idCompany") Long idCompany);

  /**
   * Obtiene la información completa de un establecimiento por su ID.
   *
   * @param idEstablishment ID del establecimiento
   * @return EstablishmentDto con la información completa
   */
  @GetMapping("/api/establishments/{idEstablishment}")
  EstablishmentDto getEstablishmentById(@PathVariable("idEstablishment") Long idEstablishment);

  /**
   * Obtiene la información completa de un punto de emisión por su ID.
   *
   * @param idInvoiceIssuancePoint ID del punto de emisión
   * @return InvoiceIssuancePointDetailDto con la información completa
   */
  @GetMapping("/api/invoice-issuance-points/{idInvoiceIssuancePoint}")
  InvoiceIssuancePointDetailDto getInvoicePointById(
      @PathVariable("idInvoiceIssuancePoint") Long idInvoiceIssuancePoint);
}
