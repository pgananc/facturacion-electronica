package com.wallparisoft.authentication.client;

import com.wallparisoft.authentication.dto.SubscriptionResponseDto;
import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para obtener información de suscripciones desde ebill-billing.
 * Usa FeignClientConfig para automáticamente agregar el token JWT del usuario autenticado.
 */
@FeignClient(
    name = "billing-client",
    url = "${ebill.billing.url}",
    configuration = FeignClientConfig.class)
public interface IBillingClient {

  /**
   * Obtiene la suscripción actual de una compañía.
   *
   * @param idCompany ID de la compañía
   * @return SubscriptionResponseDto con la información de la suscripción actual
   */
  @GetMapping("/api/subscription/{idCompany}/current")
  SubscriptionResponseDto getCurrentSubscription(@PathVariable("idCompany") Long idCompany);
}

