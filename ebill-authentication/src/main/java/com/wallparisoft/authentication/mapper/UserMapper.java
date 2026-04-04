package com.wallparisoft.authentication.mapper;

import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.dto.UserRequestDto;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.util.AESEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class UserMapper {

  @Autowired protected AESEncryptionUtil aesEncryptionUtil;

  public static final UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

  public abstract User userRequestDtoToUser(UserRequestDto userRequestDto);

  public abstract User convertUserDtoToUser(UserDto userDto);

  @InheritInverseConfiguration
  @Mapping(target = "password", expression = "java(decryptPassword(user))")
  public abstract UserDto convertUserToUserDto(User user);

  /** Desencripta el password del usuario para mostrarlo en el DTO Solo para uso de superadmins */
  protected String decryptPassword(User user) {

    try {
      if (user != null && user.getPasswordEncrypted() != null && aesEncryptionUtil != null) {
        String decrypted = aesEncryptionUtil.decrypt(user.getPasswordEncrypted());
        return decrypted;
      }
      return null;
    } catch (Exception e) {
      log.error("Error al descriptar password", e);
      return null;
    }
  }
}
