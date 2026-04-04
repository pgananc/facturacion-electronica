package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.dto.UserLoginRequestDto;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import com.wallparisoft.authentication.repository.ILoginRepository;
import com.wallparisoft.authentication.repository.IUserInvoicePointSessionRepository;
import com.wallparisoft.authentication.repository.IUserRepository;
import com.wallparisoft.authentication.response.TokenResponse;
import com.wallparisoft.authentication.service.ILoginService;
import com.wallparisoft.authentication.service.IRoleService;
import com.wallparisoft.authentication.service.ISessionFingerprintService;
import com.wallparisoft.authentication.service.IUserCompanyEstablishmentService;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import com.wallparisoft.ebill.auth.util.model.CompanyEstablishment;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class LoginServiceImpl implements ILoginService {

  final ILoginRepository loginRepository;
  final IRoleService roleService;
  final IUserRepository userRepository;
  final IUserCompanyEstablishmentService userCompanyEstablishmentService;
  final PasswordEncoder passwordEncoder;
  final UtilsRedis utilsRedis;
  final IUserInvoicePointSessionRepository userInvoicePointSessionRepository;
  final ISessionFingerprintService sessionFingerprintService;

  final IAuthService authService;

  private User validateUserName(String userName) {
    return loginRepository
        .findByUserNameAndStatusTrue(userName)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
  }

  @Override
  public TokenResponse authentication(UserLoginRequestDto userDto) {
    // Delegamos al método con parámetros de fingerprint nulos para compatibilidad
    return authentication(userDto, null, null);
  }

  @Override
  public TokenResponse authentication(
      UserLoginRequestDto userDto, String ipAddress, String userAgent) {
    User user = validateUserName(userDto.getUserName());
    boolean passwordValid = passwordEncoder.matches(userDto.getPassword(), user.getPassword());
    if (!passwordValid) {
      throw new BadCredentialsException("Credenciales incorrectas");
    }

    List<Role> roles = roleService.findByUserNameAndStatus(user.getUserName(), true);
    if (Objects.isNull(roles) || roles.isEmpty()) {
      return TokenResponse.builder()
          .status(HttpStatus.OK.getReasonPhrase())
          .token(null)
          .code(0)
          .build();
    }
    List<String> rolesName = roles.stream().map(Role::getName).toList();

    List<CompanyEstablishment> companyEstablishments =
        userCompanyEstablishmentService.getCompanyEstablishmentsByUser(user);

    // ⭐ OPCIÓN 1: Invalidar TODAS las sesiones activas de punto de emisión del usuario
    // Regla de negocio: Un usuario solo puede estar en UN punto de emisión a la vez
    List<UserInvoicePointSession> activeSessions =
        userInvoicePointSessionRepository.findByUser_IdUserAndActiveTrueOrderByStartTimeDesc(
            user.getIdUser());

    if (!activeSessions.isEmpty()) {
      LocalDateTime now = LocalDateTime.now();
      for (UserInvoicePointSession session : activeSessions) {
        session.setActive(false);
        session.setEndTime(now);
        userInvoicePointSessionRepository.save(session);
      }
      log.info(
          "🔄 Invalidadas {} sesiones activas previas para usuario: {}",
          activeSessions.size(),
          user.getUserName());

      // También liberar en Redis
      utilsRedis.removePointSession(user.getUserName());
    }

    TokenInformation informationEntity = new TokenInformation();
    informationEntity.setIdUser(user.getIdUser().toString());
    informationEntity.setUserName(user.getUserName());
    informationEntity.setRoles(rolesName);
    informationEntity.setCompanyEstablishments(companyEstablishments);
    TokenSession token = authService.generateToken(informationEntity, Boolean.FALSE);

    // Crear fingerprint de sesión si se proporcionan IP y User-Agent
    // Nota: sessionFingerprintService.createSessionFingerprint() ya elimina fingerprints anteriores
    UUID sessionId = null;
    if (ipAddress != null && userAgent != null) {
      try {
        sessionId =
            sessionFingerprintService.createSessionFingerprint(
                user.getUserName(), ipAddress, userAgent);
        log.info("✅ Login exitoso con fingerprint para usuario: {}", user.getUserName());
      } catch (Exception e) {
        log.error(
            "⚠️ Error creando fingerprint para usuario {}: {}", user.getUserName(), e.getMessage());
        // No fallamos el login si el fingerprint falla
      }
    }

    return TokenResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .code(1)
        .token(token)
        .sessionId(sessionId)
        .build();
  }

  @Transactional
  public void logoutUser(String userName) {
    // 1. Validar existencia del usuario
    User user =
        userRepository
            .findByUserName(userName)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

    // 2. Marcar como inactivas TODAS las sesiones activas del usuario (puede haber múltiples)
    // Esto previene el error NonUniqueResultException cuando hay sesiones duplicadas
    List<UserInvoicePointSession> activeSessions =
        userInvoicePointSessionRepository.findByUser_IdUserAndActiveTrueOrderByStartTimeDesc(
            user.getIdUser());

    if (!activeSessions.isEmpty()) {
      LocalDateTime now = LocalDateTime.now();
      for (UserInvoicePointSession session : activeSessions) {
        session.setActive(false);
        session.setEndTime(now);
        userInvoicePointSessionRepository.save(session);
      }
    }

    // 3. Eliminar fingerprint de sesión
    sessionFingerprintService.deleteSessionFingerprint(userName);

    // 4. Eliminar la sesión en Redis (token + punto de emisión)
    utilsRedis.removePointSession(userName);
    utilsRedis.removeKey("token:" + userName); // Si deseas también invalidar token

    log.info("🚪 Logout completado para usuario: {}", userName);
  }
}
