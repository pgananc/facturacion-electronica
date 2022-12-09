package com.wallparisoft.response;


import com.wallparisoft.dto.UserCompanyDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyResponse extends BasicResponse {
    List<UserCompanyDto> userCompanyDtos;
    UserCompanyDto userCompanyDto;
}
