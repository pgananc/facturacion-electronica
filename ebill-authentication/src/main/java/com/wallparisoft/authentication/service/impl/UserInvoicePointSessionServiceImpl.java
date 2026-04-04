package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.client.IInvoiceIssuancePointClient;
import com.wallparisoft.authentication.dto.InvoiceIssuancePointDto;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import com.wallparisoft.authentication.exception.ResourceInUseException;
import com.wallparisoft.authentication.repository.IUserInvoicePointSessionRepository;
import com.wallparisoft.authentication.repository.IUserRepository;
import com.wallparisoft.authentication.service.IUserInvoicePointSessionService;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class UserInvoicePointSessionServiceImpl implements IUserInvoicePointSessionService {

  final IUserInvoicePointSessionRepository userInvoicePointSessionRepository;
  final IUserRepository userRepository; // para obtener entidad User
  final IInvoiceIssuancePointClient invoiceIssuancePointClient;
  final UtilsRedis utilsRedis;

  @Override
  public void assignInvoiceIssuancePointToUser(String userName, Long idInvoiceIssuancePoint) {
    // Validar si el punto ya está siendo usado por otro usuario
    if (userInvoicePointSessionRepository.existsByIdInvoiceIssuancePointAndActiveTrue(
        idInvoiceIssuancePoint)) {
      throw new ResourceInUseException("Punto de emisión en uso por otro usuario.");
    }

    User user =
        userRepository
            .findByUserName(userName)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

    // Buscar si existe un registro inactivo previo con mismo user y punto
    Optional<UserInvoicePointSession> existingInactiveSession =
        userInvoicePointSessionRepository
            .findByUser_UserNameAndIdInvoiceIssuancePointAndActiveFalse(
                userName, idInvoiceIssuancePoint);

    UserInvoicePointSession sessionToSave;

    LocalDateTime now = LocalDateTime.now();

    if (existingInactiveSession.isPresent()) {
      // Reutilizamos el registro inactivo
      sessionToSave = existingInactiveSession.get();
      sessionToSave.setStartTime(now);
      sessionToSave.setEndTime(null);
      sessionToSave.setLastActivity(now);
      sessionToSave.setActive(true);
    } else {
      // Creamos nuevo si no existe
      sessionToSave =
          UserInvoicePointSession.builder()
              .user(user)
              .idInvoiceIssuancePoint(idInvoiceIssuancePoint)
              .startTime(now)
              .lastActivity(now)
              .active(true)
              .build();
    }

    userInvoicePointSessionRepository.save(sessionToSave);

    utilsRedis.registerPointSession(userName, idInvoiceIssuancePoint.toString());
  }

  @Override
  public Optional<UserInvoicePointSession> findActiveSessionByUser(Long idUser) {
    return userInvoicePointSessionRepository.findByUser_IdUserAndActiveTrue(idUser);
  }

  @Override
  public void releaseActiveSession(Long idUser) {
    userInvoicePointSessionRepository
        .findByUser_IdUserAndActiveTrue(idUser)
        .ifPresent(
            session -> {
              session.setActive(false);
              session.setEndTime(LocalDateTime.now());
              userInvoicePointSessionRepository.save(session);
            });
  }

  @Override
  public List<Long> getActiveInvoicePointIds() {
    return userInvoicePointSessionRepository.findByActiveTrue().stream()
        .map(UserInvoicePointSession::getIdInvoiceIssuancePoint)
        .toList();
  }

  @Override
  public List<InvoiceIssuancePointDto> findAvailablePointsByEstablishments(
      List<Long> idEstablishments) {
    // 1. Obtener IDs de puntos de emisión ocupados (sesiones activas)
    List<Long> inUse =
        userInvoicePointSessionRepository.findByActiveTrue().stream()
            .map(UserInvoicePointSession::getIdInvoiceIssuancePoint)
            .toList();

    // 2. Consultar puntos activos desde ebill-customer
    List<InvoiceIssuancePointDto> allActivePoints =
        invoiceIssuancePointClient.findAvailablePoints(idEstablishments);

    // 3. Filtrar para obtener solo los disponibles
    return allActivePoints.stream()
        .filter(p -> !inUse.contains(p.getIdInvoiceIssuancePoint()))
        .toList();
  }

  @Override
  public InvoiceIssuancePointDto getCurrentSession(String userName) {
    return invoiceIssuancePointClient.getCurrentSession(userName);
  }

  @Override
  public boolean updateSessionHeartbeat(String userName) {
    Optional<UserInvoicePointSession> activeSession =
        userInvoicePointSessionRepository.findByUser_UserNameAndActiveTrue(userName);

    if (activeSession.isPresent()) {
      UserInvoicePointSession session = activeSession.get();
      LocalDateTime now = LocalDateTime.now();
      session.setLastActivity(now);
      userInvoicePointSessionRepository.save(session);

      // 🔧 FIX: Actualizar TTL de la sesión en Redis cuando se recibe heartbeat
      // Esto previene que la sesión expire en Redis antes del timeout configurado
      try {
        String invoicePointId = session.getIdInvoiceIssuancePoint().toString();
        utilsRedis.registerPointSession(userName, invoicePointId);
      } catch (Exception e) {
        // Log del error pero no fallar el heartbeat si Redis falla
        // La sesión en BD ya fue actualizada, así que el heartbeat es exitoso
        log.warn(
            "⚠️  Error al actualizar TTL de sesión en Redis para usuario {}: {}",
            userName,
            e.getMessage());
      }

      return true;
    }

    return false;
  }
}
