package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.repository.ISessionFingerprintRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de limpieza automática de fingerprints de sesión inactivos.
 *
 * <p>Elimina sesiones que no han tenido actividad en el tiempo configurado para prevenir
 * acumulación de registros obsoletos y mejorar seguridad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionFingerprintCleanupService {

  private final ISessionFingerprintRepository sessionFingerprintRepository;

  /**
   * Tiempo máximo de inactividad permitido en horas antes de eliminar la sesión. Default: 24 horas
   */
  @Value("${session.fingerprint.max-inactive-hours:24}")
  private int maxInactiveHours;

  /**
   * Job programado para limpiar sesiones de fingerprint inactivas.
   *
   * <p>Se ejecuta cada hora por defecto. Las sesiones sin actividad por más de maxInactiveHours
   * serán eliminadas.
   *
   * <p>Cron: 0 0 * * * * = cada hora, al minuto 0
   */
  @Scheduled(cron = "${session.fingerprint.cleanup.cron:0 0 * * * *}")
  @Transactional
  public void cleanupInactiveSessions() {
    LocalDateTime cutoffTime = LocalDateTime.now().minusHours(maxInactiveHours);

    log.info(
        "🧹 Iniciando limpieza de fingerprints de sesión inactivos (sin actividad desde {})",
        cutoffTime);

    int deleted = sessionFingerprintRepository.deleteByLastActivityAtBefore(cutoffTime);

    if (deleted > 0) {
      log.info("✅ Se eliminaron {} fingerprints de sesión inactivos", deleted);
    } else {
      log.debug("ℹ️ No hay fingerprints de sesión inactivos para eliminar");
    }
  }

  /**
   * Método manual para forzar limpieza (útil para administración).
   *
   * @param hours Número de horas de inactividad
   * @return Cantidad de registros eliminados
   */
  @Transactional
  public int forceCleanup(int hours) {
    LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
    int deleted = sessionFingerprintRepository.deleteByLastActivityAtBefore(cutoffTime);
    log.info(
        "🧹 Limpieza forzada: {} fingerprints eliminados (inactivos por {} horas)", deleted, hours);
    return deleted;
  }
}
