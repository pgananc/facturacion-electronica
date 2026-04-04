package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import com.wallparisoft.ebillbilling.dto.ClientDtoResponse;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.EstablishmentDto;
import com.wallparisoft.ebillbilling.dto.subscription.CompanyWithoutSubscriptionDto;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "customer-service",
    url = "${service.customer.base-url}",
    configuration = FeignClientConfig.class)
public interface CustomerClient {

  @GetMapping("/api/client/{idClient}")
  ClientDtoResponse getClientById(@PathVariable("idClient") Long idClient);

  /**
   * Busca un cliente por su identificación (cédula/RUC)
   *
   * @param identification Identificación del cliente
   * @return Cliente encontrado o null si no existe
   */
  @GetMapping("/api/client/identification/{identification}")
  ClientDtoResponse getClientByIdentification(@PathVariable String identification);

  /**
   * Actualiza el secuencial de un punto de emisión en ebill-customer
   *
   * @param code Código del punto de emisión (ej: "001")
   * @param establishmentId ID del establecimiento
   * @param body Map con el campo "sequentialInvoice"
   */
  @PutMapping("/api/invoice-issuance-points/{code}/sequential")
  void updateSequential(
      @PathVariable("code") String code,
      @RequestParam("establishmentId") Long establishmentId,
      @RequestBody Map<String, Long> body);

  /**
   * Obtiene un Establishment con su InvoiceIssuancePoint asociado por el ID del punto de emisión.
   * Esta estructura es compatible con EstablishmentDto de ebill-billing.
   *
   * @param idIssuancePoint ID del punto de emisión
   * @return DTO con la información del establecimiento y su punto de emisión
   */
  @GetMapping("/api/invoice-issuance-points/{idIssuancePoint}/with-establishment")
  EstablishmentDto getEstablishmentWithIssuancePoint(
      @PathVariable("idIssuancePoint") Long idIssuancePoint);

  /**
   * Obtiene la información de una compañía por su ID.
   *
   * @param idCompany ID de la compañía
   * @return DTO con la información de la compañía
   */
  @GetMapping("/api/company/{idCompany}")
  CompanyDto getCompanyById(@PathVariable("idCompany") Long idCompany);

  /**
   * Obtiene empresas activas excluyendo las que tienen IDs en la lista.
   *
   * @param excludeIds Lista de IDs de empresas a excluir
   * @return Lista de empresas que no están en excludeIds
   */
  @GetMapping("/api/company/without-ids")
  List<CompanyWithoutSubscriptionDto> getCompaniesWithoutIds(
      @RequestParam(value = "excludeIds", required = false) List<Long> excludeIds);
}
