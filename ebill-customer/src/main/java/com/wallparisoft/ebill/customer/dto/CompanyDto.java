package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class CompanyDto implements Serializable {

    private Long idCompany;
    @Min(value = 1)
    private Integer idType;
    @NotEmpty
    private String identification;
    @NotEmpty
    private String name;
    @NotEmpty
    private String branchOfficeCode;
    @NotEmpty
    private String forcedToAccounting;
    @Min(value = 1)
    private Long specialTaxpayer;
    @NotEmpty
    private String principal;
    @NotNull
    private Boolean status;

    List<ContactDto> contacts;

}
