package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.constants.AuthenticationConstant;
import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.UserCompanyRole;
import com.wallparisoft.authentication.mapper.RoleMapper;
import com.wallparisoft.authentication.repository.IRoleRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRoleRepository;
import com.wallparisoft.authentication.response.RoleDtoResponse;
import com.wallparisoft.authentication.service.IRoleService;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleServiceImpl implements IRoleService {

  final IRoleRepository roleRepository;
  final IUserCompanyRoleRepository userCompanyRoleRepository;
  final RoleMapper roleMapper;

  @Override
  public Role save(Role entity) {
    return roleRepository.save(entity);
  }

  @Override
  public List<Role> findAll() {
    return roleRepository.findAll();
  }

  @Override
  public Role findById(Long id) {
    return null;
  }

  @Override
  public RoleDtoResponse findAllActiveWithoutSuperAdmin() {
    List<RoleDto> roleList = new ArrayList<>();
    List<Role> clients =
        this.roleRepository.findByStatusAndNameNotOrderByNameAsc(
            true, AuthenticationConstant.SUPER_ADMIN);
    clients.parallelStream()
        .forEach(
            role -> {
              RoleDto clientDto = roleMapper.convertRoleToRoleDto(role);
              roleList.add(clientDto);
            });
    return RoleDtoResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .roleDtos(roleList)
        .build();
  }

  @Override
  public List<Role> findByUserNameAndStatus(String userName, Boolean status) {
    return userCompanyRoleRepository
        .findByUserCompany_User_UserNameAndStatus(userName, status)
        .stream()
        .map(UserCompanyRole::getRole)
        .toList();
  }

  @Override
  public RoleDtoResponse findAllActive() {
    List<RoleDto> roleList = new ArrayList<>();
    List<Role> clients = this.roleRepository.findByStatusOrderByNameAsc(true);
    clients.parallelStream()
        .forEach(
            role -> {
              RoleDto clientDto = roleMapper.convertRoleToRoleDto(role);
              roleList.add(clientDto);
            });
    return RoleDtoResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .roleDtos(roleList)
        .build();
  }
}
