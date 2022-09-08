package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
@Data
public class ClientDto implements Serializable {

    private Long idClient;
    @Min(value = 1)
    private Integer idType;
    @NotEmpty
    private String identification;
    @NotEmpty
    private String name;
    @NotEmpty
    private String clientType;
    @NotNull
    private Boolean status;
    private Long idCompany;
    List<ContactDto> contacts;

}
