package com.wallparisoft.ebill.product.dto;

import static lombok.AccessLevel.PRIVATE;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

  int idProduct;
  @NotEmpty
  String mainCode;
  @NotEmpty
  @NotNull
  String auxiliarCode;
  @NotEmpty
  String productType;
  @NotEmpty
  String name;
  @NotEmpty
  String description;
  double unitPrice;
  double discount;
  boolean status;
}
