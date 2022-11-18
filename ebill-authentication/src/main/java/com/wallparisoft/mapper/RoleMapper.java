package com.wallparisoft.mapper;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.entity.Role;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);
    Role convertRoleDtoToRole(RoleDto roleDto);

    List<Role> convertRoleDtoListToRoleList(List<RoleDto> list);
    @InheritInverseConfiguration
    RoleDto convertRoleToRoleDto(Role role);

    List<RoleDto> convertRoleListToRoleDtoList(List<Role> list);
}
