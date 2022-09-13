package com.wallparisoft.ebill.product.response;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = PRIVATE)
public class ProductResponse extends BasicResponse {

  List<ProductDto> productDtos;
  ProductDto productDto;

}
