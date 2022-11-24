package com.wallparisoft.mapper;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    User convertUserDtoToUser(UserDto userDto);
    @InheritInverseConfiguration
    UserDto convertUserToUserDto(User user);
}
