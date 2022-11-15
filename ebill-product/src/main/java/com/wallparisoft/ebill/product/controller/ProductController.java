package com.wallparisoft.ebill.product.controller;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.response.ProductResponse;
import com.wallparisoft.ebill.product.service.IProductService;
import com.wallparisoft.ebill.product.util.EventLog;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

import static com.wallparisoft.ebill.product.util.EventType.REQUEST;
import static com.wallparisoft.ebill.product.util.EventType.RESPONSE;
import static com.wallparisoft.ebill.product.util.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

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

    @GetMapping("/products-status/{status}")
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

    @GetMapping("/{idProduct}")
    public ResponseEntity<ProductResponse> findProductById(@PathVariable(value = "idProduct") Long idProduct) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        ProductDto productDto = productService.getProductById(idProduct);
        ProductResponse response = ProductResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .productDto(productDto)
                .productDtos(List.of(productDto))
                .build();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getProductDto())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
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

    @PostMapping("/pageable")
    public ResponseEntity<Page<ProductDto>> listPageable(Pageable pageable,
                                                         @Valid @RequestBody ProductDto productDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Object product: ".concat(productDto.toString()))
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Page<ProductDto> productDtos = productService.findProductByMainCodeOrNameOrType
                (productDto.getMainCode(),
                        productDto.getName(), productDto.getProductType(), productDto.isStatus(), pageable);
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(productDtos, HttpStatus.OK);
    }

    @GetMapping("exist/{mainCode}")
    public ResponseEntity<Boolean> existsByMainCode(@PathVariable(value = "mainCode") String mainCode) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Boolean exists = productService.existsByMainCode(mainCode);

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(exists)
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }
}
