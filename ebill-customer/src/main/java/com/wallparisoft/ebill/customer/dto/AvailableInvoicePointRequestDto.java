package com.wallparisoft.ebill.customer.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class AvailableInvoicePointRequestDto {

  @NotEmpty(message = "La lista de establecimientos no puede estar vacía")
  private List<Long> idEstablishments;
}
