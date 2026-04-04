package com.wallparisoft.authentication.response;

import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class UserDtoResponse extends BasicResponse {
  private List<UserDto> userDtos;
  private UserDto userDto;
}
