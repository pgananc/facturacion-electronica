package com.wallparisoft.authentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionValidationResponseDto {
  private boolean valid;
  private String reason;
  private UserInfoDto user;
  private CompanyInfoDto company;
  private EstablishmentInfoDto establishment;
  private InvoicePointInfoDto invoicePoint;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfoDto {
    private String userName;
    private String name;
    private java.util.List<String> roles;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CompanyInfoDto {
    private Long idCompany;
    private String name;
    private String identification;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EstablishmentInfoDto {
    private Long idEstablishment;
    private String name;
    private String code;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InvoicePointInfoDto {
    private Long idInvoicePoint;
    private String code;
  }
}
