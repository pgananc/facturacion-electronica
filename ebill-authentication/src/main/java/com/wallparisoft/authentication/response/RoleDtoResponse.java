package com.wallparisoft.authentication.response;

import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class RoleDtoResponse extends BasicResponse {
  private List<RoleDto> roleDtos;
}
