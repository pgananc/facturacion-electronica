package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.SessionFingerprint;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ISessionFingerprintRepository extends JpaRepository<SessionFingerprint, Long> {

  Optional<SessionFingerprint> findByUser_IdUser(Long userId);

  Optional<SessionFingerprint> findBySessionId(UUID sessionId);

  void deleteByUser_IdUser(Long userId);

  boolean existsByUser_IdUser(Long userId);

  /**
   * Elimina sesiones cuya última actividad sea anterior a la fecha dada.
   *
   * @param cutoffTime Fecha límite de inactividad
   * @return Número de registros eliminados
   */
  @Modifying
  @Query("DELETE FROM SessionFingerprint s WHERE s.lastActivityAt < :cutoffTime")
  int deleteByLastActivityAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
