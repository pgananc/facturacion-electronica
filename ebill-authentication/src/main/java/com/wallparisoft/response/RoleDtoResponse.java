package com.wallparisoft.response;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import com.wallparisoft.entity.Role;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
public class RoleDtoResponse extends BasicResponse {
    List<RoleDto> roleDtos;
}
