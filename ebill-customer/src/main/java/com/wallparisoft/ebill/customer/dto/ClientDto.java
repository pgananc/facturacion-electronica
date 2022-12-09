package com.wallparisoft.ebill.customer.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
@Data
@Builder
public class ClientDto implements Serializable {

    private Long idClient;

    private Integer idType;

    private String identification;

    private String name;

    private Integer clientType;

    private Boolean status;
    private Long idCompany;
    List<ContactDto> contacts;

}
