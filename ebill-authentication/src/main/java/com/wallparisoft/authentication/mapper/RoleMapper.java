package com.wallparisoft.authentication.mapper;

import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.authentication.entity.Role;
import java.util.List;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RoleMapper {

  RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

  Role convertRoleDtoToRole(RoleDto roleDto);

  List<Role> convertRoleDtoListToRoleList(List<RoleDto> list);

  @InheritInverseConfiguration
  RoleDto convertRoleToRoleDto(Role role);

  List<RoleDto> convertRoleListToRoleDtoList(List<Role> list);
}
