package com.wallparisoft.ebillbilling.config;

import com.wallparisoft.ebillbilling.entity.Proforma;
import com.wallparisoft.ebillbilling.event.CronConfigUpdatedEvent;
import com.wallparisoft.ebillbilling.repository.IProformaRepo;
import com.wallparisoft.ebillbilling.service.ParamsService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Tarea programada que expira proformas vencidas diariamente.
 *
 * <p>Regla: - Proformas con validUntil &lt;= hoy - Sin idInvoice (no fueron convertidas a factura)
 * - En estado DRAFT o SENT se marcan como EXPIRED.
 *
 * <p>La expresión cron y el flag de habilitación se leen desde BD para permitir configurar y
 * reprogramar la tarea dinámicamente en tiempo de ejecución.
 */
@Component
@Slf4j
public class ProformaExpirationScheduler implements DisposableBean {

  private static final String PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM =
      "PROFORMA_EXPIRE_CRON_EXPRESSION";
  private static final String PROFORMA_EXPIRE_CRON_ENABLED_PARAM = "PROFORMA_EXPIRE_CRON_ENABLED";
  // 00:00:00 todos los días por defecto
  private static final String DEFAULT_CRON_EXPRESSION = "0 0 0 * * *";
  private static final String DEFAULT_CRON_ENABLED = "true";

  private final IProformaRepo proformaRepo;
  private final ThreadPoolTaskScheduler taskScheduler;
  private final ParamsService paramsService;

  private boolean cronTaskEnabled;
  private String cronExpression;
  private ScheduledFuture<?> scheduledFuture;

  public ProformaExpirationScheduler(
      IProformaRepo proformaRepo,
      ThreadPoolTaskScheduler taskScheduler,
      ParamsService paramsService) {
    this.proformaRepo = proformaRepo;
    this.taskScheduler = taskScheduler;
    this.paramsService = paramsService;
    log.info("Constructor de ProformaExpirationScheduler ejecutado.");
  }

  @PostConstruct
  public void init() {
    loadCronConfigFromDatabase();
    scheduleTask();
  }

  /** Carga la configuración del cron desde la base de datos. */
  private void loadCronConfigFromDatabase() {
    cronExpression =
        paramsService.getParamValueOrDefault(
            PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM, DEFAULT_CRON_EXPRESSION);

    String enabledValue =
        paramsService.getParamValueOrDefault(
            PROFORMA_EXPIRE_CRON_ENABLED_PARAM, DEFAULT_CRON_ENABLED);
    cronTaskEnabled = Boolean.parseBoolean(enabledValue);

    log.info(
        "Configuración del cron de expiración de proformas cargada desde BD: enabled={}, expression={}",
        cronTaskEnabled,
        cronExpression);
  }

  /** Reprograma la tarea recargando la configuración desde BD. */
  public void rescheduleTask() {
    loadCronConfigFromDatabase();
    log.info(
        "Reprogramando tarea de expiración de proformas: enable={}, expression={}",
        cronTaskEnabled,
        cronExpression);

    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }

    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron de expiración de proformas ha sido deshabilitada.");
    }
  }

  /** Reprograma la tarea con valores explícitos. */
  public void rescheduleTask(boolean enable, String expression) {
    this.cronTaskEnabled = enable;
    this.cronExpression = expression;
    log.info(
        "Reprogramando tarea de expiración de proformas: enable={}, expression={}",
        enable,
        expression);

    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      scheduledFuture = null;
    }

    if (cronTaskEnabled) {
      scheduleTask();
    } else {
      log.info("La tarea cron de expiración de proformas ha sido deshabilitada.");
    }
  }

  private void scheduleTask() {
    if (cronTaskEnabled) {
      try {
        scheduledFuture =
            taskScheduler.schedule(this::executeTask, new CronTrigger(cronExpression));
        log.info("Tarea de expiración de proformas programada con expresión: {}", cronExpression);
      } catch (Exception e) {
        log.error(
            "Error al programar la tarea cron de expiración de proformas con expresión '{}': {}",
            cronExpression,
            e.getMessage(),
            e);
      }
    } else {
      log.info("La tarea cron de expiración de proformas está deshabilitada y no se programará.");
    }
  }

  /** Lógica principal: expira proformas vencidas. */
  private void executeTask() {
    try {
      LocalDate today = LocalDate.now();

      int updated = proformaRepo.markExpiredProformas(today, Proforma.Status.EXPIRED);
      if (updated > 0) {
        log.info(
            "ProformaExpirationScheduler: {} proformas marcadas como EXPIRED para la fecha {}",
            updated,
            today);
      } else {
        log.debug(
            "ProformaExpirationScheduler: no hay proformas para expirar en la fecha {}", today);
      }
    } catch (Exception e) {
      log.error("Error al ejecutar tarea de expiración de proformas: {}", e.getMessage(), e);
    }
  }

  /**
   * Escucha el evento de actualización de configuración del cron y reprograma la tarea
   * automáticamente solo si el parámetro corresponde a esta tarea.
   */
  @EventListener
  @Async
  public void handleCronConfigUpdated(CronConfigUpdatedEvent event) {
    if (PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM.equals(event.getParamCode())
        || PROFORMA_EXPIRE_CRON_ENABLED_PARAM.equals(event.getParamCode())) {
      log.info(
          "Evento de actualización de configuración cron recibido para parámetro de proformas: {}",
          event.getParamCode());
      rescheduleTask();
    }
  }

  @Override
  public void destroy() throws Exception {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    log.info("ProformaExpirationScheduler destruido.");
  }
}
