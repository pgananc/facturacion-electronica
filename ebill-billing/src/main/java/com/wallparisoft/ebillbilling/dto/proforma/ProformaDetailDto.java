package com.wallparisoft.ebillbilling.dto.proforma;

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
public class ProformaDetailDto {

  Long idProformaDetail;
  Long idProforma;
  Long idProduct;
  String productCode;
  String description;
  BigDecimal quantity;
  BigDecimal unitPrice;
  BigDecimal discount;
  BigDecimal ivaPercentage;
  BigDecimal icePercentage;
  BigDecimal subtotal;
  Integer lineOrder;
}
