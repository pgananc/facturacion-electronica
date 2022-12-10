package com.wallparisoft.mapper;


import com.wallparisoft.dto.UserCompanyDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.UserCompany;
import org.mapstruct.Mapper;
import org.mapstruct.control.DeepClone;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", mappingControl = DeepClone.class)
public abstract class UserCompanyMapper {

    public UserCompanyDto convertUserCompanyToUserCompanyDto(UserCompany companyClient) {
        return UserCompanyDto
                .builder()
                .idCompany(companyClient.getCompany())
                .userDtos(List.of(UserDto.builder()
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
        UserCompanyDto userCompanyDto = UserCompanyDto.builder()
                .idCompany(userCompanies.get(0).getCompany())
                .userDtos(new ArrayList<>()).build();
        for (UserCompany companyClient : userCompanies) {
            userCompanyDto.getUserDtos()
                    .add(UserDto.builder()
                            .idUser(companyClient.getUser().getIdUser())
                            .userName(companyClient.getUser().getUserName())
                            .mail(companyClient.getUser().getMail())
                            .name(companyClient.getUser().getName())
                            .password(companyClient.getUser().getPassword())
                            .status(companyClient.getUser().getStatus())
                            .build());
        }
        return userCompanyDto;
    }
}
