package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebill.auth.util.config.FeignClientConfig;
import com.wallparisoft.ebillbilling.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "product-service",
    url = "${service.product.base-url}",
    configuration = FeignClientConfig.class)
public interface ProductClient {

  @GetMapping("/api/product/id/{idProduct}")
  ProductResponse getProductById(@PathVariable("idProduct") Long idProduct);
}
