package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CompanyDto implements Serializable {

    private Long idCompany;
    private Integer idType;
    private String identification;
    private String name;
    private String branchOfficeCode;
    private String forcedToAccounting;
    private Long specialTaxpayer;
    private String principal;
    private Boolean status;

    List<ContactDto> contacts;

}
