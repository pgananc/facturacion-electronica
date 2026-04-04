package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserInvoicePointSessionRepository
    extends JpaRepository<UserInvoicePointSession, Long> {

  // Verifica si un punto ya está en uso
  boolean existsByIdInvoiceIssuancePointAndActiveTrue(Long idInvoiceIssuancePoint);

  // Obtiene la sesión activa de un usuario (si existe)
  Optional<UserInvoicePointSession> findByUser_IdUserAndActiveTrue(Long idUser);

  // Obtiene la sesión activa de un usuario por username
  Optional<UserInvoicePointSession> findByUser_UserNameAndActiveTrue(String userName);

  // Obtiene todas las sesiones activas de un usuario (puede haber múltiples en caso de error)
  List<UserInvoicePointSession> findByUser_IdUserAndActiveTrueOrderByStartTimeDesc(Long idUser);

  // Devuelve todos los puntos actualmente en uso
  List<UserInvoicePointSession> findByActiveTrue();

  Optional<UserInvoicePointSession> findByUser_UserNameAndIdInvoiceIssuancePointAndActiveFalse(
      String userName, Long idInvoiceIssuancePoint);

  // Encuentra sesiones inactivas (sin actividad reciente).
  // Incluye lastActivity IS NULL para liberar puntos tras suspensión o datos legacy.
  @Query(
      "SELECT s FROM UserInvoicePointSession s "
          + "WHERE s.active = true "
          + "AND (s.lastActivity IS NULL OR s.lastActivity < :threshold)")
  List<UserInvoicePointSession> findInactiveSessions(@Param("threshold") LocalDateTime threshold);

  // Libera sesiones inactivas en batch (incluye lastActivity NULL)
  @Modifying
  @Query(
      "UPDATE UserInvoicePointSession s "
          + "SET s.active = false, "
          + "s.endTime = :now "
          + "WHERE s.active = true "
          + "AND (s.lastActivity IS NULL OR s.lastActivity < :threshold)")
  int deactivateInactiveSessions(
      @Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);
}
