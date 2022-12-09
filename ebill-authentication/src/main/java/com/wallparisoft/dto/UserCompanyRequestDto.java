package com.wallparisoft.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyRequestDto implements Serializable {
    Long idCompany;
    Long idUser;
}
