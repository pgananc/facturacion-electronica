package com.wallparisoft.ebill.product.mapper;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.response.ProductResponse;
import java.util.List;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

  @Mapping(target = "idProduct", ignore = true)
  @Mapping(target = "status", source = "status")
  @Mapping(target = "creationDate", ignore = true)
  @Mapping(target = "updateDate", ignore = true)
  @Mapping(target = "ivaTaxType", source = "ivaTaxType")
  @Mapping(target = "iceTaxType", source = "iceTaxType")
  Product toProduct(ProductDto dto);

  @Mapping(
      target = "idProduct",
      expression = "java(product.getIdProduct() != null ? product.getIdProduct().intValue() : 0)")
  @Mapping(target = "ivaTaxType", source = "ivaTaxType")
  @Mapping(target = "iceTaxType", source = "iceTaxType")
  ProductDto toProductDto(Product product);

  @Mapping(target = "productDto", source = ".")
  @Mapping(target = "productDtos", ignore = true)
  ProductResponse toProductResponse(Product product);

  default ProductResponse toProductResponse(List<Product> products) {
    List<ProductDto> dtos = products.stream().map(this::toProductDto).toList();
    return ProductResponse.builder().productDtos(dtos).build();
  }
}
