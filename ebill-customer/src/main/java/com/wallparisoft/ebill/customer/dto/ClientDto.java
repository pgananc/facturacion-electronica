package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

import java.util.List;
@Data
public class ClientDto {

    private Long idClient;
    private Integer idType;
    private String identification;
    private String name;
    private String clientType;
    private Boolean status;
    private Long idCompany;
    List<ContactDto> contacts;

}
