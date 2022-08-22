package com.wallparisoft.ebill.product.controller;

import static com.wallparisoft.ebill.product.util.EventType.REQUEST;
import static com.wallparisoft.ebill.product.util.EventType.RESPONSE;
import static com.wallparisoft.ebill.product.util.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.service.IProductService;
import com.wallparisoft.ebill.product.util.EventLog;
import java.net.URI;
import java.util.List;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("api/product")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class ProductController {

  @Autowired
  private IProductService productService;

  @GetMapping
  public ResponseEntity<List<Product>> getAll() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    List<Product> products = productService.findAll();
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(products)
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return new ResponseEntity<>(products, HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<Object> register(@Validated @RequestBody Product product) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(product)
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    Product productCreated = productService.save(product);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(productCreated.getIdProduct()).toUri();
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(productCreated)
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return ResponseEntity.created(location).build();
  }

  @DeleteMapping
  public ResponseEntity<Object> delete(@Validated @RequestBody Long id) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(id)
        .eventType(REQUEST.name())
        .level(LEVEL_001.name())
        .build());
    Product product = productService.findById(id);
    product.setStatus(false);
    Product productDeleted = productService.update(product, id);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(productDeleted.getIdProduct()).toUri();
    log.debug(EventLog.builder()
        .service(traceElement.getClassName())
        .method(traceElement.getMethodName())
        .information(productDeleted)
        .eventType(RESPONSE.name())
        .level(LEVEL_001.name())
        .build());
    return ResponseEntity.created(location).build();
  }
}
