package com.wallparisoft.ebill.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import static lombok.AccessLevel.PRIVATE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = PRIVATE)
public class ProductDto {

  int idProduct;
  String mainCode;
  String auxiliarCode;
  Integer productType;
  String name;
  String description;
  double unitPrice;
  double discount;
  boolean status;
}
