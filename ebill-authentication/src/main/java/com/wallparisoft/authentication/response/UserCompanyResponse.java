package com.wallparisoft.authentication.response;

import com.wallparisoft.authentication.dto.UserCompanyDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyResponse extends BasicResponse {
  List<UserCompanyDto> userCompanyDtos;
  UserCompanyDto userCompanyDto;
}
