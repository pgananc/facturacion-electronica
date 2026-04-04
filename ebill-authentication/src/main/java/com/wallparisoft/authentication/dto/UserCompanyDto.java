package com.wallparisoft.authentication.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyDto implements Serializable {
  Long idCompany;
  List<UserDto> userDtos;
}
