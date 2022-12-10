package com.wallparisoft.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyDto implements Serializable {
    Long idCompany;
    List<UserDto> userDtos;
}
