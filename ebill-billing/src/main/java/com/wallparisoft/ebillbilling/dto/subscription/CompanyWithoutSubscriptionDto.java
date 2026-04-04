package com.wallparisoft.ebillbilling.dto.subscription;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para empresas sin suscripción activa. Compatible con CompanyDetailsDto de ebill-customer.
 * Usado en el diálogo "Asignar Suscripción".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignorar campos extra como contacts, establishments
public class CompanyWithoutSubscriptionDto {

  /** ID de la empresa */
  private Long idCompany;

  /**
   * Razón social de la empresa. En ebill-customer se llama 'name', aquí lo mapeamos a
   * 'businessName' para el frontend.
   */
  @JsonAlias({"name", "businessName"}) // Acepta 'name' de ebill-customer o 'businessName'
  private String businessName;

  /**
   * RUC/Identificación tributaria. En ebill-customer se llama 'identification', aquí lo mapeamos a
   * 'ruc'.
   */
  @JsonAlias({"identification", "ruc"}) // Acepta 'identification' de ebill-customer o 'ruc'
  private String ruc;

  /** Nombre comercial */
  private String nameCommercial;

  /** Tipo de identificación */
  private Integer idType;

  /** Estado */
  private Boolean status;

  /** Ruta del logo */
  private String logoPath;

  /** Régimen tributario */
  private String regime;
}
