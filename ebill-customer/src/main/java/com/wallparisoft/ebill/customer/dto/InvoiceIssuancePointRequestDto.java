package com.wallparisoft.ebill.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InvoiceIssuancePointRequestDto {

  @NotBlank(message = "El código es obligatorio")
  @Pattern(
      regexp = "^[0-9]{3}$",
      message = "El código debe ser exactamente 3 dígitos numéricos (001-999)")
  private String code;

  @NotNull(message = "El ID del establecimiento es obligatorio")
  private Long establishmentId;

  /**
   * Secuencial de facturas. OPCIONAL en actualización. Si se proporciona (y es > 0), debe ser >= 1
   * y no puede ser menor que el valor actual. Si es 0 o negativo, se ignora (se trata como null).
   */
  private Long sequentialInvoice;

  private Boolean status = true; // Default true
}
