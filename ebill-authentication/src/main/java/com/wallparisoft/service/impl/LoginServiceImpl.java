package com.wallparisoft.service.impl;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import com.wallparisoft.entity.UserCompany;
import com.wallparisoft.repository.ILoginRepository;
import com.wallparisoft.repository.IUserCompanyRepo;
import com.wallparisoft.response.TokenResponse;
import com.wallparisoft.service.ILoginService;
import com.wallparisoft.service.IRoleService;
import com.wallparisoft.service.IUserCompanyService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginServiceImpl implements ILoginService {


    final ILoginRepository loginRepository;
    final IRoleService roleService;
    final IUserCompanyRepo userCompanyRepo;

    final IAuthService authService;

    public LoginServiceImpl(ILoginRepository loginRepository, IRoleService roleService, IUserCompanyRepo userCompanyRepo, IAuthService authService) {
        this.loginRepository = loginRepository;
        this.roleService = roleService;
        this.userCompanyRepo = userCompanyRepo;
        this.authService = authService;
    }

    @Override
    public User validateUserName(String userName, String password) throws Exception {
        return loginRepository.findByUserNameAndPasswordAndStatus(userName, password, true);
    }

    @Override
    public TokenResponse authentication(UserDto userDto) throws Exception {
        User user = validateUserName(userDto.getUserName(), userDto.getPassword());

        if (Objects.isNull(user)) {
            return TokenResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .token(null)
                    .code(0)
                    .build();
        }
        List<Role> roles = roleService.findByUserNameAndStatus(user.getUserName(), true);
        if (Objects.isNull(roles) || roles.isEmpty()) {
            return TokenResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .token(null)
                    .code(0)
                    .build();
        }
        List<String> rolesName = roles.stream().map(role -> {
            return role.getName();
        }).collect(Collectors.toList());
        Optional<List<UserCompany>> userCompanyList = userCompanyRepo.findByUser_IdUser(user.getIdUser());
        List<Long> idCompanies = new ArrayList<>();
        if (userCompanyList.isPresent()) {
            idCompanies = userCompanyList.get().stream().map(userCompany -> {
                return userCompany.getIdCompany();
            }).collect(Collectors.toList());
        }
        TokenInformation informationEntity = new TokenInformation();
        informationEntity.setUserName(user.getMail());
        informationEntity.setRoles(rolesName);
        informationEntity.setIdCompanies(idCompanies);
        TokenSession token = authService.generateToken(informationEntity, Boolean.FALSE);
        return TokenResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .code(1)
                .token(token)
                .build();
    }
}
