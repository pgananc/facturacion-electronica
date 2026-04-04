package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO con información básica de un producto asociado a un comprobante (factura, nota de crédito o
 * nota de débito). Los nombres de los campos están homologados con la entidad Product y ProductDto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductInfoDto {

  /** ID del producto */
  Long idProduct;

  /** Código principal del producto (mainCode) */
  String mainCode;

  /** Código auxiliar del producto (si existe) */
  String auxiliarCode;

  /** Nombre del producto */
  String name;

  /** Cantidad del producto en el comprobante */
  Integer quantity;

  /** Precio unitario del producto */
  BigDecimal unitPrice;

  /** Descuento aplicado al producto */
  BigDecimal discount;

  /** Subtotal del producto (quantity * unitPrice - discount) */
  BigDecimal subtotal;
}
