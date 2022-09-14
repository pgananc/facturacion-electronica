package com.wallparisoft.ebill.product.controller;

import static com.wallparisoft.ebill.product.util.EventType.REQUEST;
import static com.wallparisoft.ebill.product.util.EventType.RESPONSE;
import static com.wallparisoft.ebill.product.util.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.response.ProductResponse;
import com.wallparisoft.ebill.product.service.IProductService;
import com.wallparisoft.ebill.product.util.EventLog;
import javax.validation.Valid;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/product")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class ProductController {

  @Autowired
  IProductService productService;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public ProductResponse getAll() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    ProductResponse response = productService.getAllProducts();
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(response.getProductDtos())
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return response;
  }

  @GetMapping("/{status}")
  @ResponseStatus(HttpStatus.OK)
  public ProductResponse getAllByStatus(@PathVariable("status") boolean status) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    ProductResponse response = productService.getProductsByStatus(status);
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(response.getProductDtos())
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return response;
  }

  @PostMapping
  public ProductResponse register(@Valid @RequestBody ProductDto product) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(product)
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    ProductResponse response = productService.createProduct(product);
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(response.getProductDto())
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return response;
  }

  @DeleteMapping("/{id}")
  public ProductResponse delete(@PathVariable("id") Long id) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(id)
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    ProductResponse response = productService.delete(id);
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(response.getProductDto())
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return response;
  }

  @PatchMapping("/{idProduct}")
  public ProductResponse updateProduct(@Valid @RequestBody ProductDto product, @PathVariable Long idProduct) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information("ID product: ".concat(idProduct.toString()).concat(", Object product: ").concat(product.toString()))
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    ProductResponse response = productService.updateProduct(product, idProduct);
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(response.getProductDto())
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return response;
  }
}
