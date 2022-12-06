package com.wallparisoft.service;

import com.wallparisoft.entity.Role;
import com.wallparisoft.response.RoleDtoResponse;



public interface IRoleService extends  ICRUD<Role>{

    RoleDtoResponse findRoleActive();
}
