package com.wallparisoft.service.impl;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.entity.Role;
import com.wallparisoft.entity.User;
import com.wallparisoft.repository.ILoginRepository;
import com.wallparisoft.response.TokenResponse;
import com.wallparisoft.service.ILoginService;
import com.wallparisoft.service.IRoleService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginServiceImpl implements ILoginService {


    final ILoginRepository loginRepository;
    final IRoleService roleService;

    final IAuthService authService;

    public LoginServiceImpl(ILoginRepository loginRepository, IRoleService roleService, IAuthService authService) {
        this.loginRepository = loginRepository;
        this.roleService = roleService;
        this.authService = authService;
    }

    @Override
    public User validateUserName(String userName, String password) throws Exception {
        return loginRepository.findByUserNameAndPasswordAndStatus(userName, password, true);
    }

    @Override
    public TokenResponse authentication(UserDto userDto) throws Exception {
        User user = validateUserName(userDto.getUserName(), userDto.getPassword());
        List<Role> roles = roleService.findByUserNameAndStatus(user.getUserName(), true);
        List<String> rolesName = roles.stream().map(role -> {
            return role.getName();
        }).collect(Collectors.toList());
        if (Objects.isNull(user)) {
            return TokenResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .token(null)
                    .code(0)
                    .build();
        }
        TokenInformation informationEntity = new TokenInformation();
        informationEntity.setUserName(user.getMail());
        informationEntity.setRoles(rolesName);
        TokenSession token = authService.generateToken(informationEntity, Boolean.FALSE);
        return TokenResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .code(1)
                .token(token)
                .build();
    }
}
