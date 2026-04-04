package com.wallparisoft.ebillbilling.dto;

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
public class InvoiceDetailDto {

  private Long idInvoiceDetail;
  // TODO eliminar el id de producto validar
  private Long idProduct;

  private ProductDto productDto;

  private BigDecimal unitPrice;

  private BigDecimal discount;

  private BigDecimal subtotal;

  private Integer quantity;

  // Campos adicionales para facilitar edición de borradores
  private String code; // Código del producto (MainCode)
  private String mainCode; // Alias de code
  private String auxCode; // Código auxiliar
  private String description; // Nombre/descripción
  private BigDecimal ivaPercentage; // Porcentaje de IVA aplicado
  private BigDecimal ivaValue; // Valor del IVA calculado
  private BigDecimal iceValue; // Valor del ICE
}
