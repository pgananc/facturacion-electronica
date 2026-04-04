package com.wallparisoft.ebill.customer.dto;

import com.wallparisoft.ebill.customer.validation.ValidEcuadorianIdentification;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyDto implements Serializable {
  Long idCompany;
  Integer idType;

  @ValidEcuadorianIdentification(message = "RUC de la compañía inválido")
  String identification;

  String branchOfficeCode;
  String type;
  String name;
  String nameCommercial;
  String forcedToAccounting;
  String specialTaxpayerResolution;
  String logoPath;
  Long digitalSignatureId;
  Boolean status;
  Boolean rimpeRegisteredTaxpayer;
  Boolean smallScaleInformalBusiness;
  Boolean taxWithholdingAgent;
  String regime;
}
