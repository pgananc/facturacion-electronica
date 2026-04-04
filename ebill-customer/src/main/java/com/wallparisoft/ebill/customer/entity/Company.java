package com.wallparisoft.ebill.customer.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Company extends BaseEntity {
  @Id
  @SequenceGenerator(sequenceName = "company_seq", name = "company_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq")
  @Column(name = "id_company")
  private Long idCompany;

  @Column(name = "id_type", nullable = false, length = 3)
  private Integer idType;

  @Column(name = "identification", nullable = false, length = 13)
  private String identification;

  @Column(name = "name")
  private String name;

  @Column(name = "name_commercial")
  private String nameCommercial;

  @Column(name = "forced_to_accounting")
  private String forcedToAccounting;

  @Column(name = "special_taxpayer_resolution")
  private String specialTaxpayerResolution;

  @Column(name = "logo_path", length = 500)
  private String logoPath;

  @Column(name = "rimpe_registered_taxpayer")
  private Boolean rimpeRegisteredTaxpayer;

  @Column(name = "small_scale_informal_business")
  private Boolean smallScaleInformalBusiness;

  @Column(name = "tax_withholding_agent")
  private Boolean taxWithholdingAgent;

  @Column(name = "regime", length = 50)
  private String regime;
}
