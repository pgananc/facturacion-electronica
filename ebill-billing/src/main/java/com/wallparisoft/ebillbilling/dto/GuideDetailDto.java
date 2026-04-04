package com.wallparisoft.ebillbilling.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideDetailDto implements Serializable {

  Long idGuideDetail;

  Long idGuide;

  Long idProduct;

  ProductDto productDto;

  String description;

  BigDecimal quantity;

  String unit;

  BigDecimal weight;

  String additionalInfo;
}
