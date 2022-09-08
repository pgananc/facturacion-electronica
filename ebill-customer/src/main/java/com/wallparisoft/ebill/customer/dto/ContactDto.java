package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

@Data
public class ContactDto {

    private Long idContact;
    private String contactType;
    private String value;
    private Boolean status;
}
