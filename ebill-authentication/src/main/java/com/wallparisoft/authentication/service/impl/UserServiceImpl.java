package com.wallparisoft.authentication.service.impl;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_002;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.RoleDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.dto.UserRequestDto;
import com.wallparisoft.authentication.entity.Params;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.Token;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserCompanyRole;
import com.wallparisoft.authentication.exception.RateLimitExceededException;
import com.wallparisoft.authentication.exception.ResourceExistsException;
import com.wallparisoft.authentication.mapper.RoleMapper;
import com.wallparisoft.authentication.mapper.UserMapper;
import com.wallparisoft.authentication.repository.IParamsRepository;
import com.wallparisoft.authentication.repository.IRoleRepository;
import com.wallparisoft.authentication.repository.ITokenRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.repository.IUserCompanyRoleRepository;
import com.wallparisoft.authentication.repository.IUserRepository;
import com.wallparisoft.authentication.response.UserDtoResponse;
import com.wallparisoft.authentication.service.IGenericRateLimitService;
import com.wallparisoft.authentication.service.IUserService;
import com.wallparisoft.authentication.util.AESEncryptionUtil;
import com.wallparisoft.ebill.utils.dto.MailDto;
import com.wallparisoft.ebill.utils.enums.ParmsAuthEnum;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = PRIVATE)
@Slf4j
public class UserServiceImpl implements IUserService {

  final PasswordEncoder passwordEncoder;

  final IUserRepository userRepository;

  final IRoleRepository roleRepository;

  final IUserCompanyRoleRepository userCompanyRoleRepository;

  final ITokenRepository tokenRepository;

  final IParamsRepository paramsRepository;

  final EmailUtil emailUtil;
  final UserMapper userMapper;
  final RoleMapper roleMapper;
  final IUserCompanyRepo userCompanyRepository;
  final AESEncryptionUtil aesEncryptionUtil;

  final IGenericRateLimitService rateLimitService;

  @Value("${spring.mail.username}")
  private String mailNotification;

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
    return this.userRepository
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("User not found for this id :: " + id));
  }

  @Override
  public void delete(Long id) {
    User user = findById(id);
    user.setStatus(false);
    userRepository.save(user);
  }

  @Override
  public Page<UserDto> findUserByIdCompanyUserNameOrNameOrStatus(
      Long idCompany, String name, String userName, Boolean status, Pageable pageable) {

    Page<UserCompany> users = userCompanyRepository.findUserByIdCompanyUserNameOrNameOrStatus(
        idCompany,
        "%" + name.toUpperCase() + "%",
        "%" + userName.toUpperCase() + "%",
        status,
        pageable);
    List<UserDto> userList = new ArrayList<>();

    users.getContent().parallelStream()
        .forEach(
            user -> {
              UserDto userDto = userMapper.convertUserToUserDto(user.getUser());
              userDto.setRoleDtos(findRoleByUserCompany(user));
              userList.add(userDto);
            });
    return new PageImpl<>(userList, pageable, users.getTotalElements());
  }

  @Override
  public UserDtoResponse findUsersByCompanyAndStatusActive(Long idCompany) {
    List<UserDto> userDtoList = new ArrayList<>();

    List<UserCompany> userList = userCompanyRepository.findUsersByCompanyAndStatusActive(idCompany);
    userList.parallelStream()
        .forEach(
            user -> {
              UserDto userDto = userMapper.convertUserToUserDto(user.getUser());
              userDto.setRoleDtos(findRoleByUserCompany(user));
              userDtoList.add(userDto);
            });
    return UserDtoResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .userDtos(userDtoList)
        .build();
  }

  @Transactional
  @Override
  public void registerUserWithRoles(UserRequestDto userRequestDto) {
    if (userRepository.findByUserName(userRequestDto.getUserName()).isPresent()) {
      throw new ResourceExistsException("Usuario ya existe");
    }

    List<Long> roleIds = userRequestDto.getRoleDtos().stream().map(RoleDto::getIdRole).collect(Collectors.toList());

    List<Role> roles = roleRepository.findAllById(roleIds);

    if (roles.size() != roleIds.size()) {
      throw new ModelNotFoundException("Uno o más roles no existen.");
    }

    // Guardar usuario
    User user = userMapper.userRequestDtoToUser(userRequestDto);
    // Guardar password hasheado (bcrypt) para login
    user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
    // Guardar password encriptado (AES) para recuperación por superadmins
    user.setPasswordEncrypted(aesEncryptionUtil.encrypt(userRequestDto.getPassword()));
    user = userRepository.save(user);

    // Guardar user_company
    UserCompany userCompany = UserCompany.builder()
        .user(user)
        .idCompany(userRequestDto.getIdCompany())
        .creationDate(LocalDateTime.now())
        .build();
    userCompany = userCompanyRepository.save(userCompany);

    // Asignar roles con builder
    for (Role role : roles) {
      UserCompanyRole userCompanyRole = UserCompanyRole.builder()
          .userCompany(userCompany)
          .role(role)
          .status(userRequestDto.getStatus()) // este sí viene desde el DTO
          .build();
      userCompanyRoleRepository.save(userCompanyRole);
    }
  }

  @Transactional
  @Override
  public void update(UserDto userDto, Long idUser) {
    // Paso 1: Obtener el usuario existente o lanzar excepción si no existe
    User existingUser = userRepository
        .findById(idUser)
        .orElseThrow(
            () -> new ModelNotFoundException("Usuario con ID " + idUser + " no encontrado"));

    // Paso 2: Actualizar datos del usuario (manteniendo creationDate)
    existingUser.setUserName(userDto.getUserName());
    existingUser.setMail(userDto.getMail());
    existingUser.setName(userDto.getName());
    existingUser.setStatus(userDto.getStatus());
    existingUser.setUpdateDate(LocalDateTime.now());

    // Solo actualizar la contraseña si viene una nueva (no null y no vacía)
    // El password viene null desde el frontend gracias al mapper actualizado
    if (userDto.getPassword() != null && StringUtils.isNotBlank(userDto.getPassword())) {
      // Actualizar password hasheado (bcrypt) para login
      existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
      // Actualizar password encriptado (AES) para recuperación por superadmins
      existingUser.setPasswordEncrypted(aesEncryptionUtil.encrypt(userDto.getPassword()));
    }

    userRepository.save(existingUser);

    // Paso 3: Buscar UserCompany según usuario y compañía
    UserCompany userCompany = userCompanyRepository
        .findByUser_IdUserAndIdCompany(idUser, userDto.getIdCompany())
        .orElseThrow(
            () -> new ModelNotFoundException("Relación usuario/compañía no encontrada"));

    // Paso 4: Validar y convertir roles
    List<Long> roleIds = userDto.getRoleDtos().stream().map(RoleDto::getIdRole).collect(Collectors.toList());

    List<Role> roles = roleRepository.findAllById(roleIds);

    if (roles.size() != roleIds.size()) {
      throw new ModelNotFoundException("Uno o más roles proporcionados no existen.");
    }

    // Paso 5: Obtener roles actuales
    List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserCompany(userCompany);

    // Mapear roles actuales a sus IDs
    Set<Long> existingRoleIds = existingRoles.stream().map(ur -> ur.getRole().getIdRole()).collect(Collectors.toSet());

    // Mapear nuevos roles a sus IDs
    Set<Long> incomingRoleIds = roles.stream().map(Role::getIdRole).collect(Collectors.toSet());

    // Paso 6.1: Eliminar roles que ya no están en la nueva lista
    for (UserCompanyRole existing : existingRoles) {
      if (!incomingRoleIds.contains(existing.getRole().getIdRole())) {
        userCompanyRoleRepository.delete(existing);
      }
    }

    // Paso 6.2: Agregar nuevos roles que antes no existían
    for (Role role : roles) {
      if (!existingRoleIds.contains(role.getIdRole())) {
        UserCompanyRole userCompanyRole = UserCompanyRole.builder()
            .userCompany(userCompany)
            .role(role)
            .status(userDto.getStatus()) // o existingUser.getStatus()
            .build();
        userCompanyRoleRepository.save(userCompanyRole);
      }
    }
  }

  @Override
  public Boolean existsByUserName(String userName) {
    return userCompanyRepository.existsByUser_UserName(userName);
  }

  @Override
  public User findByMailAndStatus(String mail) {
    return userRepository.findByMailAndStatus(mail, true);
  }

  @Override
  @Transactional
  public Integer sendMailUser(String mail, String clientIp) {
    // Validar rate limiting por email
    if (!rateLimitService.canMakeRequest(
        "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL, mail)) {
      long secondsLeft = rateLimitService.getTimeUntilNextAttempt(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL, mail);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts = rateLimitService.getCurrentAttempts(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL, mail);
      int maxAttempts = rateLimitService.getMaxRequests(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
      int windowMinutes = rateLimitService.getWindowMinutes(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
      String message = String.format(
          "Máximo de solicitudes alcanzado por email (%d/%d). Intente nuevamente en %d minutos",
          currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // Validar rate limiting por IP
    if (clientIp != null
        && !rateLimitService.canMakeRequest(
            "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp)) {
      long secondsLeft = rateLimitService.getTimeUntilNextAttempt(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts = rateLimitService.getCurrentAttempts(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      int maxAttempts = rateLimitService.getMaxRequests(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP);
      int windowMinutes = rateLimitService.getWindowMinutes(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP);
      String message = String.format(
          "Máximo de solicitudes alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
          currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    int result = 0;

    User user = findByMailAndStatus(mail);
    if (user != null && user.getIdUser() > 0) {
      Params paramsTime = paramsRepository.findParamsByCodeParam(
          ParmsAuthEnum.COD002_TIME_DURATION_TOKEN.getCode());
      Token token = Token.builder().token(UUID.randomUUID().toString()).user(user).status(true).build();
      token.setExpirationDate(Integer.parseInt(paramsTime.getValueParam()));
      tokenRepository.save(token);

      Params paramsMail = paramsRepository.findParamsByCodeParam(
          ParmsAuthEnum.COD001_MAIL_TEMPLATE_RESTORE_PASSWORD.getCode());
      Params paramsURL = paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD003_URL_REST_PASS.getCode());
      String html = paramsMail.getValueParam();
      html = html.replace("$${user}", user.getName())
          .replace("$${duration}", paramsTime.getValueParam())
          .replace("$${resetUrl}", paramsURL.getValueParam() + token.getToken())
          .replace("$${year}", String.valueOf(LocalDateTime.now().getYear()));

      MailDto mailDto = MailDto.builder()
          .from(mailNotification)
          .to(user.getMail())
          .subject("RESTABLECER CONTRASEÑA - WALLPARISOFT")
          .templateHtml(html)
          .build();
      emailUtil.sendMail(mailDto);
      result = 1;

      // Incrementar contadores de rate limiting después de envío exitoso
      rateLimitService.incrementRequest(
          "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL, mail);
      if (clientIp != null) {
        rateLimitService.incrementRequest(
            "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      }
    }
    return result;
  }

  @Override
  public boolean resetPassword(String token, String password) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(token),
        REQUEST,
        LEVEL_002);
    try {
      // Get user ID without loading User entity (avoids stale entity overwriting bulk
      // UPDATE)
      Long idUser = tokenRepository.findUserIdByToken(token);
      if (Objects.isNull(idUser)) {
        throw new ModelNotFoundException("Token no válido o expirado");
      }
      changePassword(password, idUser);
      tokenRepository.deleteByToken(token);
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(token),
          RESPONSE,
          LEVEL_002);
      throw new ModelNotFoundException("Error al actualizar password");
    }

    return true;
  }

  @Override
  public void changePassword(String password, Long idUser) throws Exception {
    // Validate user exists without loading entity (avoids persistence context
    // overwriting bulk UPDATE)
    if (!userRepository.existsById(idUser)) {
      throw new ModelNotFoundException("Usuario con ID " + idUser + " no encontrado");
    }

    // Hash password with bcrypt for login
    String hashedPassword = passwordEncoder.encode(password);

    // Encrypt password with AES for super admin recovery
    String encryptedPassword = aesEncryptionUtil.encrypt(password);

    // Update both password fields via bulk UPDATE (no User entity loaded)
    userRepository.changePassword(hashedPassword, encryptedPassword, idUser);
  }

  @Override
  public UserDtoResponse findActiveUsersAndNotInCompany(Long idCompany) {
    List<User> userList = userRepository.findActiveUsersAndNotInCompany(idCompany).orElse(null);
    List<UserDto> userDtoList = new ArrayList<>();
    if (userList != null) {
      userList.parallelStream()
          .forEach(
              user -> {
                UserDto clientDto = userMapper.convertUserToUserDto(user);
                userDtoList.add(clientDto);
              });
    }
    return UserDtoResponse.builder().status(HttpStatus.OK.name()).userDtos(userDtoList).build();
  }

  @Override
  public List<RoleDto> findRoleByUserCompany(UserCompany userCompany) {
    List<UserCompanyRole> userCompanyRoles = userCompanyRoleRepository.findByUserCompanyAndStatus(userCompany, true);

    List<Role> roleList = userCompanyRoles.stream()
        .map(UserCompanyRole::getRole)
        .distinct() // opcional, por si el usuario tiene múltiples roles iguales en distintas
        // compañías
        .toList();

    return roleMapper.convertRoleListToRoleDtoList(roleList);
  }

  @Override
  public Boolean existsByMail(String mail) {
    return userCompanyRepository.existsByUser_Mail(mail);
  }

  @Override
  public UserDtoResponse findUserByUserNameAndPassword(String userName, String password) {
    // Find user by userName and status (password is hashed, so we can't query by it
    // directly)
    User user = userRepository.findByUserNameAndStatus(userName, true);

    if (user == null) {
      throw new ModelNotFoundException("Usuario no encontrado o inactivo");
    }

    // Validate password using bcrypt
    boolean passwordValid = passwordEncoder.matches(password, user.getPassword());
    if (!passwordValid) {
      throw new ModelNotFoundException("Credenciales incorrectas");
    }

    // Convert to DTO
    // Set the password from the request (already validated) so frontend can compare
    // it
    // This ensures password_encrypted desync issues don't cause false negatives
    UserDto userDto = userMapper.convertUserToUserDto(user);
    userDto.setPassword(password); // Use the validated password from request

    return UserDtoResponse.builder().status(HttpStatus.OK.name()).userDto(userDto).build();
  }

  @Override
  public UserDtoResponse findUserByUserName(String userName) {
    User user = userRepository.findByUserNameAndStatus(userName, true);
    UserDto userDto = userMapper.convertUserToUserDto(user);
    return UserDtoResponse.builder().status(HttpStatus.OK.name()).userDto(userDto).build();
  }
}
