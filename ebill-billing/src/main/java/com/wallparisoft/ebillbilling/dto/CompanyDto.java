package com.wallparisoft.ebillbilling.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyDto implements Serializable {

  Long idCompany;
  Integer idType;
  String identification;
  String name;
  String nameCommercial;
  String branchOfficeCode;
  String forcedToAccounting;
  Long specialTaxpayer;
  String principal;
  Boolean status;
  List<ContactDto> contacts;
  List<Long> companiesIds;
  Boolean isSuperAdmin;
  String logoPath; // Ruta del logo de la compañía
  String regime; // Régimen tributario (GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL)
  String specialTaxpayerResolution; // Resolución de contribuyente especial
}
