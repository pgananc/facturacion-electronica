package com.wallparisoft.response;


import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@SuperBuilder
@Getter
public class UserDtoResponse extends BasicResponse {
    private List<UserDto> userDtos;

}
