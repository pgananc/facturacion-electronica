package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.response.RoleDtoResponse;
import java.util.List;

public interface IRoleService extends ICRUD<Role> {

  RoleDtoResponse findAllActive();

  List<Role> findByUserNameAndStatus(String userName, Boolean status);

  RoleDtoResponse findAllActiveWithoutSuperAdmin();
}
