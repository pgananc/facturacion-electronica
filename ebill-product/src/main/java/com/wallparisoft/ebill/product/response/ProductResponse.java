package com.wallparisoft.ebill.product.response;

import com.wallparisoft.ebill.product.dto.ProductDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductResponse extends BasicResponse {

  private List<ProductDto> productDtos;
  private ProductDto productDto;
}
