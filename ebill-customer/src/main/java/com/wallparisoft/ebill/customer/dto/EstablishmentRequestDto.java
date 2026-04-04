package com.wallparisoft.ebill.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EstablishmentRequestDto {

  @NotBlank(message = "El nombre es obligatorio")
  @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
  private String name;

  @NotBlank(message = "El código es obligatorio")
  @Pattern(
      regexp = "^[0-9]{3}$",
      message = "El código debe ser exactamente 3 dígitos numéricos (001-999)")
  private String code;

  @NotBlank(message = "La dirección es obligatoria")
  @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
  private String address;

  @NotBlank(message = "El tipo es obligatorio")
  @Pattern(regexp = "^(MATRIZ|SUCURSAL)$", message = "El tipo debe ser MATRIZ o SUCURSAL")
  private String type;

  @NotNull(message = "El ID de la compañía es obligatorio")
  private Long companyId;

  private Boolean status;
}
