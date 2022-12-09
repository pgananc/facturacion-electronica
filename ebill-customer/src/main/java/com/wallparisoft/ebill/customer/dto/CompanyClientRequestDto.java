package com.wallparisoft.ebill.customer.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyClientRequestDto implements Serializable {
    Long idCompany;
    Long idClient;
}
