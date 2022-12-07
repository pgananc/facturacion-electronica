package com.wallparisoft.service.impl;

import com.wallparisoft.dto.RoleDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.dto.MailDto;
import com.wallparisoft.ebill.utils.enums.ParmsAuthEnum;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.entity.*;
import com.wallparisoft.mapper.RoleMapper;
import com.wallparisoft.mapper.UserMapper;
import com.wallparisoft.repository.*;
import com.wallparisoft.response.UserDtoResponse;
import com.wallparisoft.service.IUserService;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_002;
import static lombok.AccessLevel.PRIVATE;

@Service
@FieldDefaults(level = PRIVATE)
@Log4j2
public class UserServiceImpl implements IUserService {

    final IUserRepository userRepository;

    final IRoleRepository roleRepository;
    final IUserRoleRepository userRoleRepository;

    final ITokenRepository tokenRepository;

    final IParamsRepository paramsRepository;

    final EmailUtil emailUtil;
    final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public UserServiceImpl(IUserRepository userRepository, IRoleRepository roleRepository, IUserRoleRepository userRoleRepository, ITokenRepository tokenRepository, IParamsRepository paramsRepository, EmailUtil emailUtil, UserMapper userMapper, RoleMapper roleMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.tokenRepository = tokenRepository;
        this.paramsRepository = paramsRepository;
        this.emailUtil = emailUtil;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public User save(User entity) {
        return userRepository.save(entity);
    }


    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("User not found for this id :: " + id));
    }

    @Override
    public void delete(Long id) {
        User user = findById(id);
        user.setStatus(false);
        userRepository.save(user);
    }

    @Override
    public Page<UserDto> findUserByUserNameOrNameOrStatus(String name, String userName, Boolean status, Pageable pageable) {
        Page<User> users = userRepository.
                findUserByUserNameOrNameOrStatus("%" + name.toUpperCase() + "%", "%" + userName.toUpperCase() + "%", status, pageable);
        List<UserDto> userDtos = new ArrayList<>();
        users.getContent().parallelStream().forEach(user -> {
            UserDto userDto = userMapper.convertUserToUserDto(user);
            userDtos.add(userDto);
        });
        return new PageImpl<>(userDtos, pageable, users.getTotalElements());
    }

    @Override
    public UserDtoResponse findUsersActive() {
        List<UserDto> userDtoList = new ArrayList<>();
        List<User> userList = this.userRepository.findUsersActive();
        userList.parallelStream().forEach(user -> {
            UserDto clientDto = userMapper.convertUserToUserDto(user);
            userDtoList.add(clientDto);
        });
        return UserDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .userDtos(userDtoList)
                .build();
    }

    @Override
    public UserDtoResponse findUserWithRolById(Long idUser) {
        List<UserDto> userDtoList = new ArrayList<>();
        User user = findById(idUser);
        UserDto userDto = userMapper.convertUserToUserDto(user);
        List<Role> roles = roleRepository.findRoleActiveByIdUser(user.getIdUser());
        List<RoleDto> roleDtos = roleMapper.convertRoleListToRoleDtoList(roles);
        userDto.setRoleDtos(roleDtos);
        userDtoList.add(userDto);

        return UserDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .userDtos(userDtoList)
                .build();
    }

    @Transactional
    @Override
    public void saveUserAndRole(UserDto userDto) {
        User user = userMapper.convertUserDtoToUser(userDto);
        List<Role> roles = roleMapper.convertRoleDtoListToRoleList(userDto.getRoleDtos());
        User userSave = save(user);
        roles.forEach(role -> {
            UserRole userRole = UserRole.builder()
                    .user(userSave)
                    .role(role)
                    .status(userSave.getStatus()).build();
            userRoleRepository.save(userRole);

        });
    }

    @Transactional
    @Override
    public void update(UserDto userDto, Long idUser) {
        User userSave = findById(idUser);
        User user = userMapper.convertUserDtoToUser(userDto);
        List<Role> roles = roleMapper.convertRoleDtoListToRoleList(userDto.getRoleDtos());
        user.setCreationDate(userSave.getCreationDate());
        user.setUpdateDate(LocalDateTime.now());
        save(user);
        roles.forEach(role -> {
            UserRole userRoleSave = userRoleRepository.findByUser_IdUserAndAndRole_IdRole(user.getIdUser(), role.getIdRole());
            if (Objects.isNull(userRoleSave)) {
                userRoleRepository.deleteUserRoleByUser(user);
                UserRole userRole = UserRole.builder()
                        .user(userSave)
                        .role(role)
                        .status(user.getStatus()).build();
                userRoleRepository.save(userRole);
            }
        });
    }

    @Override
    public Boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Override
    public User findByMailAndStatus(String mail) {
        return userRepository.findByMailAndStatus(mail, true);
    }

    @Override
    @Transactional
    public Integer sendMailUser(String mail) {
        int result = 0;

        User user = findByMailAndStatus(mail);
        if (user != null && user.getIdUser() > 0) {
            Params paramsTime = paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD002_TIME_DURATION_TOKEN.getCode());
            Token token = Token.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .status(true)
                    .build();
            token.setExpirationDate(Integer.valueOf(paramsTime.getValueParam()));
            tokenRepository.save(token);

            Params paramsMail = paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD001_MAIL_TEMPLATE_RESTORE_PASSWORD.getCode());
            Params paramsURL = paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD003_URL_REST_PASS.getCode());
            String html = paramsMail.getValueParam();
            html = html.replace("${user}", user.getName())
                    .replace("${duration}", paramsTime.getValueParam())
                    .replace("${resetUrl}", paramsURL.getValueParam() + token.getToken());

            MailDto mailDto = MailDto.builder()
                   // .from(mail)
                    .to(user.getMail())
                    .subject("RESTABLECER CONTRASEÑA - WALLPARISOFT")
                    .templateHtml(html)
                    .build();
            emailUtil.sendMail(mailDto);
            result = 1;


        }
        return result;

    }

    @Override
    public boolean resetPassword(String token, String password) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(token)
                .eventType(REQUEST.name())
                .level(LEVEL_002.name())
                .build());
        try {
            Token resetToken = tokenRepository.findByToken(token);
            changePassword(password, resetToken.getUser().getIdUser());
            tokenRepository.delete(resetToken);
        } catch (Exception e) {
            log.debug(EventLog.builder()
                    .service(traceElement.getClassName())
                    .method(traceElement.getMethodName())
                    .information(token)
                    .eventType(RESPONSE.name())
                    .level(LEVEL_001.name())
                    .build());
            throw new ModelNotFoundException("Error al actualizar password");
        }

        return true;
    }

    @Override
    public void changePassword(String password, Long idUser) throws Exception {
        userRepository.changePassword(password, idUser);
    }
}
