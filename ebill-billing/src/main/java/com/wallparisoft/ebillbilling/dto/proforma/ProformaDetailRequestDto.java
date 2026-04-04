package com.wallparisoft.ebillbilling.dto.proforma;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class ProformaDetailRequestDto {

  Long idProduct;

  String productCode;

  String description;

  @NotNull(message = "quantity is required")
  @DecimalMin(value = "0.0001", message = "quantity must be positive")
  BigDecimal quantity;

  @NotNull(message = "unitPrice is required")
  @DecimalMin(value = "0", message = "unitPrice must be >= 0")
  BigDecimal unitPrice;

  @Builder.Default
  @DecimalMin(value = "0", message = "discount must be >= 0")
  BigDecimal discount = BigDecimal.ZERO;

  @Builder.Default
  @DecimalMin(value = "0", message = "ivaPercentage must be >= 0")
  BigDecimal ivaPercentage = BigDecimal.ZERO;

  @Builder.Default
  @DecimalMin(value = "0", message = "icePercentage must be >= 0")
  BigDecimal icePercentage = BigDecimal.ZERO;

  @NotNull(message = "lineOrder is required")
  Integer lineOrder;
}
