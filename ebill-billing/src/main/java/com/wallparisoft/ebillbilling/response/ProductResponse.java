package com.wallparisoft.ebillbilling.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductResponse extends BasicResponse {

  private List<Map<String, Object>> productDtos;
  private Map<String, Object> productDto;
}
