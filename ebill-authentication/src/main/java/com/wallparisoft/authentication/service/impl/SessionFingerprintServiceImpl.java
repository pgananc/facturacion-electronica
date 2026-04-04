package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.client.ICustomerClient;
import com.wallparisoft.authentication.client.IInvoiceIssuancePointClient;
import com.wallparisoft.authentication.dto.CompanyDto;
import com.wallparisoft.authentication.dto.EstablishmentDto;
import com.wallparisoft.authentication.dto.InvoiceIssuancePointDto;
import com.wallparisoft.authentication.dto.SessionValidationResponseDto;
import com.wallparisoft.authentication.dto.SessionValidationResponseDto.CompanyInfoDto;
import com.wallparisoft.authentication.dto.SessionValidationResponseDto.EstablishmentInfoDto;
import com.wallparisoft.authentication.dto.SessionValidationResponseDto.InvoicePointInfoDto;
import com.wallparisoft.authentication.dto.SessionValidationResponseDto.UserInfoDto;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.SessionFingerprint;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import com.wallparisoft.authentication.repository.ISessionFingerprintRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.repository.IUserInvoicePointSessionRepository;
import com.wallparisoft.authentication.repository.IUserRepository;
import com.wallparisoft.authentication.service.IRoleService;
import com.wallparisoft.authentication.service.ISessionFingerprintService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionFingerprintServiceImpl implements ISessionFingerprintService {

  private final ISessionFingerprintRepository sessionFingerprintRepository;
  private final IUserRepository userRepository;
  private final IRoleService roleService;
  private final IUserCompanyRepo userCompanyRepo;
  private final IUserInvoicePointSessionRepository userInvoicePointSessionRepository;
  private final IInvoiceIssuancePointClient invoiceIssuancePointClient;
  private final ICustomerClient customerClient;

  @Override
  @Transactional
  public UUID createSessionFingerprint(String userName, String ipAddress, String userAgent) {
    User user =
        userRepository
            .findByUserName(userName)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

    // Eliminar fingerprint anterior si existe
    sessionFingerprintRepository.deleteByUser_IdUser(user.getIdUser());

    // Crear nuevo fingerprint
    UUID sessionId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    SessionFingerprint fingerprint =
        SessionFingerprint.builder()
            .sessionId(sessionId)
            .user(user)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .browserBase(extractBrowserBase(userAgent))
            .createdAt(now)
            .lastActivityAt(now)
            .build();

    sessionFingerprintRepository.save(fingerprint);

    log.info(
        "🔐 Fingerprint de sesión creado para usuario: {}, IP: {}, Browser: {}",
        userName,
        ipAddress,
        fingerprint.getBrowserBase());

    return sessionId;
  }

  @Override
  public SessionValidationResponseDto validateSession(
      String userName, String currentIp, String currentUserAgent) {

    // 1. Buscar usuario
    Optional<User> userOpt = userRepository.findByUserName(userName);
    if (userOpt.isEmpty()) {
      return buildInvalidResponse("session_not_found");
    }

    User user = userOpt.get();

    // 2. Buscar fingerprint de sesión
    Optional<SessionFingerprint> fingerprintOpt =
        sessionFingerprintRepository.findByUser_IdUser(user.getIdUser());

    if (fingerprintOpt.isEmpty()) {
      return buildInvalidResponse("session_not_found");
    }

    SessionFingerprint fingerprint = fingerprintOpt.get();

    // 3. Validar IP
    if (!fingerprint.getIpAddress().equals(currentIp)) {
      log.warn(
          "⚠️ IP mismatch para usuario {}: guardada={}, actual={}",
          userName,
          fingerprint.getIpAddress(),
          currentIp);
      return buildInvalidResponse("ip_mismatch");
    }

    // 4. Validar navegador base (no versión exacta)
    String currentBrowser = extractBrowserBase(currentUserAgent);
    if (!fingerprint.getBrowserBase().equalsIgnoreCase(currentBrowser)) {
      log.warn(
          "⚠️ Browser mismatch para usuario {}: guardado={}, actual={}",
          userName,
          fingerprint.getBrowserBase(),
          currentBrowser);
      return buildInvalidResponse("browser_mismatch");
    }

    // 5. Verificar punto de emisión asignado
    // Usamos el método que devuelve lista para manejar casos de duplicados
    User userForSession =
        userRepository
            .findByUserName(userName)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

    List<UserInvoicePointSession> activeSessions =
        userInvoicePointSessionRepository.findByUser_IdUserAndActiveTrueOrderByStartTimeDesc(
            userForSession.getIdUser());

    InvoicePointInfoDto invoicePointInfo = null;
    EstablishmentInfoDto establishmentInfo = null;
    Long establishmentIdFromPoint = null;

    if (activeSessions.isEmpty()) {
      log.info("ℹ️ Usuario {} no tiene punto de emisión asignado", userName);
      // No es un error - el usuario puede no tener punto asignado aún
    } else {
      // Si hay múltiples sesiones activas (error de datos), tomamos la más reciente
      if (activeSessions.size() > 1) {
        log.warn(
            "⚠️ Se encontraron {} sesiones activas para usuario {}. Usando la más reciente.",
            activeSessions.size(),
            userName);
      }
      // Obtener información del punto de emisión desde ebill-customer
      try {
        InvoiceIssuancePointDto pointDto = invoiceIssuancePointClient.getCurrentSession(userName);
        if (pointDto != null) {
          invoicePointInfo =
              InvoicePointInfoDto.builder()
                  .idInvoicePoint(pointDto.getIdInvoiceIssuancePoint())
                  .code(pointDto.getCode())
                  .build();
          establishmentIdFromPoint = pointDto.getEstablishmentId();
        }
      } catch (Exception e) {
        log.warn(
            "⚠️ Error obteniendo punto de emisión para usuario {}: {}", userName, e.getMessage());
      }
    }

    // 6. Obtener información completa del establishment si tenemos el ID
    if (establishmentIdFromPoint != null) {
      try {
        EstablishmentDto estDto = customerClient.getEstablishmentById(establishmentIdFromPoint);
        if (estDto != null) {
          establishmentInfo =
              EstablishmentInfoDto.builder()
                  .idEstablishment(estDto.getIdEstablishment())
                  .name(estDto.getName())
                  .code(estDto.getCode())
                  .build();
        }
      } catch (Exception e) {
        log.warn(
            "⚠️ Error obteniendo datos de establishment {}: {}",
            establishmentIdFromPoint,
            e.getMessage());
        // Fallback: al menos devolver el ID
        establishmentInfo =
            EstablishmentInfoDto.builder().idEstablishment(establishmentIdFromPoint).build();
      }
    }

    // 7. Actualizar última actividad
    fingerprint.setLastActivityAt(LocalDateTime.now());
    sessionFingerprintRepository.save(fingerprint);

    // 8. Obtener información del usuario
    List<Role> roles = roleService.findByUserNameAndStatus(userName, true);
    List<String> roleNames =
        roles != null ? roles.stream().map(Role::getName).toList() : Collections.emptyList();

    UserInfoDto userInfo =
        UserInfoDto.builder()
            .userName(user.getUserName())
            .name(user.getName())
            .roles(roleNames)
            .build();

    // 9. Obtener información COMPLETA de company desde ebill-customer
    CompanyInfoDto companyInfo = null;

    List<UserCompany> userCompanies =
        userCompanyRepo.findByUser_IdUser(user.getIdUser()).orElse(Collections.emptyList());

    if (!userCompanies.isEmpty()) {
      UserCompany firstCompany = userCompanies.get(0);
      Long idCompany = firstCompany.getIdCompany();

      // Obtener información completa de la compañía desde ebill-customer
      try {
        CompanyDto companyDto = customerClient.getCompanyById(idCompany);
        if (companyDto != null) {
          companyInfo =
              CompanyInfoDto.builder()
                  .idCompany(companyDto.getIdCompany())
                  .name(companyDto.getName())
                  .identification(companyDto.getIdentification())
                  .build();
        }
      } catch (Exception e) {
        log.warn("⚠️ Error obteniendo datos de company {}: {}", idCompany, e.getMessage());
        // Fallback: al menos devolver el ID
        companyInfo = CompanyInfoDto.builder().idCompany(idCompany).build();
      }
    }

    log.info("✅ Sesión validada exitosamente para usuario: {}", userName);

    return SessionValidationResponseDto.builder()
        .valid(true)
        .user(userInfo)
        .company(companyInfo)
        .establishment(establishmentInfo)
        .invoicePoint(invoicePointInfo)
        .build();
  }

  @Override
  @Transactional
  public void deleteSessionFingerprint(String userName) {
    Optional<User> userOpt = userRepository.findByUserName(userName);
    if (userOpt.isPresent()) {
      sessionFingerprintRepository.deleteByUser_IdUser(userOpt.get().getIdUser());
      log.info("🗑️ Fingerprint de sesión eliminado para usuario: {}", userName);
    }
  }

  @Override
  public void updateLastActivity(UUID sessionId) {
    sessionFingerprintRepository
        .findBySessionId(sessionId)
        .ifPresent(
            fingerprint -> {
              fingerprint.setLastActivityAt(LocalDateTime.now());
              sessionFingerprintRepository.save(fingerprint);
            });
  }

  @Override
  public String extractBrowserBase(String userAgent) {
    if (userAgent == null || userAgent.isEmpty()) {
      return "Unknown";
    }

    // Orden importante: algunos navegadores incluyen nombres de otros en su UA
    // Edge incluye "Chrome" y "Safari", Chrome incluye "Safari"
    if (userAgent.contains("Edg")) {
      return "Edge";
    }
    if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
      return "Opera";
    }
    if (userAgent.contains("Chrome")) {
      return "Chrome";
    }
    if (userAgent.contains("Safari")) {
      return "Safari";
    }
    if (userAgent.contains("Firefox")) {
      return "Firefox";
    }
    if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
      return "Internet Explorer";
    }

    return "Unknown";
  }

  private SessionValidationResponseDto buildInvalidResponse(String reason) {
    return SessionValidationResponseDto.builder().valid(false).reason(reason).build();
  }
}
