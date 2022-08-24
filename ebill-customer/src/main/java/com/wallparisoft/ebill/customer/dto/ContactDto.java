package com.wallparisoft.ebill.customer.dto;

import lombok.Data;

@Data
public class ContactDto {

    private String contactType;
    private String value;
    private Boolean status;
}
