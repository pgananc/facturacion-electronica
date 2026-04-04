package com.wallparisoft.ebill.product.dto;

import java.io.Serializable;
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
public class ProductDto implements Serializable {

  Integer idProduct;
  Long idCompany;
  String mainCode;
  String name;
  Integer productType;
  Double unitPrice;
  String description;
  Boolean ice;
  Boolean tourismIva;
  Boolean status;
  TaxTypeDto ivaTaxType;
  TaxTypeDto iceTaxType;
}
