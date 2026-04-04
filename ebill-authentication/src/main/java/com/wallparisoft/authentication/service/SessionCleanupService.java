package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import com.wallparisoft.authentication.repository.IUserInvoicePointSessionRepository;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de limpiar sesiones inactivas de puntos de emisión.
 *
 * <p>Este servicio se ejecuta periódicamente para liberar puntos de emisión que han quedado
 * bloqueados por sesiones que no han enviado heartbeat recientemente.
 *
 * <p>Estrategia: - Cada X minutos (configurable), busca sesiones activas sin actividad reciente
 * (o con last_activity NULL, p. ej. tras suspensión de la máquina) - Las marca como inactivas y
 * libera el punto de emisión - Limpia también el registro en Redis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionCleanupService {

  private final IUserInvoicePointSessionRepository sessionRepository;
  private final UtilsRedis utilsRedis;

  // Timeout de inactividad en segundos (configurable desde properties)
  @Value("${session.inactivity.timeout.seconds:600}")
  private int inactivityTimeoutSeconds;

  /**
   * Tarea programada que se ejecuta periódicamente para limpiar sesiones inactivas.
   *
   * <p>Se ejecuta cada 3 minutos por defecto (configurable).
   *
   * <p>Configuración: Cron expression configurable via properties. Por defecto cada 3 minutos.
   * Formato cron: segundos minutos horas días meses días-semana
   */
  @Scheduled(cron = "${session.cleanup.cron:0 */3 * * * *}")
  @Transactional
  public void cleanupInactiveSessions() {
    log.info("🧹 Iniciando limpieza de sesiones inactivas...");

    try {
      LocalDateTime threshold = LocalDateTime.now().minusSeconds(inactivityTimeoutSeconds);
      LocalDateTime now = LocalDateTime.now();

      // Encontrar sesiones inactivas
      List<UserInvoicePointSession> inactiveSessions =
          sessionRepository.findInactiveSessions(threshold);

      if (inactiveSessions.isEmpty()) {
        log.info("✅ No hay sesiones inactivas para limpiar");
        return;
      }

      log.info(
          "⚠️  Encontradas {} sesiones inactivas (última actividad anterior a {})",
          inactiveSessions.size(),
          threshold);

      // Procesar cada sesión inactiva
      int cleaned = 0;
      for (UserInvoicePointSession session : inactiveSessions) {
        try {
          String userName = session.getUser() != null ? session.getUser().getUserName() : "N/A";
          Long invoicePointId = session.getIdInvoiceIssuancePoint();
          LocalDateTime lastActivity = session.getLastActivity();

          log.info(
              "🔍 Procesando sesión inactiva - Usuario: {} - Punto: {} - Última actividad: {} (hace {} segundos)",
              userName,
              invoicePointId,
              lastActivity,
              java.time.Duration.between(lastActivity, now).getSeconds());

          // Desactivar sesión
          session.setActive(false);
          session.setEndTime(now);
          sessionRepository.save(session);

          // Limpiar Redis si existe
          if (session.getUser() != null && session.getUser().getUserName() != null) {
            try {
              utilsRedis.removePointSession(userName);
              log.debug(
                  "🗑️  Redis limpiado para usuario: {} - Punto: {}", userName, invoicePointId);
            } catch (Exception redisEx) {
              log.warn(
                  "⚠️  Error al limpiar Redis para usuario {}: {}", userName, redisEx.getMessage());
            }
          }

          cleaned++;
          log.info(
              "✅ Sesión liberada exitosamente - Usuario: {} - Punto: {} - Última actividad: {}",
              userName,
              invoicePointId,
              lastActivity);

        } catch (Exception e) {
          log.error(
              "❌ Error al liberar sesión ID {}: {}", session.getIdUserInvoicePointSession(), e);
        }
      }

      log.info(
          "✅ Limpieza completada: {} de {} sesiones liberadas", cleaned, inactiveSessions.size());

    } catch (Exception e) {
      log.error("❌ Error en proceso de limpieza de sesiones: {}", e.getMessage(), e);
    }
  }

  /**
   * Método manual para forzar limpieza (útil para testing o admin).
   *
   * @return Número de sesiones liberadas
   */
  public int forceCleanup() {
    log.warn("⚠️  Limpieza forzada de sesiones iniciada manualmente");
    cleanupInactiveSessions();

    LocalDateTime threshold = LocalDateTime.now().minusSeconds(inactivityTimeoutSeconds);
    return sessionRepository.findInactiveSessions(threshold).size();
  }
}
