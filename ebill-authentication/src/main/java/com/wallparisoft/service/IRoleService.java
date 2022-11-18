package com.wallparisoft.service;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.entity.Role;

import java.util.List;

public interface IRoleService extends  ICRUD<Role>{

    List<RoleDto> findRoleActive();
}
