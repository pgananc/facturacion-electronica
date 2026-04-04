package com.wallparisoft.authentication.mapper;

import com.wallparisoft.authentication.dto.UserCompanyDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.util.AESEncryptionUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.control.DeepClone;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", mappingControl = DeepClone.class)
@Slf4j
public abstract class UserCompanyMapper {

	@Autowired protected AESEncryptionUtil aesEncryptionUtil;
  public UserCompanyDto convertUserCompanyToUserCompanyDto(UserCompany companyClient) {
    return UserCompanyDto.builder()
        .idCompany(companyClient.getIdCompany())
        .userDtos(
            List.of(
                UserDto.builder()
                    .idUser(companyClient.getUser().getIdUser())
                    .userName(companyClient.getUser().getUserName())
                    .mail(companyClient.getUser().getMail())
                    .name(companyClient.getUser().getName())
                    .password(companyClient.getUser().getPassword())
                    .status(companyClient.getUser().getStatus())
                    .build()))
        .build();
  }

  public UserCompanyDto getUsersFromCompany(List<UserCompany> userCompanies) {
    UserCompanyDto userCompanyDto =
        UserCompanyDto.builder()
            .idCompany(userCompanies.get(0).getIdCompany())
            .userDtos(new ArrayList<>())
            .build();
    for (UserCompany companyClient : userCompanies) {
      userCompanyDto
          .getUserDtos()
          .add(
              UserDto.builder()
                  .idUser(companyClient.getUser().getIdUser())
                  .userName(companyClient.getUser().getUserName())
                  .mail(companyClient.getUser().getMail())
                  .name(companyClient.getUser().getName())
                  .password( decryptPassword(companyClient.getUser()))
                  .status(companyClient.getUser().getStatus())
                  .build());
    }
    return userCompanyDto;
  }
  
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
