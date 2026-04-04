package com.wallparisoft.ebill.product.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.response.BasicResponse;
import com.wallparisoft.ebill.product.response.ProductResponse;
import com.wallparisoft.ebill.product.service.IProductService;
import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/product")
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class ProductController {

  final IProductService productService;

  @GetMapping("/company/{idCompany}")
  @ResponseStatus(HttpStatus.OK)
  public ProductResponse getAll(@PathVariable("idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductResponse response =
        productService.findProductsByIdCompanyAndStatusOrderByMainCode(idCompany);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDtos()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @GetMapping("/products-status/{status}")
  @ResponseStatus(HttpStatus.OK)
  public ProductResponse getAllByStatus(@PathVariable("status") boolean status) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductResponse response = productService.getProductsByStatus(status);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDtos()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @GetMapping("/id/{idProduct}")
  public ResponseEntity<ProductResponse> findProductById(
      @PathVariable(value = "idProduct") Long idProduct) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductDto productDto = productService.getProductById(idProduct);
    ProductResponse response =
        ProductResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .productDto(productDto)
            .productDtos(List.of(productDto))
            .build();
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping
  public ProductResponse register(@Valid @RequestBody ProductDto product) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductResponse response = productService.createProduct(product);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @DeleteMapping("/{idProduct}")
  public ProductResponse delete(@PathVariable("idProduct") Long idProduct) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductResponse response = productService.delete(idProduct);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @DeleteMapping("/{idProduct}/permanent")
  public BasicResponse deletePhysical(@PathVariable("idProduct") Long idProduct) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Physical delete for product ID: " + idProduct),
        EventType.REQUEST,
        Level.LEVEL_001);
    BasicResponse response = productService.deletePhysical(idProduct);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @PatchMapping("/{idProduct}")
  public ProductResponse updateProduct(
      @Valid @RequestBody ProductDto product, @PathVariable Long idProduct) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "ID product: "
                    .concat(idProduct.toString())
                    .concat(", Object product: ")
                    .concat(product.toString())),
        EventType.REQUEST,
        Level.LEVEL_001);
    ProductResponse response = productService.updateProduct(product, idProduct);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }

  @GetMapping("/pageable")
  public ResponseEntity<Page<ProductDto>> listPageable(
      Pageable pageable,
      @RequestParam(required = false) Long idCompany,
      @RequestParam(required = false) String mainCode,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Integer productType,
      @RequestParam(required = false) Boolean status) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Pageable request - idCompany: "
                    + idCompany
                    + ", mainCode: "
                    + mainCode
                    + ", name: "
                    + name),
        EventType.REQUEST,
        Level.LEVEL_001);
    Page<ProductDto> productDtos =
        productService.findProductByIdCompanyAndMainCodeOrNameOrType(
            idCompany, mainCode, name, productType, status, pageable);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return new ResponseEntity<>(productDtos, HttpStatus.OK);
  }

  @GetMapping("/exists/{mainCode}/company/{idCompany}")
  public ResponseEntity<Boolean> existsByMainCode(
      @PathVariable(value = "mainCode") String mainCode,
      @PathVariable(value = "idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        EventType.REQUEST,
        Level.LEVEL_001);
    Boolean exists = productService.existsByMainCodeAndIdCompany(mainCode, idCompany);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(exists),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return new ResponseEntity<>(exists, HttpStatus.OK);
  }

  @GetMapping("/search")
  public ResponseEntity<ProductResponse> searchProducts(
      @RequestParam(value = "term", required = false) String term) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Search term: " + (term == null ? "" : term)),
        EventType.REQUEST,
        Level.LEVEL_001);

    ProductResponse response;
    if (term == null || term.trim().isEmpty()) {
      // Si no hay término de búsqueda, retornar todos los productos activos
      response = productService.getProductsByStatus(true);
    } else {
      // Aquí puedes implementar la lógica de búsqueda por término
      // Por ahora, retornaremos productos activos (ajusta según tu necesidad)
      response = productService.getProductsByStatus(true);
    }

    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getProductDtos()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/billing/search")
  public ResponseEntity<ProductResponse> searchProductsForBilling(
      @RequestParam(value = "idCompany", required = true) Long idCompany,
      @RequestParam(value = "term", required = false) String term) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Billing search - idCompany: "
                    + idCompany
                    + ", term: "
                    + (term == null || term.isEmpty() ? "[EMPTY - All products]" : term)),
        EventType.REQUEST,
        Level.LEVEL_001);

    ProductResponse response = productService.searchProductsForBilling(idCompany, term);

    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Found "
                    + (response.getProductDtos() != null ? response.getProductDtos().size() : 0)
                    + " products"),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
