package com.wallparisoft.ebillbilling.dto;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
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
public class ProductDto {

  Long idProduct;
  String mainCode;
  String auxiliarCode;
  Integer productType;
  String name;
  String description;
  double unitPrice;
  double discount;
  boolean status;
  Long idCompany;
  BigDecimal rate;
  String taxCode;
  String taxCodPercentage;

  // Campos para ICE (Impuesto a los Consumos Especiales)
  BigDecimal iceRate;
  String iceTaxCode;
  String iceTaxCodPercentage;
}
