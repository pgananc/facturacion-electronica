package com.wallparisoft.authentication.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar menús Campos opcionales para permitir actualización parcial (PATCH)
 * Para crear: name, icon, active son requeridos Para actualizar: solo los campos que se quieren
 * modificar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuRequestDto {

  @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
  private String name;

  @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
  private String icon;

  @Pattern(regexp = "^/.*|^$|^null$", message = "La URL debe comenzar con '/' o estar vacía")
  @Size(max = 200, message = "La URL no puede exceder 200 caracteres")
  private String url;

  @Size(max = 50, message = "El feature code no puede exceder 50 caracteres")
  private String featureCode;

  private Integer parentId;

  private Boolean active;

  @Min(value = 0, message = "El orden debe ser mayor o igual a 0")
  private Integer order;
}
