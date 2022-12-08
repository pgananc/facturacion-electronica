package com.wallparisoft.service;

import com.wallparisoft.entity.Role;
import com.wallparisoft.response.RoleDtoResponse;

import java.util.List;


public interface IRoleService extends  ICRUD<Role>{

    RoleDtoResponse findRoleActive();

    List<Role> findByUserNameAndStatus(String userName, Boolean status);
}
