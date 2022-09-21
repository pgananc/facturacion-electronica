package com.wallparisoft.ebill.product.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.response.ProductResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.control.DeepClone;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

@Log4j2
@FieldDefaults(level = PRIVATE)
@Mapper(componentModel = "spring", mappingControl = DeepClone.class)
public abstract class ProductServiceMapper {

  public Product toProduct(ProductDto productDto) {
    Product product = Product.builder()
        .productType(productDto.getProductType())
        .mainCode(productDto.getMainCode())
        .discount(productDto.getDiscount())
        .auxiliarCode(productDto.getAuxiliarCode())
        .unitPrice(productDto.getUnitPrice())
        .description(productDto.getDescription())
        .creationDate(LocalDateTime.now())
        .status(true)
        .name(productDto.getName())
        .build();
    if (productDto.getIdProduct() > 0) {
      product.setIdProduct(product.getIdProduct());
    }
    return product;
  }

  public ProductResponse toProductResponse(Product product) {
    return ProductResponse.builder()
        .productDto(ProductDto.builder()
            .idProduct(product.getIdProduct().intValue())
            .unitPrice(product.getUnitPrice())
            .productType(product.getProductType())
            .name(product.getName())
            .mainCode(product.getMainCode())
            .discount(product.getDiscount())
            .description(product.getDescription())
            .auxiliarCode(product.getAuxiliarCode())
            .status(product.isStatus())
            .build())
        .build();
  }

  public ProductResponse toProductResponse(List<Product> products) {
    List<ProductDto> productDtos = products.stream().map(product -> ProductDto.builder()
        .idProduct(product.getIdProduct().intValue())
        .productType(product.getProductType())
        .discount(product.getDiscount().doubleValue())
        .description(product.getDescription())
        .auxiliarCode(product.getAuxiliarCode())
        .mainCode(product.getMainCode())
        .name(product.getName())
        .unitPrice(product.getUnitPrice().doubleValue())
        .status(product.isStatus())
        .build()).collect(
        Collectors.toList());
    return ProductResponse.builder().productDtos(productDtos).build();
  }
}
