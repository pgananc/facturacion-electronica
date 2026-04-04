package com.wallparisoft.ebill.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar el secuencial de un punto de emisión. Se usa en el endpoint PATCH basado en
 * el código del punto de emisión.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSequentialRequestDto {

  /** Nuevo valor del secuencial de facturas. Debe ser mayor o igual a 1. */
  @NotNull(message = "El secuencial no puede ser nulo")
  @Min(value = 1, message = "El secuencial debe ser mayor o igual a 1")
  private Long sequentialInvoice;
}
